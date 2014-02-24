package com.intel.startup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.intel.inproclib.R;
import com.intel.startup.NewUnbox.UnboxFragmentEnum;
import com.intel.stc.lib.StcLib;
import com.intel.stc.utility.StcConstants;
import com.intel.stc.utility.d;
import com.intel.stclibcc.StcLibCC;

public class StartupFragment extends NewUnboxFragment implements OnClickListener
{
	/********************** Constants ********************************************/
	public final static String 	tag 						= "Unboxing";
	public final static String	className					= "InProc - " + StartupFragment.class.getSimpleName();
	
	/********************** Local Variables ********************************************/
	Button mCreateAccountButton;
	Button mSignInButton;
	Button mSkipSignInButton;
	
	private String			clientID						= "UW1zLA6yCWaqErsllAAK1kC1yJQQWldZ";
	private String			redirectURL						= "http://www.intel.com/robots.txt";
	
	boolean createAccountBtnClicked = false;
	
	/*********************************************************************/
	/**** Android Activity Life Cycle ************************************/
	/*********************************************************************/
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.v("Unboxing", "StartupFragment: OnCreateView - Begin");
		
		View vStartupFragmentView = inflater.inflate(R.layout.frag_unboxing_startup, null);
		
		mCreateAccountButton = (Button)vStartupFragmentView.findViewById(R.id.create_account_button);
		mCreateAccountButton.setOnClickListener(this);
		
		mSignInButton = (Button)vStartupFragmentView.findViewById(R.id.sign_in_button);
		mSignInButton.setOnClickListener(this);	
		
		mSkipSignInButton = (Button) vStartupFragmentView.findViewById(R.id.skip_sign_in_button);
		
		if(newActivity.registerButtonClicked == false)
		{
			// User clicked 'Sign in' button on the first startup page and not 'Register' button in Profile.
			mSkipSignInButton.setOnClickListener(this);
		}
		else
		{
			// User clicked 'Register' button in Profile. Do not show 'Skip Sign In' button.
			mSkipSignInButton.setVisibility(View.GONE);
		}
		
		Log.v("Unboxing", "StartupFragment: OnCreateView - End");
		
		return vStartupFragmentView;
	}
	
	/*********************************************************************/
	/**** Android Callbacks **********************************************/
	/*********************************************************************/	
	@Override
	public void onClick(View startupView)
	{		
		final int buttonId = startupView.getId();
		
		if(buttonId == R.id.create_account_button)
		{			
			d.print(tag, className, "onClick", "User clicked 'Create Account' button.");
			
			File file = new File(Environment.getExternalStorageDirectory(), "/Intel/cloudsettings.xml");
			if(file.exists())
			{
				try
				{			
					XmlPullParserFactory xppfact = XmlPullParserFactory.newInstance();
					xppfact.setNamespaceAware(true);
					XmlPullParser xpp = xppfact.newPullParser();
					
					FileInputStream fis = new FileInputStream(file);
					xpp.setInput(new InputStreamReader(fis));
					int eventType;
					while ((eventType = xpp.next()) != XmlPullParser.END_DOCUMENT)
					{
						if (eventType == XmlPullParser.START_TAG)
						{
							String name = xpp.getName();
							if(name.compareTo("ClientId") == 0)
							{
								clientID = xpp.nextText();
							}
							if(name.compareTo("RedirectURI") == 0)
							{
								redirectURL = xpp.nextText();
							}
						}
					}
				}
				catch(IOException e)
				{
					Log.v("StartupFragment:onStartCommand",e.getMessage());
				}
				catch(XmlPullParserException e)
				{
					Log.v("StartupFragment:onStartCommand",e.getMessage());
				}
			}
			
			// start executing 'Create Account' flow. Currently 'Create new account 'and 'Sign In' flows are the same.
			Intent i = new Intent(newActivity, CloudAuthorizationActivity.class);
			i.putExtra(newActivity.CREATE_ACCOUNT_BUTTON_CLICKED, true);
			i.putExtra("clientID", clientID);
			i.putExtra("redirectURL", redirectURL);
			i.putExtra("appId", StcLibCC.id.appId.toString());
			
			newActivity.startActivityForResult(i, NewUnbox.STARTED_CLOUD);
		}
		else if(buttonId == R.id.sign_in_button)
		{			
			d.print(tag, className, "onClick", "User clicked 'Sign In' button.");

			File file = new File(Environment.getExternalStorageDirectory(), "/Intel/cloudsettings.xml");
			if(file.exists())
			{
				try
				{			
					XmlPullParserFactory xppfact = XmlPullParserFactory.newInstance();
					xppfact.setNamespaceAware(true);
					XmlPullParser xpp = xppfact.newPullParser();
					
					FileInputStream fis = new FileInputStream(file);
					xpp.setInput(new InputStreamReader(fis));
					int eventType;
					while ((eventType = xpp.next()) != XmlPullParser.END_DOCUMENT)
					{
						if (eventType == XmlPullParser.START_TAG)
						{
							String name = xpp.getName();
							if(name.compareTo("ClientId") == 0)
							{
								clientID = xpp.nextText();
							}
							if(name.compareTo("RedirectURI") == 0)
							{
								redirectURL = xpp.nextText();
							}
						}
					}
				}
				catch(IOException e)
				{
					Log.v("StartupFragment:onStartCommand",e.getMessage());
				}
				catch(XmlPullParserException e)
				{
					Log.v("StartupFragment:onStartCommand",e.getMessage());
				}
			}
			
			// start executing 'Sing In' flow
			Intent i = new Intent(newActivity, CloudAuthorizationActivity.class);
			i.putExtra("clientID", clientID);
			i.putExtra("redirectURL", redirectURL);
			i.putExtra("appId", StcLibCC.id.appId.toString());
			
			newActivity.startActivityForResult(i, NewUnbox.STARTED_CLOUD);
		}
		else if(buttonId == R.id.skip_sign_in_button)
		{
			Log.v("Unboxing", "StartupFragment: OnClick - User clicked 'Skip Sign In' button.");
			
			// start executing 'Skip Sign In' flow
			if (newActivity != null)
			{	
				d.print(tag, className, "onClick", "Switching to 'UserName' view.");
				UnboxFragmentEnum.username.addOrReplaceFragment(newActivity);
			}				
		}		
	}
		
	@Override
	public void onBackPressed()
	{
		if (newActivity != null)
		{			
			d.print(tag, className, "onBackPressed", "User clicked 'Back' button. Cancelling unboxing and exiting application.");
			newActivity.setFailFinish();;
		}
	}
	
}
