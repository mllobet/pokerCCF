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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.intel.inproclib.R;
import com.intel.inproclib.utility.InProcConstants;
import com.intel.mw.PlatformHelper;
import com.intel.stc.events.CloudAuthorizationEvent;
import com.intel.stc.events.CloudAuthorizationEvent.RegistrationState;
import com.intel.stc.events.InviteRequestEvent;
import com.intel.stc.events.InviteResponseEvent;
import com.intel.stc.events.RegistrationStatusEvent;
import com.intel.stc.events.StcException;
import com.intel.stc.events.UriResultEvent;
import com.intel.stc.interfaces.CloudEventListener;
import com.intel.stc.interfaces.StcConnectionListener;
import com.intel.stc.lib.StcLib;
import com.intel.stc.slib.StcLibFactory;
import com.intel.stc.utility.LoginURIProperties;
import com.intel.stc.utility.StcConstants;
import com.intel.stc.utility.d;
import com.intel.stclibcc.StcLibCC;


public class CloudAuthorizationActivity extends FragmentActivity implements StcConnectionListener,
		CloudEventListener
{

	/********************** Constants  ***************************************/
	final int 					UNBOX 							= 5555;
	final int 					CLOUD_AUTHORIZATION 			= 5557;
	public final String 		tag 							= "Unboxing";
	public final String			className						= "InProc - " + CloudAuthorizationActivity.class.getSimpleName();
		
	//Swati: DevTest Server Keys and RedirectURL from Tory.
	private String			clientID						= ""; 
	private String			redirectURL						= "";
	private String			appId							= "";
	
	//Swati: STCService_Debug Keys: for reference - Replase clientID and redirectURL with appropriate
	//public final String			clientID						= "0081cf4ba55f2f5736879864bbd8cf51";
	//public final String			redirectURL						= "https://www.google.com";
	
	
	final String 				KEYS 							= "keys";

	/********************** Jobids for callbacks  ***************************************/
	Integer						registerJobId 					= -1;
	protected List<Integer> 	jobIds 							= new ArrayList<Integer>();	
	
	/********************** Local Variables ********************************************/
	AlertDialog 				mReauthenAlert 					= null;
	boolean 					initialCloudRegStatus			= false;
	boolean 					cloudAuthorizationIsRegistered	= false;
	int							cloudAuthorizationStatus		= 0;
	String						cloudAuthorizationResult		= null;
	boolean 					mStartedFromCentral 			= false;
	boolean 					reauthen 						= false;
	Bundle 						mSavedBundle 					= null;
	Button 						errorTryAgainButton;
	Button 						errorSkipSignInButton;
	CloudAuthorizationActivity 	mCloudAuthorizationActivity;
	RegistrationState			cloudAuthorizationState			= RegistrationState.NeverRegistered;
	Handler 					mHandler 						= new Handler();
	StcLib 						mLib;
	String[] 					mTokens;
	String 						mLoginUrl, mBadUrl, mGoodUrl;
	WebView 					mWebView;
	LoginURIProperties 			props;
	public boolean				createAccountButtonClicked		= false;
		
	/*********************************************************************/
	/**** Android Activity Life Cycle ************************************/
	/*********************************************************************/
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{		
		super.onCreate(savedInstanceState);		
		mSavedBundle = savedInstanceState;
		
		Bundle extras = getIntent().getExtras();
		if(extras != null)
		{
			createAccountButtonClicked = extras.getBoolean("CREATEACCOUNTBTNCLICKED");
			
			clientID = extras.getString("clientID");
			redirectURL = extras.getString("redirectURL");
			appId = extras.getString("appId");
		}
		
		initializeCloud();				
	}

	@Override
	protected void onResume() 
	{
		super.onResume();
		if (!reauthen)
		{			
			d.print(tag, className, "onResume", "Initiating cloud registration.");
			createInstanceLib(false);
			checkCloudRegistration();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) 
	{
		/*
		if(accessCode != null)
			outState.putString(KEYS, accessCode);
			*/
		
		/* 2.5 old code
		if (mTokens != null)
			outState.putStringArray(KEYS, mTokens);*/

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() 
	{
		super.onPause();

		if (mLib != null && mLib instanceof StcLibCC) 
		{
			try 
			{
				mLib.disconnectFromPlatform();
			} 
			catch (StcException exception) 
			{				
				d.error(tag, className, "onPause", exception.getMessage());
			}
			mLib = null;
		}

		if (mWebView != null) 
		{
			try 
			{
				Class.forName("android.webkit.WebView")
						.getMethod("onPause", (Class[]) null)
						.invoke(mWebView, (Object[]) null);
			} 
			catch (Exception exception) 
			{
				d.error(tag, className, "onPause", exception.getMessage());
			}
		}

		// We need to restart the library if this was started from central.
		if (mStartedFromCentral) 
		{
			final Intent centralRestartIntent = new Intent(
					StcConstants.INTEL_MW_SERVICE_INTENT);
			centralRestartIntent.putExtra(StcConstants.CAA_LAUNCH_ACTIVITY,
					true);
			startService(centralRestartIntent);
		}

	}

	@Override
	public void onBackPressed() 
	{
		if (mReauthenAlert != null && mReauthenAlert.isShowing()) 
		{
			mReauthenAlert.dismiss();
			mReauthenAlert = null;
		} 
		else if (mWebView != null && mLoginUrl != null && mWebView.canGoBack()
				&& !mWebView.getUrl().contains(mLoginUrl)) 
		{
			mWebView.loadUrl(mLoginUrl);
		} 
		else 
		{
			d.error(tag, className, "onBackPressed", "Cancel out the cloud registration.");
			setCancelAndFinish();
			super.onBackPressed();
		}
	}
	
	/*********************************************************************/
	/**** Private Helpers - Initializing *********************************/
	/*********************************************************************/
	private void initializeCloud()
	{
		try 
		{
			// Do we need to show a
			Intent i = getIntent();
			if (i != null) {
				Bundle b = i.getExtras();
				if (b != null) {
					long timeTillExpire = -99959;
					if (b.containsKey(StcConstants.MN_TIME)) {
						// Don't start the lib until we confirm with the user.
						reauthen = true;

						timeTillExpire = b.getLong(StcConstants.MN_TIME);

						if (mReauthenAlert != null) {
							mReauthenAlert.dismiss();
							mReauthenAlert = null;
						}

						AlertDialog.Builder builder = new AlertDialog.Builder(
								this);
						builder.setTitle(R.string.notification_renew_registration);
						// Expired
						if (timeTillExpire <= 0) {
							builder.setMessage(R.string.reauthen_expired_body);
							builder.setPositiveButton(R.string.nu_sub_button1,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int arg1) {
											dialog.dismiss();
											mReauthenAlert = null;
											createInstanceLib(false);
										}
									});

							builder.setNegativeButton(R.string.nu_sub_button3,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int arg1) {
											revertToLocal(dialog);
										}
									});
						}
						// Going to expire soon
						else {
							builder.setMessage(R.string.reauthen_not_expired_body);
							builder.setNegativeButton(R.string.nu_sub_button1,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int arg1) {
											dialog.dismiss();
											mReauthenAlert = null;
											createInstanceLib(false);
										}
									});

							builder.setNeutralButton(R.string.nu_sub_button2,
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int arg1) {
											dialog.dismiss();
											mReauthenAlert = null;
											CloudAuthorizationActivity.this
													.finish();
										}
									});

							builder.setPositiveButton(R.string.nu_sub_button3,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											revertToLocal(dialog);
										}
									});
						}

						builder.setCancelable(false);
						mReauthenAlert = builder.create();
						mReauthenAlert.show();

					}

					mStartedFromCentral = b.getBoolean(
							StcConstants.CAA_FROM_CENTRAL, false);
				}
			}
			setContentView(R.layout.cloud_progress);

			if (mSavedBundle != null
					&& mSavedBundle.containsKey(KEYS))
				mTokens = mSavedBundle.getStringArray(KEYS);
		} catch (Exception e) {
			setCancelAndFinish();
			d.error(InProcConstants.INPROC_TAG, tag, "onCreate", e);
		}
	}
	
	private void createInstanceLib(boolean cancelCloud) 
	{
		try 
		{

			d.print(tag, className, "onBackPressed", "Creating an instance of StcLib.");
			mLib = StcLibFactory.getLib(UUID.fromString(appId));
			if(mLib == null)
			{
				mLib = new StcLibCC(PlatformHelper.GetPath(), CloudAuthorizationActivity.this);
			}

			mLib.addCloudEventListener(this);
			mCloudAuthorizationActivity = this;			
			
		}
		catch(Exception e)
		{
			Log.v("createInstanceLib", e.toString());
		}
	}
	
	private void checkCloudRegistration()
	{
		if (mLib != null)
		{
			try
			{
				Integer jobId = mLib.checkCloudAuthorization();
				jobIds.add(jobId);
			}
			catch (StcException exception)
			{
				d.error(tag, className, "checkCloudRegistration", exception.getMessage());
			}
		}	
	}
	
	private void queryCloudRegistration()
	{
		if (mLib != null)
		{
			try
			{
				Integer jobId = mLib.queryRegistrationStatus();
				jobIds.add(jobId);
			}
			catch (StcException exception)
			{
				d.error(tag, className, "queryCloudRegistration", exception.getMessage());
			}
		}	
	}
	
	private void checkNetworkAvailability()
	{
		//check if internet or 3G - 4G network available is available. If available, then only start cloud registration process.			
		if(IsInternetAvailable() || IsDataNetworkAvailable()) // 
		{
			loadWebPage();
		}
		else
		{
			// Internet is not available. Show 'No Internet' error message.			
			mCloudAuthorizationActivity.showNoInternetErrorPage();
		}
	}
	
	private boolean IsInternetAvailable()
	{
		boolean retval = false;
		
		ConnectivityManager connManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo myWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		
		if(myWifi!= null && myWifi.isAvailable() && myWifi.isConnected())
		{
			retval = true;
		}
		
		return retval;
	}
	
	private boolean IsDataNetworkAvailable()
	{
		boolean retval = false;
		
		ConnectivityManager connManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo myDataNetwork = connManager.getActiveNetworkInfo();
		if(myDataNetwork != null)
		{
			int netType = myDataNetwork.getType();
			if(netType == ConnectivityManager.TYPE_MOBILE)
			{
				if(myDataNetwork.isAvailable())
				{
					retval = true;
				}
			}
		}
		
		return retval;
	}
		
	/*********************************************************************/
	/**** Private Helpers - Initializing ************************************/
	/*********************************************************************/	
	private void queryLoginURI()
	{
		try
		{					
			//props = new LoginURIProperties("e284d4c91b608b7d3a45b9f0ed9055f5", "https://www.google.com");
			props = new LoginURIProperties(clientID, redirectURL);
			props.getExtendedProperties().put("user-agent-env", "embedded");
			
			// Add extended property for "default_action", so that the correct tab is selected,
            // depending on if the user pressed the "Create Account" or "Sign In" button.
			if(createAccountButtonClicked)
			{
				props.getExtendedProperties().put("default_action", "create");
			}
			else
			{
				props.getExtendedProperties().put("default_action", "signin");
			}
				
			props.setState(UUID.randomUUID().toString());
			Integer jobId = mLib.queryLoginURI(props);
			jobIds.add(jobId);
		}
		catch (StcException exception)
		{
			d.error(tag, className, "queryLoginURI", exception.getMessage());
		}
	}	
	
	private void loadWebPage()
	{
		// Cloud Urls			
		//mLoginUrl = "https://api.intel.com/identityui/v2/auth?client_id=f077732fdeb51c189789f74a74d1676e&redirect_uri=urn:intel:identity:oauth:oob:async&scope=user:details user:scope profile:full profile:basic profile:full:write&state=3f2504e04f8911d39a0c0305e82c3301&response_type=code&provider=intel&auto_register=true";

		//mLoginUrl = "https://api.intel.com/identityui/v2/auth?client_id=f077732fdeb51c189789f74a74d1676e&redirect_uri=urn%3Aintel%3Aidentity%3Aoauth%3Aoob%3Aasync&scope=profile%3Afull%20user%3Adetails&state=2c382d31-ea46-4b3d-8f12-7296f1bb5a15&response_type=code&provider=intel&user-agent-env=embedded";
	
		if (mLoginUrl == null)
		{
			setCancelAndFinish();
			return;
		}

		mHandler.post(new Runnable() {

			@SuppressLint("SetJavaScriptEnabled")
			@Override
			public void run()
			{
				d.print(tag, className, "loadWebPage", "Loading web page.");
				setContentView(R.layout.cloud_webview);
				mWebView = (WebView) findViewById(R.id.cloud_wv);

				// Setup Webview
				if (mWebView != null)
				{
					mWebView.setWebViewClient(new WebViewInterceptor());
					mWebView.getSettings().setJavaScriptEnabled(true);
					int size = getResources().getConfiguration().screenLayout
							& Configuration.SCREENLAYOUT_SIZE_MASK;
					if (size <= Configuration.SCREENLAYOUT_SIZE_NORMAL)
					{
						mWebView.getSettings().setUseWideViewPort(true);
					}
					mWebView.setHorizontalScrollBarEnabled(true);

					mWebView.getSettings().setJavaScriptEnabled(true);
					// Load Loginpage
					mWebView.loadUrl(mLoginUrl);
				}
			}
		});
	}

	private class WebViewInterceptor extends WebViewClient 
	{
			@Override
		public void onPageFinished(WebView view, String url)
		{						
			Log.v("CloudRegistration", "CloudAuthorizationActivity: WebViewInterceptor: onPageFinished.");			
		}

		public void onReceivedSslError(WebView view, SslErrorHandler handler,
				SslError error) {
			// TODO: this might need to be handled better (user interaction?)
			handler.proceed();
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) 
		{
        	if(url.contains("code="))
        	{
        		setContentView(R.layout.cloud_signing_in);
        		int index = url.indexOf("code=") + 5;
        		String code = url.substring(index , url.length()-index);
        		StringTokenizer token = new StringTokenizer(code, "&");
        		code = token.nextToken();
        		
        		// Send accesscode to stack to complete cloud registration.
        		registerWithCloud(code);
        	}
			super.onPageStarted(view, url, favicon);		
		}
		/**  This AsyncTask is currently unused.  It is not currently needed, but it might be 
		 *   worth keeping if we need to move back to an Async approach in the future.
		private class GetCodeTask extends AsyncTask<Void, Void, String>
			{
				BufferedReader bufferedReader = null;
				HttpClient httpClient = new DefaultHttpClient();
				HttpPost request = new HttpPost("https://api.intel.com/identity/v2/authcode");
				List<NameValuePair> postParameters = new ArrayList<NameValuePair>(2);
				String code = "";				
				
				protected void onPreExecute()
				{					
				}
				
				@Override
				protected String doInBackground(Void... arg0) 
				{								
					d.log(tag, className, "GetCodeTask: doInBackground");
					
					postParameters.add(new BasicNameValuePair("client_id", "f077732fdeb51c189789f74a74d1676e"));
					postParameters.add(new BasicNameValuePair("state", "3f2504e04f8911d39a0c0305e82c3301"));
					
					try
					{
						UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParameters);
						request.setEntity(entity);
						HttpResponse response = httpClient.execute(request);
						
						if(response == null)
						{							
							d.error(tag, className, "GetCodeTask: doInBackground", "Response received is null.");
							mCloudAuthorizationActivity.setCancelAndFinish();
							return null;
						}
						
						
						d.print(tag, className, "GetCodeTask: doInBackground", "Response received is " + response.toString());
						bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
						
						while ((code = bufferedReader.readLine()) != null) 
						{
							if(code.contains("code"))
								break;
						}
						
						bufferedReader.close();
						try 
						{
							JSONObject oo = new JSONObject(code);
							accessCode = oo.getString("code");
							afterGettingAccessCode = true;
							registerWithCloud(accessCode);							
							
						}
						catch (JSONException exception) 
						{		
							d.error(tag, className, "GetCodeTask: doInBackground", exception.getMessage());							
							mCloudAuthorizationActivity.setCancelAndFinish();
							exception.printStackTrace();
						}
					} 
					catch (ClientProtocolException exception) 
					{			
						d.error(tag, className, "GetCodeTask: doInBackground", exception.getMessage());							
						mCloudAuthorizationActivity.setCancelAndFinish();
						exception.printStackTrace();
					} 
					catch (IOException exception) 
					{			
						d.error(tag, className, "GetCodeTask: doInBackground", exception.getMessage());						
						mCloudAuthorizationActivity.setCancelAndFinish();						
						exception.printStackTrace();				
					}			
					
					return accessCode;
				}
				
				@SuppressWarnings("unused")
				protected void onPostExecute()
				{					
					if(isCancelled())
						return;
				}
			}
			*/		
	}
	
	/*********************************************************************/
	/**** Cloud Registration Complete ************************************/
	/*********************************************************************/
	
	private void registerWithCloud(String accessCode)
	{
		if(registerJobId > 0)
		{
			d.error(tag, className, "", "trying to call register with pending registration, ignoring.");
			return;
		}
		if(accessCode != null && mLib != null)
		{
			try
			{	
				d.print(tag, className, "finishCloud", "About to call 'registerWithCloud' to complete cloud registration.");
				registerJobId = mLib.registerWithCloud(accessCode);
				jobIds.add(registerJobId);				
			}
			catch (StcException exception)
			{
				d.error(tag, className, "finishCloud", exception.getMessage());
				// Encountered error while registering with cloud.
				setCancelAndFinish();				
			}
		}
		else
		{
			// For some reason, code received from cloud is null or mLib instance is null. 
			d.error(tag, className, "finishCloud", "AccessCode is null. About to show cloud error page.");
			showGeneralCloudErrorPage();
		}
	}
	
	private void setSuccessAndFinish()
	{
	// TODO: remove because its old cloud?
	//	InProcService.hideAuthorizationNotification(this);

		this.setResult(StcConstants.STC_RESULT_OK);
		this.finish();
	}
	
	/*********************************************************************/
	/**** Cloud Error Handling *******************************************/
	/*********************************************************************/	
	private void showGeneralCloudErrorPage()
	{
		d.print(tag, className, "showGeneralCloudErrorPage", "Showing general cloud error page.");
		setContentView(R.layout.cloud_error);
		
		errorTryAgainButton = (Button)findViewById(R.id.cloud_error_try_again_button);
		errorTryAgainButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				// Call swatis test cloud code.
				checkNetworkAvailability();
			}
		});
		
		errorSkipSignInButton = (Button)findViewById(R.id.cloud_error_skip_sign_in_button);
		errorSkipSignInButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				
				// Let NewUnbox know that user has clicked 'Skip Sign In' button and wishes to be 'Local Identity User'
				setSkipSignInAndFinish();
			}
		});
	}
	
	private void showNoInternetErrorPage()
	{
		d.print(tag, className, "showGeneralCloudErrorPage", "Showing no internet error page.");
		setContentView(R.layout.cloud_no_internet_error);
		
		errorTryAgainButton = (Button)findViewById(R.id.cloud_error_try_again_button);
		errorTryAgainButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				// Call swatis test cloud code.
				checkNetworkAvailability();
			}
		});
		
		errorSkipSignInButton = (Button)findViewById(R.id.cloud_error_skip_sign_in_button);
		errorSkipSignInButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				
				// Let NewUnbox know that user has clicked 'Skip Sign In' button and wishes to be 'Local Identity User'
				setSkipSignInAndFinish();
			}
		});
	}
			
	private void setSkipSignInAndFinish()
	{	
		setResult(StcConstants.STC_RESULT_SKIP_SIGN_IN);
		finish();
	}
	
	private void setCancelAndFinish() 
	{
		setResult(StcConstants.STC_RESULT_CANCELED);
		finish();
	}
		
	/*********************************************************************/
	/**** Cloud Callbacks ************************************************/
	/*********************************************************************/
		
	public void uriResult(UriResultEvent uriResultEvent) 
	{
		if(uriResultEvent != null)
		{			
//			if(!jobIds.contains(uriResultEvent.getJobId()))
//				return;
					
			mLoginUrl = uriResultEvent.getUri();		
			Log.v("Unboxing", "CloudAuthorization: uriResult - login URI received: " + mLoginUrl);
			
			jobIds.remove(Integer.valueOf(uriResultEvent.getJobId()));
			if(mLoginUrl != null)
			{
				if (mHandler != null)
				{
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							mCloudAuthorizationActivity.checkNetworkAvailability();
						}
					});
				}		
			}
			else
			{
				if (mHandler != null)
				{
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							mCloudAuthorizationActivity.showGeneralCloudErrorPage();
						}
					});
				}
			}
		}
		else
		{
			// uriResultEvent callback is null. We should not be here.
			if (mHandler != null)
			{
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mCloudAuthorizationActivity.showGeneralCloudErrorPage();
					}
				});
			}
		}
	} 
	
	public void cloudAuthorization(CloudAuthorizationEvent cloudAuthorizationEvent) 
	{
		if(cloudAuthorizationEvent != null)
		{		
			Integer id = Integer.valueOf(cloudAuthorizationEvent.getJobId());
			
			if(!jobIds.contains(id))
				return;
			
			jobIds.remove(id);	
			
			cloudAuthorizationIsRegistered = cloudAuthorizationEvent.getIsRegistered();			
			cloudAuthorizationState = cloudAuthorizationEvent.getState();
			
			if(cloudAuthorizationIsRegistered == true && cloudAuthorizationState == RegistrationState.RegisteredWithGoodCert)
			{
				d.print(tag, className, "cloudAuthorization", "Cloud registration status received from stack is "+ cloudAuthorizationIsRegistered);
				// Cloud registration successful.
				mCloudAuthorizationActivity.setSuccessAndFinish();	
				if(id == registerJobId) registerJobId = -1;
				return;
			}		
			
			if(id == registerJobId)
			{
				// Cloud registration failed even though we received code. This should not happen.
				d.print(tag, className, "cloudAuthorization", "Cloud registration failed.");
				
				if (mHandler != null)
				{
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							mCloudAuthorizationActivity.showGeneralCloudErrorPage();
						}
					});
				}
				registerJobId = -1;
			} 
			else
			{
				// First time cloud registration.
				d.print(tag, className, "cloudAuthorization", "First time cloud registration. About to call queryLoginURI.");
				mCloudAuthorizationActivity.queryLoginURI();
			}	
		}
	}

	public void registrationStatus(RegistrationStatusEvent registrationStatusEvent) 
	{		
		if(registrationStatusEvent != null)
		{			
			if(!jobIds.contains(registrationStatusEvent.getJobId()))
				return;
			
			jobIds.remove(Integer.valueOf(registrationStatusEvent.getJobId()));
			
			int value = registrationStatusEvent.getStatus();
			
			// TODO: Swati - need better return code here than just 1 and 0.
			if(value == 1)
			{
				// We failed in cloud registration.
				setCancelAndFinish();
			}
			else if(value == 0)
			{				
				// We completed the cloud registration.
				// queryCloudRegistration();
				setSuccessAndFinish();
			}
		}
		
	}
	
	/*********************************************************************/
	/**** Need Cleanup - Do not use old cloud APIs. All is commented *****/
	/*********************************************************************/
	/*private void showNoInternet()
	{
		if (mHandler != null)
		{
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					AlertDialog.Builder b = new AlertDialog.Builder(
							CloudAuthorizationActivity.this);
					b.setMessage(R.string.cloud_no_connection_new);
					b.setCancelable(false);
					b.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									setCancelAndFinish();
								}
							});
					b.create().show();
				}
			});
		}
	}
	*/
	
	private void oldfinishCloud(String[] tokens)
	{
		/*if (tokens != null && tokens.length == 2 && mLib != null)
		{
			try
			{
				mLib.completeCloudAuthorization(tokens[0], tokens[1]);
			} 
			catch (StcException e) 
			{
				setCancelAndFinish();
			}
		}

		// TODO: Swati - we do not show cloud registration complete anymore. May need to delete this
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				setContentView(R.layout.cloud_progress);
				TextView title = (TextView) findViewById(R.id.cloud_progress_title);
				if (title != null)
					title.setText(R.string.nu_title_registering);
			}
		});*/
	}
	
	/* StcCloudAuthorizationListener */

	private void revertToLocal(final DialogInterface dialog) {
		/*AsyncTask<Void, Void, Void> mTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected void onPreExecute() {
				TextView tv = (TextView) findViewById(R.id.cloud_progress_title);
				if (tv != null)
					tv.setVisibility(View.GONE);
				tv = (TextView) findViewById(R.id.cloud_progress_label);
				if (tv != null)
					tv.setVisibility(View.GONE);
				dialog.dismiss();
				mReauthenAlert = null;
				super.onPreExecute();
			}

			@Override
			protected Void doInBackground(Void... arg0) {
				createInstanceLib(true);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				CloudAuthorizationActivity.this.finish();
				super.onPostExecute(result);
			}
		};
		mTask.execute();*/
	}
	
	/* Conn Listener -Ignored- */

	@Override
	public void connectionRequest(InviteRequestEvent ire) {
	}

	@Override
	public void connectionCompleted(InviteResponseEvent ire) {
	}

	/* Notification BS */

	static final boolean honeycomb = android.os.Build.VERSION.SDK_INT >= 11;

	public static void createNotification(Context context, int note_id,
			String title, String message, PendingIntent contentIntent) {
		/*Notification n = null;

		if (honeycomb)
			n = createV11Notification(context, note_id, title, message,
					contentIntent);
		else
			n = createV4Notification(context, note_id, title, message,
					contentIntent);

		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(note_id, n);*/
	}

	@SuppressWarnings("deprecation")
	private static Notification createV4Notification(Context context,
			int note_id, String title, String message,
			PendingIntent contentIntent) {
		/*Notification n = new Notification(R.drawable.ccf_icon, title,
				System.currentTimeMillis());

		n.defaults |= Notification.DEFAULT_ALL;
		n.flags = Notification.FLAG_AUTO_CANCEL;
		n.contentIntent = contentIntent;
		n.tickerText = message;

		n.setLatestEventInfo(context, title, message, contentIntent);

		return n;*/
		return null;
	}

	@TargetApi(16)
	private static Notification createV11Notification(Context context,
			int note_id, String title, String message,
			PendingIntent contentIntent) {
		/*Builder mNb = new Builder(context);

		mNb.setWhen(System.currentTimeMillis());
		mNb.setDefaults(Notification.DEFAULT_ALL);
		mNb.setAutoCancel(true);
		mNb.setContentTitle(title);
		mNb.setContentText(message);
		mNb.setContentIntent(contentIntent);
		mNb.setSmallIcon(R.drawable.ccf_icon);

		//return mNb.getNotification();
		return mNb.build();*/
		return null;
	}

	public static void removeNotification(Context context, int note_id) {
		/*NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(note_id);*/
	}
		
	/* Notification */

	private static final String CLOUD_NOTE_PREF = "_cloud";
	private static final String CLOUD_NOTE_KEY = "_key";

	public static void showCloudNote(Context context) {
		/*
		final int cloud_id = getCloudNoteId(context);

		Intent i = new Intent(context, CloudAuthorizationActivity.class);
		PendingIntent cloudPendingIntent = PendingIntent.getActivity(context,
				cloud_id, i, PendingIntent.FLAG_ONE_SHOT);

		createNotification(context, cloud_id,
				context.getString(R.string.notification_renew_registration),
				context.getString(R.string.cloud_reauthenticate),
				cloudPendingIntent);*/
	}

	public static void removeCloudNote(Context context) {
		/*final int cloud_id = getCloudNoteId(context);

		removeNotification(context, cloud_id);*/
	}

	private static int getCloudNoteId(Context context) {
/*
		SharedPreferences result = context.getSharedPreferences(
				CLOUD_NOTE_PREF, Activity.MODE_PRIVATE);
		int noteId = result.getInt(CLOUD_NOTE_KEY, 0);

		if (noteId == 0) {
			Random r = new Random(System.currentTimeMillis());
			noteId = r.nextInt();

			Editor editor = result.edit();
			editor.putInt(CLOUD_NOTE_KEY, noteId);
			editor.commit();
		}

		return noteId;*/
		return 0;
	}
}
