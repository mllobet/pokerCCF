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

package com.intel.startup;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.intel.inproclib.R;
import com.intel.inproclib.utility.InProc_ImageManager_Assets;
import com.intel.startup.NewUnbox.UnboxFragmentEnum;

public class AvatarPickerFragment extends NewUnboxFragment
{
	/********************** Constants  ***************************************/
	private final String 		tag 						= "Unboxing";
	private final String		className					= "InProc - " + AvatarPickerFragment.class.getSimpleName();
	
	/********************** Local Variables ********************************************/
	public boolean				ignoreBack					= false;
	public String[]				defaultAvatarFiles			= null;
	public GridView				grid;

	/*********************************************************************/
	/**** Android Activity Life Cycle ************************************/
	/*********************************************************************/
	
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		ignoreBack = false;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.v("Unboxing", "AvatarPickerFragment: onCreateView - Begin.");
		
		View v = inflater.inflate(R.layout.frag_unboxing_avatar_select, null);

		grid = (GridView) v.findViewById(R.id.unboxing_avatar_grid);
		
		AsyncTask<Void, Void, Void> loadAvatars = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params)
			{
				AssetManager mngr = newActivity.getAssets();
				try
				{
					defaultAvatarFiles = mngr.list(NewUnbox.AVATAR_FOLDER);
				}
				catch (IOException e)
				{
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result)
			{
				grid.setAdapter(new AvatarAdapter());
				super.onPostExecute(result);
			}
		};
		
		loadAvatars.execute();

		Log.v("Unboxing", "AvatarPickerFragment: onCreateView - End.");
		return v;
	}

	/*********************************************************************/
	/**** Interface of abstract base class *******************************/
	/*********************************************************************/
	@Override
	public void onBackPressed()
	{
		if (!ignoreBack)
		{
			Log.v("Unboxing", "AvatarPickerFragment: onBackPressed - User clicked back button. Move to avatar page.");
			UnboxFragmentEnum.avatar.addOrReplaceFragment(newActivity);
		}
	}

	/*********************************************************************/
	/**** Private Helpers ************************************************/
	/*********************************************************************/
	private class AvatarAdapter extends BaseAdapter
	{
		InProc_ImageManager_Assets	assetManager	= new InProc_ImageManager_Assets(getActivity());

		@Override
		public int getCount()
		{
			return defaultAvatarFiles.length;
		}

		@Override
		public String getItem(int position)
		{
			return defaultAvatarFiles[position];
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			if (convertView == null)
			{
				convertView = new ImageView(getActivity());
				convertView.setLayoutParams(new android.widget.AbsListView.LayoutParams(90, 90));
			}
			else
			{
				assetManager.resetImageView((ImageView) convertView);
			}

			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v)
				{
					if (newActivity != null)
					{
						ignoreBack = true;
						newActivity.showSpinner();
						newActivity.avatarSelected(NewUnbox.AVATAR_FOLDER + File.separator
								+ defaultAvatarFiles[position]);
					}
					else
						onBackPressed();
				}
			});

			assetManager.requestImage(NewUnbox.AVATAR_FOLDER + File.separator + defaultAvatarFiles[position],
					(ImageView) convertView);
			return convertView;
		}
	}

}
