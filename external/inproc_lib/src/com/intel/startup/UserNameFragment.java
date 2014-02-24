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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.intel.inproclib.R;
import com.intel.inproclib.utility.MaxLengthTextWatcher;
import com.intel.startup.NewUnbox.UnboxFragmentEnum;
import com.intel.stc.utility.StcConstants;


public class UserNameFragment extends NewUnboxFragment implements TextWatcher, OnClickListener
{
	/********************** Constants ********************************************/
	public final static String 	tag 						= "Unboxing";
	public final static String	className					= "InProc - " + UserNameFragment.class.getSimpleName();
	
	/********************** Local Variables ********************************************/
	EditText	mNameET;
	Button		mContinue;
	
	/*********************************************************************/
	/**** Android Activity Life Cycle ************************************/
	/*********************************************************************/
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.v("Unboxing", "UserNameFragment: OnCreateView - Begin.");
		
		View vUserNameFragmentView = inflater.inflate(R.layout.frag_unboxing_name, null);

		mContinue = (Button) vUserNameFragmentView.findViewById(R.id.enter_name_page_continue_button);
		mContinue.setOnClickListener(this);
		
		mNameET = (EditText) vUserNameFragmentView.findViewById(R.id.screen_name_text);
		
		final String mNameString = newActivity.mUserName;
		if (mNameString != null)
		{
			mNameET.setText("");			
			mNameET.append(mNameString);
			if(!mNameString.isEmpty())
			{
				mContinue.setEnabled(true);
			}
		}

		mNameET.addTextChangedListener(new MaxLengthTextWatcher(StcConstants.MAX_USER_NAME_JAVA_STRING_LENGTH, mNameET));
		mNameET.addTextChangedListener(this);
		
		Log.v("Unboxing", "UserNameFragment: OnCreateView - End.");
		
		return vUserNameFragmentView;
	}
	
	/*********************************************************************/
	/**** Android Callbacks **********************************************/
	/*********************************************************************/	
	@Override
	public void onClick(View v)
	{
		hideKeyboard();
		
		if (newActivity != null)
		{
			Log.v("Unboxing", "UserNameFragment: onClick - User clicked Continue. Move to Avatar page.");
			UnboxFragmentEnum.avatar.addOrReplaceFragment(newActivity);
		}
	}
	
	@Override
	public void onBackPressed()
	{
		/* Old code
		if (newActivity != null)
			newActivity.setFailFinish();
		*/
		
		// New unboxing changes - show initial 'Start Up' page.
		if(newActivity != null)
		{
			if(!newActivity.mFinishedCloudReg)
			{
				Log.v("Unboxing", "UserNameFragment: onBackPressed - User clicked back button. Move to 'StartUp' page.");
				UnboxFragmentEnum.startup.addOrReplaceFragment(newActivity);
			}
		}
		
	}

	/*********************************************************************/
	/**** Public Helpers *************************************************/
	/*********************************************************************/	
	public void updateUserName(final String newName)
	{
		if (newName != null && newName.length() > 0)
		{
			if (mNameET != null)
				mNameET.setText(newName);
		}
	}
		
	public void hideKeyboard()
	{
		if (newActivity != null && mNameET != null)
		{
			InputMethodManager inputManager = (InputMethodManager) newActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputManager.hideSoftInputFromWindow(mNameET.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

	public void showKeyboard()
	{
		if (newActivity != null && mNameET != null)
		{
			InputMethodManager inputManager = (InputMethodManager) newActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
		}
	}
	
	/*********************************************************************/
	/**** Android TextWatcher ********************************************/
	/*********************************************************************/	
	@Override
	public void afterTextChanged(Editable s)
	{
		if (newActivity != null)
			newActivity.setUserName(mNameET.getText().toString().trim());	
		mContinue.setEnabled(true);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{		
	}
}
