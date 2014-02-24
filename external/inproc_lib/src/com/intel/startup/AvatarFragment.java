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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.intel.inproclib.R;
import com.intel.startup.NewUnbox.UnboxFragmentEnum;

public class AvatarFragment extends NewUnboxFragment implements OnClickListener
{
	/********************** Constants ********************************************/
	public final static String 		tag 						= "Unboxing";
	public final static String		className					= "InProc - " + AvatarFragment.class.getSimpleName();
	
	/********************** Local Variables ********************************************/
	Handler							mhandler;
	ImageView						mAvatarImageView;
	Button							mContinue;

	/*********************************************************************/
	/**** Android Activity Life Cycle ************************************/
	/*********************************************************************/	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.v("Unboxing", "AvatarFragment: OnCreateView - Begin.");
		
		View vAvatarView = inflater.inflate(R.layout.frag_unboxing_avatar, null);

		mContinue = (Button) vAvatarView.findViewById(R.id.unboxing_avatar_continue_button);
		mContinue.setOnClickListener(this);

		mAvatarImageView = (ImageView) vAvatarView.findViewById(R.id.unboxing_avatar);
		mAvatarImageView.setOnClickListener(this);

		if (newActivity.mAvatarBytes != null)
		{
			updateAvatarWork();
			mContinue.setEnabled(true);
		}
		else
		{
			mContinue.setEnabled(false);
		}

		Log.v("Unboxing", "AvatarFragment: OnCreateView - End.");
		
		return vAvatarView;
	}

	/*********************************************************************/
	/**** Interface of abstract base class *******************************/
	/*********************************************************************/	
	@Override
	public void onBackPressed()
	{
		if (newActivity != null)
		{
			Log.v("Unboxing", "AvatarFragment: onBackPressed - User clicked back button. Move to 'Screen Name' page.");
			UnboxFragmentEnum.username.addOrReplaceFragment(newActivity);
		}
	}

	/*********************************************************************/
	/**** Android Callbacks **********************************************/
	/*********************************************************************/
	
	@Override
	public void onClick(View v)
	{		
		if (newActivity != null)
		{
			if (v.getId() == mAvatarImageView.getId())
			{
				Log.v("Unboxing", "AvatarFragment: onClick - User clicked Avatar image. Showing avatar picker page.");
				UnboxFragmentEnum.avatarpicker.addOrReplaceFragment(newActivity);
			}
			else if (v.getId() == mContinue.getId())
			{
				Log.v("Unboxing", "AvatarFragment: onClick - User clicked 'Continue' button. Move to device name page.");
				
				// Goto 'Enter Device Name' page
				UnboxFragmentEnum.devicename.addOrReplaceFragment(newActivity);
			}				
		}
	}
	
	/*********************************************************************/
	/**** Private Helpers ************************************************/
	/*********************************************************************/	
	private void updateAvatarWork()
	{
		AsyncTask<Void,Void,Bitmap> setImageTask = new AsyncTask<Void,Void,Bitmap>() {
			@Override
			protected Bitmap doInBackground(Void... params)
			{
				if (newActivity != null && mAvatarImageView != null)
				{
					return BitmapFactory.decodeByteArray(newActivity.mAvatarBytes , 0, newActivity.mAvatarBytes.length);
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Bitmap result)
			{
				if (newActivity != null && mAvatarImageView != null && result != null)
				{
					mAvatarImageView.setImageBitmap(result);
				}
				super.onPostExecute(result);
			}
		};
		setImageTask.execute();
	}

	

}
