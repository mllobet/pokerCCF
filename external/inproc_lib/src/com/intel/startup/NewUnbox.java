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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;

import com.intel.inproclib.R;
import com.intel.inproclib.utility.InProcConstants;
import com.intel.mw.PlatformHelper;
import com.intel.stc.events.InviteRequestEvent;
import com.intel.stc.events.InviteResponseEvent;
import com.intel.stc.events.StcException;
import com.intel.stc.interfaces.StcConnectionListener;
import com.intel.stc.utility.StcConstants;
import com.intel.stc.utility.StcSession;
import com.intel.stc.utility.d;
import com.intel.stclibcc.StcLibCC;
import com.intel.ux.ImageUtilities;

public class NewUnbox extends FragmentActivity implements StcConnectionListener
{	
	/********************** Save State ********************************************/
	public final static String	USER_NAME_BUNDLE			= "__username";
	public final static String	CUSTOM_AVATAR_PATH_BUNDLE	= "__custom_path";
	public final static String	TEMP_URI_BUNDLE				= "__uri_path";
	public final static String	AVATAR_PATH_BUNDLE			= "__path";
	
	/********************** Preferences ********************************************/
	final String				PAUSE_PREFS				= "__PAUSE_PREFS";
	final String				PREFS_USERNAME			= "__PREFS_USERNAME";
	final String				PREFS_REGAVATAR			= "__PREFS_REGAVATAR";
	final String				PREFS_CUSTOMAVATAR		= "__PREFS_CUSTOMAVATAR";
	final String				PREFS_DEVICENAME		= "__PREFS_DEVICENAME";
	final String				PREFS_CURFRAG			= "__PREFS_CURFRAG";
	
	/********************** Constants ********************************************/
	public final static String 	tag 						= "Unboxing";
	public final static String	className					= "InProc - " + NewUnbox.class.getSimpleName();
	public final static int		STARTED_FOR_CAMERA			= 0x0202;
	public final static int		STARTED_FOR_GALLERY			= 0x0203;
	public final static int		STARTED_CLOUD				= 0x0204;
	public final static int		BITMAP_HEIGHT				= 48;
	public final static int		BITMAP_WIDTH				= 48;
	public final static String	CUR_PAGE					= "__curPage";
	
	
	
	/******************************************************************************/	
	public enum UnboxFragmentEnum
	{

	 	// TODO - Add cloud registration page
		startup(), username(), avatar(), avatarpicker(), devicename();
	 	
	 	//, registration();

		/* Instance */

		public final NewUnboxFragment	fFrag;
		public final String				fFragId;

		private UnboxFragmentEnum() {
			switch (ordinal())
			{
			case 0:
				fFrag = new StartupFragment();
				fFragId = "StartupFragment";
				break;
				
			case 1:
				fFrag = new UserNameFragment();
				fFragId = "UserNameFragment";
				break;
				
			case 2:
				fFrag = new AvatarFragment();
				fFragId = "AvatarFragment";
				break;
				
			case 3:
				fFrag = new AvatarPickerFragment();
				fFragId = "AvatarPickerFragment";
				break;
				
			case 4:				
				fFrag = new DeviceNameFragment();
				fFragId = "DeviceNameFragment";
				break;
				
			default:
				fFrag = null;
				fFragId = "";
				break;
			}
		}

		public void addOrReplaceFragment(NewUnbox parent)
		{
			if (parent == null)
				return;

			FragmentManager man = parent.getSupportFragmentManager();
			View v = parent.findViewById(R.id.fragment_holder);

			if (man != null && v != null)
			{
				Fragment loadedFragment = man.findFragmentById(R.id.fragment_holder);
				if (loadedFragment != null)
				{
					if(loadedFragment.getClass().equals(fFrag.getClass()))
					{
						man.beginTransaction().remove(loadedFragment).commit();
						man.executePendingTransactions();
					}
					man.beginTransaction().replace(R.id.fragment_holder, fFrag, fFragId).commit();
				}
				else
					man.beginTransaction().add(R.id.fragment_holder, fFrag, fFragId).commit();

				parent.curFrag = this;
				man.executePendingTransactions();
			}
		}

		public static UnboxFragmentEnum getFragmentFromId(final int ordinal)
		{
			switch (ordinal)
			{
			case 0:
				return startup;
			case 1:
				return username;
			case 2:
				return avatar;
			case 3:
				return avatarpicker;
			case 4:
				return devicename;
			default:
				return startup;
			}
		}

	}
	
	/********************** Local Variables ********************************************/
	AlertDialog					alertDialog					= null;
	boolean						mIsRegisteredWithCloud		= false;
	boolean						mIsUnboxed					= false;
	boolean						mFinished					= false;
	boolean						mFinishedCloudReg			= false;
	boolean						mSkippedCloudReg			= false;
	boolean						mScreenNamePresent			= false;
	boolean						mAvatarPresent				= false;
	boolean						mResumeAfterAvatarSetup 	= false;
	byte[]						mAvatarBytes;
	final Handler				mHandler					= new Handler();
	public final static String	AVATAR_FOLDER				= "avatars";
	public final static String	GENERIC_AVATAR				= AVATAR_FOLDER + File.separator + "generic_128x128_002.png";
	public String				CUSTOM_AVATAR_PATH;	
	StcLibCC					mLib;
	StcSession					mLocalSession;
	String						mUserName					= "";
	String						mSystemUserName				= "";
	String						mDefaultAvatarsPath			= GENERIC_AVATAR;
	String						mAvatarPath;	
	String						mTempFileName;	
	String						mDeviceName					= "";
	UnboxFragmentEnum			curFrag						= UnboxFragmentEnum.startup;
	View						galleryView					= null;
	public final static String	CREATE_ACCOUNT_BUTTON_CLICKED= "CREATEACCOUNTBTNCLICKED";
	public boolean 				registerButtonClicked 		= false;
	
	/*********************************************************************/
	/**** Android Activity Life Cycle ************************************/
	/*********************************************************************/
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == STARTED_FOR_CAMERA || requestCode == STARTED_FOR_GALLERY)
		{
			if (resultCode != Activity.RESULT_CANCELED)
			{
				showSpinner();
				
				// added following check because when user selects picture from 
				// Picasa, the original code would fail and UI will keep showing spinner
				// until user ends the application. We do not retrieve picture from Picasa
				// as we have to copy the picture first on the local storage which will fill the
				// storage faster.
				boolean goodSelectedImage = checkSelectedImage(data, requestCode);
				if(!goodSelectedImage)
				{
					showErrorMessage();
				}
				else
				{
					startTask(new AvatarTask(data, requestCode));
				}
			}
		}
		else if (requestCode == STARTED_CLOUD)
		{
			if (resultCode == StcConstants.STC_RESULT_OK)
			{
				mFinishedCloudReg = true;
			}
			else if(resultCode == StcConstants.STC_RESULT_SKIP_SIGN_IN)
			{
				mFinishedCloudReg = false;
				mSkippedCloudReg = true;				
			}
			else
			{
				mFinishedCloudReg = false;
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{		
		d.enter(InProcConstants.INPROC_TAG, className, "onCreate");
		super.onCreate(savedInstanceState);

		CUSTOM_AVATAR_PATH = getFilesDir().getAbsolutePath() + File.separator + InProcConstants.CUSTOM_AVATAR_PREFIX
				+ "0.png";

		showSpinner();
		if(getIntent().hasExtra("FromRegisterButton"))
		{
			registerButtonClicked = getIntent().getExtras().getBoolean("FromRegisterButton");
		}
		if(!mIsUnboxed)		
		{			
			d.print(tag, className, "onCreate", "unboxing is not finished. Proceed with unboxing.");
			mSystemUserName = getSystemUserName();
			startTask(new LoadPeferencesTask());
		}
				
		d.exit(InProcConstants.INPROC_TAG, className, "onCreate");
	}

	@Override
	protected void onResume()
	{
		d.enter(InProcConstants.INPROC_TAG, className, "onResume");
		super.onResume();
		startTask(new OnResumeTask());
		
		d.exit(InProcConstants.INPROC_TAG, className, "onResume");
	}

	@Override
	protected void onPause()
	{
		d.enter(InProcConstants.INPROC_TAG, className, "onPause");
		
		super.onPause();
		
		if (mLib != null)
		{
			try
			{
				mLib.disconnectFromPlatform();
			}
			catch (StcException e2)
			{
			}
			mLib = null;
		}

		synchronized (mTaskLock)
		{
			mPendingTaskList.clear();
			if (mCurTask != null)
				mCurTask.cancel(true);
		}
		FragmentManager man = getSupportFragmentManager();
		View v = findViewById(R.id.fragment_holder);

		if (man != null && v != null)
		{
			Fragment loadedFragment = man.findFragmentById(R.id.fragment_holder);
			if (loadedFragment != null)
			{				
					man.beginTransaction().remove(loadedFragment).commit();
					man.executePendingTransactions();
			}
		}
		
		startTask(new SavePreferencesTask());
		
		d.exit(InProcConstants.INPROC_TAG, className, "onPause");
	}
	
	/*********************************************************************/
	/**** Android Callbacks **********************************************/
	/*********************************************************************/
	@Override
	public void onBackPressed()
	{
		d.print(tag, className, "onBackPressed - ", "Begin. Execute current fragment: " + curFrag.toString() +" onBackPressed method.");
		curFrag.fFrag.onBackPressed();		
	}

	/*********************************************************************/
	/**** Public Helpers *************************************************/
	/*********************************************************************/	
	public void showSpinner()
	{	
		setContentView(R.layout.loading_spinner);
		findViewById(R.id.outer_layout).setVisibility(View.VISIBLE);
	}
			
	public void setFailFinish()
	{		
		d.print(tag, className, "setFailFinish", "User did not complete unboxing. Exit the application.");
		mFinished = false;
		setResult(StcConstants.STC_RESULT_CANCELED);
		finish();
	}

	public void setSuccessFinish()
	{
		d.print(tag, className, "setSuccessFinish", "User completed unboxing. Start FinishUnboxTask.");
		mFinished = true;
		if (!mIsUnboxed)
		{
			setResult(StcConstants.STC_RESULT_OK);
			startTask(new FinishUnboxTask(mUserName, mAvatarBytes, mDeviceName));
		}
	}
	
	public StcSession getLocalUSerSession()
	{
		try
		{			
			d.print(tag, className, "getLocalUSerSession", "Get local user's screen name from stack.");
			mLocalSession = mLib.queryLocalSession();
		}
		catch (StcException exception)
		{
			d.error(tag, className, "getLocalUSerSession", exception.getMessage());
		}
		
		return mLocalSession;
	}
	
	// Retrieve screen name and avatar from the server. This is used to determine if the device is first or later.
	// if screen name is present, set at the stack level. 
	public boolean IsLocalUserScreenNamePresent()
	{
		boolean retval = false;
		
		mLocalSession = getLocalUSerSession();
		if(mLocalSession != null)
		{
			mUserName = mLocalSession.getUserName();
			
			d.print(tag, className, "IsLocalUserScreenNamePresent", "Stack returned screen name as: "+ mUserName);
			if(mUserName != null && !mUserName.isEmpty())
			{
				//TODO:Swati - set the screen namemUserName for second device. Do we need to set here again?
				//setUserName(mUserName);
				retval = true;
			}
		}
		
		return retval;
	}
	
	// Retrieve screen name and avatar from the server. This is used to determine if the device is first or later.
	// if avatar is present, set at the stack level. 
	public boolean IsLocalUserAvatarPresent()
	{
		boolean retval = false;
		byte[] localUserAvatar = null;				
		
		mLocalSession = getLocalUSerSession();
		if(mLocalSession != null)
		{
			localUserAvatar = mLocalSession.getAvatarBytes();			
			if(localUserAvatar != null)
			{		
				d.print(tag, className, "IsLocalUserAvatarPresent", "Local user has avatar setup.");
				mAvatarBytes = localUserAvatar;
				retval = true;
			}
		}
		
		return retval;
	}
		
	public String getLocalUserStatusMessage()
	{
		String localUserStatusMessage = null;
		
		Log.v("Unboxing", "UserNameFragment: IsScreenNamePresent - Get local user's screen name from stack.");
		
		mLocalSession = getLocalUSerSession();
		if(mLocalSession != null)
		{
			localUserStatusMessage = mLocalSession.getStatus();			
			d.print(tag, className, "IsLocalUserAvatarPresent", "Stack returned status message as: "+ localUserStatusMessage);
		}
		
		return localUserStatusMessage;
	}
	
	/*********************************************************************/
	/**** Public Helpers - Set UserName **********************************/
	/*********************************************************************/
	
	@SuppressLint({ "InlinedApi", "NewApi" })
	public String getSystemUserName()
	{	
		Cursor cursor = this.getContentResolver().query(
				ContactsContract.Profile.CONTENT_URI, null, null, null, null);

		int count = cursor.getCount();

		String[] columnNames = cursor.getColumnNames();	
		
		cursor.moveToFirst();
		int position = cursor.getPosition();
		if (count == 1 && position == 0) 
		{
			for (int i = 0; i < count; i++) 
			{
				for (int j = 0; j < columnNames.length; j++) 
				{
					String columnName = columnNames[j];
					if(columnName.equals("display_name"))
					{						
						mSystemUserName = cursor.getString(cursor.getColumnIndex(columnName));	
						d.print(tag, className, "getSystemUserName", "returns - " + mSystemUserName);
					}					
				}
				cursor.moveToNext();
			}
		}
		cursor.close();
		
		return mSystemUserName;
	}
	
	public void setUserName(final String userName)
	{
		if (userName != null)
		{			
			d.print(tag, className, "setUserName", userName);
			mUserName = userName;
		}
		mSkippedCloudReg = false; // This makes OnResumeTask logic work for Custom Avatar Selection.
	}
	
	/*public void setScreenName()
	{
		startTask(new LoadPeferencesTask());
	}*/
	
	/*********************************************************************/
	/**** Public Helpers - Set Avatar ************************************/
	/*********************************************************************/
	
	public void startCamera(View v)
	{
		getSupportFragmentManager().beginTransaction().remove(curFrag.fFrag).commit();

		Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		String fileName = "StcProfilePic_" + System.currentTimeMillis();

		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, fileName);
		Uri tempUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

		// create parameters for Intent with filename
		camera.putExtra(MediaStore.EXTRA_OUTPUT, tempUri);

		mTempFileName = uriToString(tempUri);

		try
		{
			d.print(tag, className, "startCamera", "Starting activity result for camera.");
			startActivityForResult(camera, STARTED_FOR_CAMERA);
		}
		catch (Exception exception)
		{
			d.error(tag, className, "startCamera", exception.getMessage());
		}

		// Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// File tempFile = new File(CUSTOM_AVATAR_PATH);
		// if (tempFile.exists())
		// tempFile.delete();
		//
		// File.cre
		//
		// try
		// {
		// tempFile.createNewFile();
		// }
		// catch (IOException e)
		// {
		// e.printStackTrace();
		// }
		//
		// cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
		// Uri.fromFile(tempFile));
		// startActivityForResult(cameraIntent, STARTED_FOR_CAMERA);
	}
	
	public void startGallery(View v)
	{
		getSupportFragmentManager().beginTransaction().remove(curFrag.fFrag).commit();

		final Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType("image/*");
		galleryView = v;
		
		d.print(tag, className, "startCamera", "Starting activity result for gallery.");
		startActivityForResult(intent, STARTED_FOR_GALLERY);
	}
	
	private String uriToString(Uri uri)
	{
		String[] proj = { MediaStore.Images.Media.DATA };

		Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
		cursor.moveToFirst();

		int columnIndex = cursor.getColumnIndex(proj[0]);
		String filePath = cursor.getString(columnIndex);
		cursor.close();

		return filePath;
	}

	private boolean checkSelectedImage(Intent data, int requestcode)
	{
		boolean retval = true;
		
		if (requestcode == STARTED_FOR_GALLERY && data != null)
		{
			Uri uri = data.getData();
			if (uri != null)
			{
				if (uri.toString().startsWith("content://com.google.android.gallery3d"))
				{
					d.print(tag, className, "checkSelectedImage", "Picture from Picasa album is selected.");
					retval = false;
				}
			}
		}
		
		return retval;
	}
		
	private void showErrorMessage()
	{
		alertDialog = new AlertDialog.Builder(this).create();		
		
		Resources res = getResources();
		
		Bitmap original = BitmapFactory.decodeResource(res, R.drawable.message_warning_icon);
		Bitmap b = Bitmap.createScaledBitmap(original, BITMAP_WIDTH, BITMAP_HEIGHT, false);
		Drawable d = new BitmapDrawable(res, b);
		
		alertDialog.setIcon(d);
		alertDialog.setTitle(R.string.choose_avatar_error_gallery_not_stored_picture_title);
		alertDialog.setMessage(getString(R.string.choose_avatar_error_gallery_not_stored_picture));
		alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
				getString(R.string.ok_button_text),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						dialog.cancel();
						alertDialog = null;
						
						// Go back to avatar picker page
						setContentView(R.layout.unboxing_main);
						UnboxFragmentEnum.avatar.addOrReplaceFragment(NewUnbox.this);
					}
				});
		
		alertDialog.show();
	}
	
	public void setAvatarPath(final String avatarPath)
	{
		if (avatarPath != null)
		{			
			d.print(tag, className, "setAvatarPath", avatarPath);
			mAvatarPath = avatarPath;
		}
		mResumeAfterAvatarSetup = false;
	}

	// Runs after onactivity result.
	public class AvatarTask extends AsyncTask<Void, Void, Void>
	{

		final Intent	mData;
		final int		mStartedFor;

		byte[]			mATAvatarBytes;
		String			mATAvatarPath;

		public AvatarTask(final Intent data, final int startedFor) {
			mData = data;
			mStartedFor = startedFor;
		}

		@Override
		protected Void doInBackground(Void... params)
		{

			// Get the path for the picture or the image from the gallery.
			File imageFile = null;

			if (mStartedFor == STARTED_FOR_CAMERA && mTempFileName != null)
			{
				imageFile = new File(mTempFileName);
			}
			else
			{
				if (mData != null)
				{
					Uri uri = mData.getData();
					if (uri != null)
					{	
						String temp = uriToString(uri);
						if (temp != null)
						{
							imageFile = new File(temp);
						}
					}
				}

			}

			try
			{
				if (imageFile == null || !imageFile.exists())
					return null;

				// Create a temp file to resize.
				File resizeFile = new File(CUSTOM_AVATAR_PATH);
				if (resizeFile.exists())
					resizeFile.delete();
				FileOutputStream resizeOut = new FileOutputStream(resizeFile);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();

				Bitmap b = ImageUtilities.getThumbnail(NewUnbox.this, imageFile.getAbsolutePath());
				if (b != null)
					b.compress(CompressFormat.PNG, 100, bos);
				else
				{
					resizeFile.delete();
					imageFile.delete();
					resizeOut.close();
					return null;
				}

				mATAvatarBytes = bos.toByteArray();
				
				if(mATAvatarBytes != null)
					resizeOut.write(mATAvatarBytes);
				
				resizeOut.close();
				bos.close();

				// Rename the file.
				resizeFile.renameTo(imageFile);

				mATAvatarPath = CUSTOM_AVATAR_PATH;
			}
			catch (IOException e)
			{
				imageFile.delete();
				return null;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			if (isCancelled())
				return;

			// If we failed, just return.
			if (mATAvatarBytes == null || mATAvatarPath == null)
				return;

			mAvatarPath = mATAvatarPath;
			mDefaultAvatarsPath = null;

			mResumeAfterAvatarSetup = true;
			setContentView(R.layout.unboxing_main);
			curFrag = UnboxFragmentEnum.avatar;

			super.onPostExecute(result);
			finishTask();
		}
	}

	// Called from AvatarPickerFragment after the custom avatar is selected.	
	public void avatarSelected(String path)
	{
		startTask(new AvatarSelectTask(path));
	}
	
	public class AvatarSelectTask extends AsyncTask<Void, Void, Void>
	{
		final String	path;
		byte[]			temp;

		public AvatarSelectTask(final String path) {
			this.path = path;
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			try
			{
				AssetFileDescriptor afd = getAssets().openFd(path);
				if (afd != null && afd.getLength() > 0)
				{
					InputStream is = afd.createInputStream();
					temp = new byte[(int) afd.getLength()];
					is.read(temp);
				}
			}
			catch (IOException e)
			{
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			if (isCancelled())
				return;

			if (temp != null && path != null)
			{
				mDefaultAvatarsPath = path;
				mAvatarBytes = temp;
			}

			setContentView(R.layout.unboxing_main);
			UnboxFragmentEnum.avatar.addOrReplaceFragment(NewUnbox.this);

			super.onPostExecute(result);
			finishTask();
		}
	}
	
	/*********************************************************************/
	/**** Public Helpers - Set Device Name *******************************/
	/*********************************************************************/	
	public void setDeviceName(final String deviceName)
	{
		if(deviceName != null)
		{
			d.print(tag, className, "setDeviceName", deviceName);
			mDeviceName = deviceName;
		}	
	}
	
	/*********************************************************************/
	/**** Async Tasks ****************************************************/
	/*********************************************************************/	

	final Object							mTaskLock			= new Object();
	final List<AsyncTask<Void, Void, Void>>	mPendingTaskList	= new ArrayList<AsyncTask<Void, Void, Void>>();
	AsyncTask<Void, Void, Void>				mCurTask;

	public void startTask(final AsyncTask<Void, Void, Void> newTask)
	{
		// Are we on the main thread?
		if (Looper.myLooper() == Looper.getMainLooper())
		{
			synchronized (mTaskLock)
			{
				if (mCurTask == null)
					mCurTask = newTask.execute();
				else
					mPendingTaskList.add(newTask);
			}
		}
		else
		{
			mHandler.post(new Runnable() {

				@Override
				public void run()
				{
					synchronized (mTaskLock)
					{
						if (mCurTask == null)
							mCurTask = newTask.execute();
						else
							mPendingTaskList.add(newTask);
					}
				}
			});
		}

	}

	public void finishTask()
	{
		synchronized (mTaskLock)
		{
			if (mPendingTaskList.size() > 0)
				mCurTask = mPendingTaskList.remove(0).execute();
			else
				mCurTask = null;
		}
	}

	public class LoadPeferencesTask extends AsyncTask<Void, Void, Void>
	{
		boolean error = false;
		
		@Override
		protected Void doInBackground(Void... params)
		{
			if (isCancelled())
				return null;

			loadPrefs();

			return null;
		}

		@SuppressLint({ "InlinedApi", "NewApi" })
		private void loadPrefs()
		{
			final SharedPreferences sp = NewUnbox.this.getSharedPreferences(PAUSE_PREFS, Activity.MODE_PRIVATE);
					
			// Initialize username, avatar and devicename.						
			mUserName = sp.getString(PREFS_USERNAME, mSystemUserName);
						
			if(mAvatarPath == null)
			{
				mDefaultAvatarsPath = sp.getString(PREFS_REGAVATAR, null);			
				mAvatarPath = sp.getString(PREFS_CUSTOMAVATAR, null);
				if (mDefaultAvatarsPath == null && mAvatarPath == null)
					mDefaultAvatarsPath = GENERIC_AVATAR;
			}
			
			mDeviceName = Build.MODEL.trim();			
		}

		@Override
		protected void onPostExecute(Void result)
		{
			if (isCancelled())
				return;
						
			/* TODO: Swati - this may not be the right place for this code.		
			if(mFinishedCloudReg && !mFinished)
			{
				// Cloud registration is complete. Goto Enter Name page								
				Log.v("Unboxing", "NewUnbox: OnResumeTask: onPostExecute - Start setting name avatar and device name.");				
				
				// TODO: Add the logic for second device
				
				//Screen name is not set so go to enter name page				
				curFrag = UnboxFragmentEnum.username;
				
			}			
			else if(!mFinishedCloudReg && !mFinished)
			{
				// both cloud registration and unboxing are not complete. Show startup page.
				curFrag = UnboxFragmentEnum.startup;
			}*/

			finishTask();
			super.onPostExecute(result);
		}

	}

	public class OnResumeTask extends AsyncTask<Void, Void, Void>
	{
		boolean	error	= false;

		@Override
		protected Void doInBackground(Void... params)
		{
			tryToLoadBitmap();
			createLib();
			return null;
		}

		private void createLib()
		{
			try
			{
				if (mLib != null)
				{
					try
					{
						StcSession localSession = mLib.queryLocalSession();
						if (localSession == null)
							throw new StcException(0);
					}
					catch (Exception e)
					{
						try
						{
							mLib.disconnectFromPlatform();
						}
						catch (StcException e2)
						{
						}
						mLib = null;
					}
				}

				if (mLib == null)
				{
					mLib = new StcLibCC(PlatformHelper.GetPath(), NewUnbox.this);
				}
			}
			catch (StcException e)
			{
				error = true;
			}

			if (!error)
			{
				try
				{
					mIsUnboxed = mLib.isUnboxed();
					if (mIsUnboxed)
					{
						StcSession session = mLib.queryLocalSession();
						if (session == null)
							throw new StcException(0);

						mIsRegisteredWithCloud = session.isRegisteredWithCloud();						
					}
										
					//mScreenNamePresent = IsLocalUserScreenNamePresent();
					//mAvatarPresent = IsLocalUserAvatarPresent();
				}
				catch (StcException e)
				{
					error = true;
				}
			}
		}

		private void tryToLoadBitmap()
		{
			// Load Avatar out of assets
			if (mDefaultAvatarsPath != null)
			{
				InputStream avatarIS = null;

				try
				{
					AssetFileDescriptor afd = getAssets().openFd(mDefaultAvatarsPath);
					InputStream is = afd.createInputStream();
					mAvatarBytes = new byte[(int) afd.getLength()];
					is.read(mAvatarBytes);
				}
				catch (IOException e)
				{
					mAvatarBytes = null;
				}
				catch (OutOfMemoryError oome)
				{
					mAvatarBytes = null;
				}
				finally
				{
					if (avatarIS != null)
					{
						try
						{
							avatarIS.close();
						}
						catch (IOException e)
						{
						}
					}
				}
			}
			// Load Custom avatar
			if (mAvatarPath != null)
			{
				File f = new File(mAvatarPath);
				InputStream customIS = null;
				if (f.exists())
				{
					try
					{
						customIS = new FileInputStream(f);
						mAvatarBytes = new byte[(int) f.length()];
						customIS.read(mAvatarBytes);
					}
					catch (IOException e)
					{
						mAvatarBytes = null;
						d.error(InProcConstants.INPROC_TAG, "NewUnbox", "could not read image file", e);
					}
					catch (OutOfMemoryError oome)
					{
						mAvatarBytes = null;
					}
					finally
					{
						if (customIS != null)
						{
							try
							{
								customIS.close();
							}
							catch (IOException e)
							{
							}
						}
					}
				}
			}
		}

		@Override
		protected void onPostExecute(Void result)
		{
			if (isCancelled())
				return;

			if (error)
				setFailFinish();
			
			// New 3.0 Unboxing Code	
			if(mFinishedCloudReg && mFinished)
			{
				showSpinner();
				setSuccessFinish();
			}
			else 
			{
				if(mFinishedCloudReg && !mFinished)
				{
					// Cloud registration is complete. Goto Enter Name page								
					Log.v("Unboxing", "NewUnbox: OnResumeTask: onPostExecute - Start setting name avatar and device name.");				
					
					// Logic for second device. Currently, we get default username 'Android' from stack for localsession.
					// That needs to be fixed because that causes problem at UI level.
					/*mScreenNamePresent = IsLocalUserScreenNamePresent();
					if(mScreenNamePresent)
					{
						// This is the second device registering with Cloud using the same credentials.
						if(mAvatarPresent = IsLocalUserAvatarPresent())
						{
							// This is the second device registering with Cloud using the same credentials and avatar is already set
							// Set the device name
							curFrag = UnboxFragmentEnum.devicename;
						}
						else
						{
							// For some reason avatar is not set at the cloud. This should not happen.
							curFrag = UnboxFragmentEnum.avatar;
						}
					} Logic for second device ends here*/
					
					if(mResumeAfterAvatarSetup == false)
					{
						//Screen name is not set so go to enter name page				
						curFrag = UnboxFragmentEnum.username;
					}
				}	
				else if(!mFinishedCloudReg && !mFinished)
				{
					if(mSkippedCloudReg == true)
					{
						// We started with cloud registration first. 
						// However, due to error we decided to skip cloud registration.
						// Hence no need to show the startup page again.
						curFrag = UnboxFragmentEnum.username;
					}
					else if(mResumeAfterAvatarSetup == true)
					{
						// If activity is not resumed due to avatar setup with gallery or camera, 
						// then only launch the startup page else we should continue with curfrag.
						// both cloud registration and unboxing are not complete. Show startup page.
						curFrag = UnboxFragmentEnum.avatar;
					}
					else
					{
						curFrag = UnboxFragmentEnum.startup;
					}
					
				}
				
				setContentView(R.layout.unboxing_main);
				curFrag.addOrReplaceFragment(NewUnbox.this);
			}	
			
			super.onPostExecute(result);
			finishTask();
		}
	}
	
	public class FinishUnboxTask extends AsyncTask<Void, Void, Void>
	{
		final byte[]	mFAvatarBytes;
		final String	mFUserName;
		final String	mFSessionName;


		public FinishUnboxTask(final String userName, final byte[] avatar, final String sessionName) {
			mFAvatarBytes = avatar;
			mFUserName = userName;
			mFSessionName = sessionName;
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			if (mFAvatarBytes == null || mFUserName == null || mFSessionName == null)
			{
				cancel(false);
				setFailFinish();
				return null;
			}

			//ByteArrayOutputStream out = new ByteArrayOutputStream();

			if (mLib != null)
			{
				try
				{
					Log.v("Unboxing", "NewUnbox: FinishUnboxTask - User completed unboxing. Inform screen name, avatar and device name to stack.");
					
					mLib.setUserName(mFUserName);
					mLib.setSessionName(mFSessionName);
					mLib.setAvatar(mFAvatarBytes);
					
					SharedPreferences sp = getSharedPreferences(StcConstants.AVATAR_PREFERENCES, Activity.MODE_PRIVATE);
					if (sp != null)
					{
						SharedPreferences.Editor speditor = sp.edit();
						if (mDefaultAvatarsPath != null)
						{
							if (mAvatarPath != null)
							{
								File fileToDelete = new File(CUSTOM_AVATAR_PATH);
								fileToDelete.delete();
							}
							speditor.putString(StcConstants.AVATAR_PREF_ID, mDefaultAvatarsPath);
						}
						else
						{
							speditor.putString(StcConstants.AVATAR_PREF_ID, mAvatarPath);
						}
						speditor.commit();
					}
				}
				catch (StcException e)
				{
					Log.v("Unboxing", "NewUnbox: FinishUnboxTask - Exception occurred while saving screen name, avatar and device name at stack level.");
					Log.v("Unboxing", "NewUnbox: FinishUnboxTask - Cancelling the unboxing.");
					
					cancel(false);
					setFailFinish();
					return null;
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result)
		{
			if (isCancelled())
				return;

			finish();
			super.onPostExecute(result);
			finishTask();
		}
	}

	public class SavePreferencesTask extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params)
		{
			final SharedPreferences sp = NewUnbox.this.getSharedPreferences(PAUSE_PREFS, Activity.MODE_PRIVATE);
			final Editor e = sp.edit();

			// Remove Preferences, we've completed unbox
			if (mFinished && sp != null && e != null)
			{
				e.remove(PREFS_USERNAME);
				e.remove(PREFS_REGAVATAR);
				e.remove(PREFS_CUSTOMAVATAR);
				e.remove(PREFS_DEVICENAME);
				e.remove(PREFS_CURFRAG);
				e.commit();
			}
			// Set Preferences, we haven't completed unbox
			else if (!mFinished && sp != null && e != null)
			{
				e.putString(PREFS_USERNAME, mUserName);
				e.putString(PREFS_REGAVATAR, mDefaultAvatarsPath);
				e.putString(PREFS_CUSTOMAVATAR, mAvatarPath);
				e.putString(PREFS_DEVICENAME, mDeviceName);
				e.putInt(PREFS_CURFRAG, curFrag.ordinal());
				e.commit();
			}

			synchronized (mTaskLock)
			{
				mCurTask = null;
				mPendingTaskList.clear();
			}

			return null;
		}
	}

	
	/*********************************************************************/
	/**** Empty Callbacks ************************************************/
	/*********************************************************************/	
	
	@Override
	public void connectionRequest(InviteRequestEvent ire)
	{
		// TODO Auto-generated method stub

		//String str = getResources().getString(R.string.online_image_detection);
	/*	
		if (requestcode == STARTED_FOR_GALLERY && data != null)
		{
			Uri uri = data.getData();
			if (uri != null)
			{
				if (uri.toString().startsWith("content://com.google.android.gallery3d") ||
					uri.toString().startsWith("content://com.android.gallery3d.provider"))
				{		
					//content://com.android.gallery3d.provider
					retval = false;
	*/
	}

	@Override
	public void connectionCompleted(InviteResponseEvent ire)
	{
		// TODO Auto-generated method stub

	}
}
