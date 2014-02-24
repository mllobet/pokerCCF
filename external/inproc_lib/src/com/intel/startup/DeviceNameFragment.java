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
import com.intel.inproclib.utility.InProcConstants;
import com.intel.inproclib.utility.MaxLengthTextWatcher;
import com.intel.startup.NewUnbox.UnboxFragmentEnum;
import com.intel.stc.utility.StcConstants;
import com.intel.stc.utility.d;

public class DeviceNameFragment extends NewUnboxFragment implements TextWatcher, OnClickListener
{
	/********************** Constants ********************************************/
	public final static String 	tag 						= "Unboxing";
	public final static String	className					= "InProc - " + DeviceNameFragment.class.getSimpleName();
	
	/********************** Local Variables ********************************************/
	Button mDoneButton;
	EditText mDeviceNameEditText;
	
	/*********************************************************************/
	/**** Android Activity Life Cycle ************************************/
	/*********************************************************************/
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.v("Unboxing", "DeviceNameFragment: OnCreateView - Begin.");
		
		View vDeViceNameView = inflater.inflate(R.layout.frag_unboxing_device, null);
		
		mDoneButton = (Button)vDeViceNameView.findViewById(R.id.done_button);
		mDoneButton.setOnClickListener(this);	
		
		mDeviceNameEditText = (EditText)vDeViceNameView.findViewById(R.id.device_name_text);		
		
		final String deviceName = newActivity.mDeviceName;
		if(deviceName != null)
		{
			mDeviceNameEditText.setText("");
			mDeviceNameEditText.append(deviceName);	
			if(!deviceName.isEmpty())
			{
				mDoneButton.setEnabled(true);
			}
		}
		
		mDeviceNameEditText.addTextChangedListener(new MaxLengthTextWatcher(StcConstants.MAX_USER_NAME_JAVA_STRING_LENGTH, mDeviceNameEditText));
		mDeviceNameEditText.addTextChangedListener(this);
		
		

		Log.v("Unboxing", "DeviceNameFragment: OnCreateView - End.");
		
		return vDeViceNameView;
	}
		
	/*********************************************************************/
	/**** Android Callbacks **********************************************/
	/*********************************************************************/	
	@Override
	public void onClick(View deviceNameView)
	{		
		// Unboxing is finished. Go to 'People' page.
		hideKeyboard();
		
		final int buttonId = deviceNameView.getId();
		
		if(newActivity != null)
		{
			if(buttonId == R.id.done_button)
			{
				d.print(InProcConstants.INPROC_TAG, className, "onClick", "User clicked done button. Complete unboxing");
				newActivity.setSuccessFinish();		
			}
		}
	}
	
	@Override
	public void onBackPressed()
	{
		if (newActivity != null)
		{			
			UnboxFragmentEnum.avatar.addOrReplaceFragment(newActivity);
		}
	}
		
	/*********************************************************************/
	/**** Android TextWatcher ********************************************/
	/*********************************************************************/
	@Override
	public void afterTextChanged(Editable s)
	{
		if (newActivity != null)
			newActivity.setDeviceName(mDeviceNameEditText.getText().toString().trim());
		
		mDoneButton.setEnabled(true);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
	}
		
	/*********************************************************************/
	/**** Public Helpers *************************************************/
	/*********************************************************************/	
	public void hideKeyboard()
	{
		if (newActivity != null && mDeviceNameEditText != null)
		{
			InputMethodManager in = (InputMethodManager) newActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			in.hideSoftInputFromWindow(mDeviceNameEditText.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}	
}
