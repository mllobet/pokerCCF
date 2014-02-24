/*
Copyright (c) 2011-2013, Intel Corporation

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.

    * Neither the name of Intel Corporation nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.intel.inproclib.user_details;

import java.io.InputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.intel.inproclib.R;
import com.intel.inproclib.utility.ImageViewNoLayoutRefresh;
import com.intel.inproclib.utility.InProcConstants;
import com.intel.inproclib.utility.InProc_ListViewImageManager;
import com.intel.inproclib.utility.InProc_ListViewImageManager_FileSystem;
import com.intel.mw.PlatformHelper;
import com.intel.startup.CloudAuthorizationActivity;
import com.intel.stc.events.InviteRequestEvent;
import com.intel.stc.events.InviteResponseEvent;
import com.intel.stc.events.StcException;
import com.intel.stc.interfaces.StcConnectionListener;
import com.intel.stc.utility.StcConstants;
import com.intel.stc.utility.StcSession;
import com.intel.stc.utility.d;
import com.intel.stclibcc.StcLibCC;

public class UpdateUserDetailsActivity extends Activity implements StcConnectionListener, OnFocusChangeListener, OnItemClickListener, OnClickListener
{
	private static final String tag = UpdateUserDetailsActivity.class.getSimpleName();

	public final int			START_CLOUD		= 129031;
	public static final int		CLOUD_COMPLETED	= 1091902;

	private boolean				isPaused		= false;
	private StcLibCC			mLib;
	private StcSession				mLocalSession;

	private String[]			defaultAvatarFiles;

	/* Views */

	TextView					uUserName, uUserStatus;
	ImageView					uUserAvatar;
	Button						uRegisterButton;
	Gallery						uAvatarPicker;

	InProc_ListViewImageManager	mImageManager;

	/* Activity Stuff */

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.loading_spinner);
		findViewById(R.id.outer_layout).setVisibility(View.VISIBLE);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		isPaused = false;

		mImageManager = new InProc_ListViewImageManager_FileSystem(this);

		CreateLib libTask = new CreateLib();
		libTask.execute();
	}

	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		tryUserUpdate();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		tryUserUpdate();

		isPaused = true;

		if (mImageManager != null)
		{
			mImageManager.destroyManager();
			mImageManager = null;
		}

		if (mLib != null)
		{
			try
			{
				mLib.disconnectFromPlatform();
			}
			catch (Exception e)
			{
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == START_CLOUD)
		{
			if (resultCode == StcConstants.STC_RESULT_OK)
				setResult(CLOUD_COMPLETED);
		}
	}

	/* Interfaces */

	@Override
	public void onFocusChange(View v, boolean hasFocus)
	{
		if (!hasFocus)
		{
			if (uUserName != null && v.getId() == uUserName.getId())
				updateUserName(uUserName.getText().toString().trim());
			else if (uUserStatus != null && v.getId() == uUserStatus.getId())
				updateUserStatus(uUserStatus.getText().toString().trim());
		}
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id)
	{
		final AsyncTask<Void, Void, Void> onItemClickAsyncTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params)
			{
				byte[] avatarBytes = null;
				String path = "avatars/" + defaultAvatarFiles[position];
				try
				{
					AssetFileDescriptor afd = UpdateUserDetailsActivity.this.getAssets().openFd(path);
					InputStream is = afd.createInputStream();
					avatarBytes = new byte[(int) afd.getLength()];
					is.read(avatarBytes);
					// If we get an exception trying to read or
					// anything prior we shouldn't set the
					// avatar anyways.
					mLib.setAvatar(avatarBytes);
				}
				catch (Exception e)
				{
					d.error(InProcConstants.INPROC_TAG, tag, "onItemClick", e);
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void result)
			{
				try
				{
					mLocalSession = mLib.queryLocalSession();
				}
				catch (StcException e)
				{
				}

				Bitmap b = mLocalSession.getAvatar();
				uUserAvatar.setImageBitmap(b);
			}
		};
		onItemClickAsyncTask.execute();
	}

	/* Update Details */

	AlertDialog	ad	= null;

	private synchronized void tryUserUpdate()
	{
		onFocusChange(uUserName, false);
		onFocusChange(uUserStatus, false);
	}

	private synchronized void updateUserName(final String newName)
	{
		if (newName.length() == 0 && ad == null)
		{
			ad = new AlertDialog.Builder(this).create();
			ad.setTitle(R.string.invalid_username);
			ad.setMessage(getString(R.string.invalid_username_text));
			ad.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.ok), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.cancel();
					ad = null;
				}
			});

			if (mLocalSession != null && uUserName != null)
			{
				uUserName.setText(mLocalSession.getUserName());
				ad.show();
			}

			return;
		}
		try
		{
			if (mLocalSession.getUserName().compareTo(newName) != 0)
			{
				// TODO - neither of these should be done on the UI thread.
				mLib.setUserName(newName);
				mLocalSession = mLib.queryLocalSession();
			}
		}
		catch (StcException e)
		{
			d.error(InProcConstants.INPROC_TAG, tag, "updateUserName", e);
		}
	}

	private synchronized void updateUserStatus(final String newStatus)
	{
		try
		{
			if (mLocalSession.getStatus().compareTo(newStatus) != 0)
			{
				// TODO - neither of these should be done on the UI thread.
				mLib.setStatusText(newStatus);
				mLocalSession = mLib.queryLocalSession();
			}
		}
		catch (StcException e)
		{
			d.error(InProcConstants.INPROC_TAG, tag, "updateUserStatus", e);
		}
	}

	/* Inner Classes */

	private class GalleryImageAdapter extends BaseAdapter
	{
		private Context				mContext;
		private String				filePrefix	= "";
		private ArrayList<String>	files;

		public GalleryImageAdapter(Context c, String filePrefix, String[] files) {
			mContext = c;
			this.filePrefix = filePrefix;
			this.files = new ArrayList<String>();
			for (String s : files)
			{
				this.files.add(s);
			}
		}

		public GalleryImageAdapter(Context c, String filePrefix, ArrayList<String> files) {
			mContext = c;
			this.filePrefix = filePrefix;
			this.files = files;
		}

		public void updateFiles(ArrayList<String> files)
		{
			this.files = files;
			notifyDataSetChanged();
		}

		public int getCount()
		{
			return files.size();
		}

		public Object getItem(int position)
		{
			if (files != null && position < files.size() && position >= 0)
				return files.get(position);
			else
				return null;
		}

		public long getItemId(int position)
		{
			return position;
		}

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ImageViewNoLayoutRefresh imageView;

			// if it's not recycled, initialize some attributes
			if (convertView == null)
			{
				LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = (LinearLayout) inflater.inflate(R.layout.inproc_profile_pic, null);
			}
			imageView = (ImageViewNoLayoutRefresh) convertView.findViewById(R.id.profile_picture);
			if (mImageManager != null)
			{
				mImageManager.resetImageView(imageView);

				mImageManager.requestImage(filePrefix + files.get(position), imageView);
			}
			else
			{
				imageView.setImageResource(R.drawable.generic_avatar);
			}

			return convertView;
		}
	}

	private class CreateLib extends AsyncTask<Void, Void, Void>
	{

		private StcLibCC	tempLib;
		private StcSession		localSession;

		GalleryImageAdapter	adapter;

		@Override
		protected Void doInBackground(Void... params)
		{
			try
			{
				tempLib = new StcLibCC(PlatformHelper.GetPath(), UpdateUserDetailsActivity.this);
				localSession = tempLib.queryLocalSession();

				defaultAvatarFiles = UpdateUserDetailsActivity.this.getAssets().list("avatars");
				adapter = new GalleryImageAdapter(UpdateUserDetailsActivity.this, "avatars/", defaultAvatarFiles);
			}
			catch (Exception e)
			{
				tempLib = null;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			final UpdateUserDetailsActivity ui = UpdateUserDetailsActivity.this;
			// We aren't paused yet.
			if (!isPaused)
			{
				// Failure
				if (tempLib == null)
				{
					Toast.makeText(ui, "FAIL", Toast.LENGTH_LONG).show();

					ui.finish();
				}
				// Success!
				else
				{
					mLib = tempLib;
					mLocalSession = localSession;

					UpdateUserDetailsActivity.this.setContentView(R.layout.inproc_user_details_view);

					uUserName = (TextView) ui.findViewById(R.id.p_userName);
					uUserStatus = (TextView) ui.findViewById(R.id.p_userStatus);
					uUserAvatar = (ImageView) ui.findViewById(R.id.p_userAvatar);
					uAvatarPicker = (Gallery) ui.findViewById(R.id.p_avatar_gallery);
					uRegisterButton = (Button) ui.findViewById(R.id.p_registerButton);

					uUserName.setText(mLocalSession.getUserName());
					uUserName.setOnFocusChangeListener(ui);
					uUserStatus.setText(mLocalSession.getStatus());
					uUserStatus.setOnFocusChangeListener(ui);
					uUserAvatar.setImageBitmap(mLocalSession.getAvatar());
					uAvatarPicker.setAdapter(adapter);
					uAvatarPicker.setOnItemClickListener(ui);
					if (mLocalSession.isRegisteredWithCloud())
						uRegisterButton.setVisibility(View.GONE);
					else
						uRegisterButton.setOnClickListener(UpdateUserDetailsActivity.this);
				}
			}
		}
	}

	@Override
	public void onClick(View v)
	{
		// Register Button was clicked.
		try
		{
			startActivityForResult(new Intent(getApplicationContext(), CloudAuthorizationActivity.class), START_CLOUD);
		}
		catch (Exception e)
		{
		}
	}

	/* Ignore All */

	@Override
	public void connectionRequest(InviteRequestEvent ire)
	{
	}

	@Override
	public void connectionCompleted(InviteResponseEvent ire)
	{
	}

}
