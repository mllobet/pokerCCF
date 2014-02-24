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

package com.intel.multiconnect;

import java.util.EnumSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.intel.stc.events.DiscoveryNodeUpdateEvent;
import com.intel.stc.events.StcException;
import com.intel.stc.interfaces.StcDiscoveryNodeUpdateEventListener;
import com.intel.stc.lib.StcLib;
import com.intel.stc.lib.StcLib.NodeFlags;
import com.intel.stc.slib.IStcServInetClient;
import com.intel.stc.utility.StcDiscoveryNode;

/***
 * Because all of our Activities need to bind to the service, this abstract class 
 * encapsulates the grunt work of binding to the CCFManager. Subclasses
 * must implement the onServiceConnected / onServiceDisconnected methods.
 * <p>
 * This class automatically handles the bind and unbind on pause/resume. The subclass is
 * responsible for calling doStartService() and doStopService(). doStartService() should be 
 * called in the 'entry point' activities for the application. doStopService() should be
 * called when the application is done and the service may exit.
 * <p>
 * There is nothing c3 specific here.
 */
public abstract class AbstractServiceUsingActivity extends Activity implements IStcServInetClient, ISimpleDiscoveryListener,StcDiscoveryNodeUpdateEventListener 
{
	public static final String LOGC = "PokerCCF";
	public static final String ServiceIntent = "com.intel.multiconnect.CCFManager";
	
	private static final int STCLIB_ACTIVITY_REQUEST = 23;

	protected CCFManager serviceManager;
	private CCFManagerServiceConnection mConnection = new CCFManagerServiceConnection();
	boolean isBound = false;

	Handler myHandler = new Handler();
	private Bundle	bundle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bundle = this.getIntent().getExtras();
		doStartService();
	}
	/*** 
	 * Bind to the service when resuming.
	 */
	@Override
	protected void onResume() {
		Log.i(LOGC, "resuming");
		doBindService();
		super.onResume();
	}

	/***
	 * Unbind from the service when pausing. Note that unbinding will not stop the
	 * service (nor should it) if doStartService has already been called.
	 */
	@Override
	protected void onPause() {
		Log.i(LOGC, "pausing");
		doUnbindService();
		super.onPause();
	}
	
	/***
	 * Tells the subclass that the connected Service's StcLib has been prepared and normal STC functions can be started.
	 */
	abstract protected void onStcLibPrepared();

	/* private method to do the binding */
	private void doBindService() {
		if(!isBound)
		{
			Log.i(LOGC, "binding service" + mConnection.toString());
			Intent servIntent = new Intent(ServiceIntent);
			isBound = bindService(servIntent, mConnection, 0);
			if( !isBound )
				Log.i(LOGC, "service did not bind.");
		}
	}

	
	private void doUnbindService() {
		if(isBound)
		{
			Log.i(LOGC, "unbinding service ");
			isBound = false;
			unbindService(mConnection);
		}
	}
	
	/***
	 * Must be called by the 'starting activity'. Can be called more than once, but
	 * should not be called anytime after 'shutdown' has been called.
	 */
	protected void doStartService() {
		Log.i(LOGC, "starting service");
		Intent servIntent = new Intent(ServiceIntent);
		startService(servIntent);		
	}
	
	/*** 
	 * Called to indicate that the application is finally existing.
	 */
	protected void doStopService() {
		Log.i(LOGC, "shutting down");
		Intent servIntent = new Intent(ServiceIntent);
		
		doUnbindService();		
		stopService(servIntent);
	}
	
	/* StcServiceInetClient implementation 
	 *
	 * The following overridden functions enable default cloud logic and lib prepared notifications.
	 */

	@Override
	public void platformError()
	{	
		Log.e(LOGC, "platformError");
	}

	@Override
	public void libPrepared(StcLib lib)
	{
		if(lib != null) {
			onStcLibPrepared();
			serviceManager.parseInitBundle(bundle);
//			lib.addStcDiscoveryNodeUpdateEventListener(AbstractServiceUsingActivity.this);
		}
	}

	@Override
	public void platformMissing()
	{
		Log.e(LOGC, "Platform is missing.  Is the inproc_lib referenced properly?");
	}

	@Override
	public void requestStartActivityForResult(Intent i)
	{
		startActivityForResult(i, STCLIB_ACTIVITY_REQUEST);		
	}
	
	/* ServiceConnection implementation */
	
	public class CCFManagerServiceConnection implements ServiceConnection
	{
		
		public void onServiceConnected(ComponentName className, IBinder binder) 
		{
			synchronized(this) {
				Log.i(LOGC, "service connected.");

				serviceManager = (CCFManager)((CCFManager.StcServInetBinder)binder).getService();
				if(serviceManager!=null)
					serviceManager.addListener(AbstractServiceUsingActivity.this);
					serviceManager.setLibPreparedCallback(AbstractServiceUsingActivity.this, myHandler);
				}
			}
	
		public void onServiceDisconnected(ComponentName className) 
		{
			Log.i(LOGC, "service disconnected.");
			if(serviceManager!=null){
				serviceManager.removeListener(AbstractServiceUsingActivity.this);
				serviceManager = null;
			}
		}
	};

	//STC callback
			@Override
			public void discoveryNodeUpdate(final DiscoveryNodeUpdateEvent event) {
				
				Log.i(LOGC, "discoveryNodeUpdate event.getStatus()"+event.getStatus()+event.getNode().getName()+"Event type: "+event.getEventType()+event.getDiscoveryNodeError());
				if(event.getStatus() == 1)
				{
					if(!event.getDiscoveryNodeError().equals(DiscoveryNodeUpdateEvent.DiscoveryNodeError.noError)){
						myHandler.post(new Runnable() {
							
							@Override
							public void run() {
								if(event.getDiscoveryNodeError().equals(DiscoveryNodeUpdateEvent.DiscoveryNodeError.nodeNotFound)){
									createDN(event.getNode().getName());
								}else{
									Log.e(LOGC, "Discovery Node update event failed - "+event.getDiscoveryNodeError());
									displayErrorToast(event.getDiscoveryNodeError().toString(),event.getNode());
								}
								
							}
						});
					}
					return;
				}
				
				StcDiscoveryNode node = event.getNode();
				
				switch(event.getEventType())
				{
					case CREATE:
						
						boolean value = serviceManager.addDiscoveryNode(new NodeWrapper(node,true));
						if(!value){
							Log.i(LOGC , "Node '"+node.getName()+"' already exist");
						}else{
							Log.i(LOGC , "Creating Node "+node.getName());
						}
						break;
					case DELETE:
						Log.i(LOGC , "Deleting Node "+node.getName());
						break;
					case JOIN:
						boolean temp = serviceManager.addDiscoveryNode(new NodeWrapper(node,true));
						if(!temp){
							Log.i(LOGC , "Node '"+node.getName()+"' already joined.");
						}else{
							Log.i(LOGC , "Joining Node "+node.getName()+node.getNodeState());
						}
						break;
					case LEAVE:
						Log.i(LOGC , "Leaving Node "+node.getName());
						break;
					case PUBLISH:
						Log.i(LOGC , "Publishing Node "+node.getName());
						break;
					default:
						break;
				}
			}
			
			//Display toast-Node already joined.
			private void nodeAlreadyJoined(final String value){
				myHandler.post(new Runnable() {
					
					@Override
					public void run() {
						final AlertDialog.Builder builder = new AlertDialog.Builder(AbstractServiceUsingActivity.this);
						builder.setTitle("Node '"+value+"' already joined.");
						builder.setCancelable(true);
						
						builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								if(builder!=null)
									dialog.dismiss();
							}
						});
						builder.show();
					}
				});
				
			}
			
			/*Start**************Discovery Node events**********/
			
			//This method will be used for validating Cloud server is up and running
			//before making createNode, joinNode and leaveNode calls.
			private boolean validateCloudServerStatus() throws StcException{
				StcLib lib = serviceManager.getSTCLib();
				if(lib!=null && lib.queryDiscoveryNodeServiceStatus() == 0){
					return true;
				}else{
					myHandler.post(new Runnable() {
						
						@Override
						public void run() {
							final AlertDialog.Builder builder = new AlertDialog.Builder(AbstractServiceUsingActivity.this);
							builder.setTitle("Warning");
							builder.setMessage("XMPP service is not online, try later.");
							builder.setCancelable(true);
							
							builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									if(builder!=null)
										dialog.dismiss();
								}
							});
							builder.show();
						}
					});
					return false;
				}
			}
			
			public void createDN(String temp){
				try {
					if(validateCloudServerStatus()){
						EnumSet<NodeFlags> flags = EnumSet.of(NodeFlags.PUBLISH);
						StcLib lib = serviceManager.getSTCLib();
						if(lib == null)
							return;
						int status = lib.createDiscoveryNode(MultiConnectRegisterApp.id.appId, temp, flags);
						displayToast("createDiscoveryNode Status:"+status);
						Log.i(LOGC, "Creating Node " + temp + " Status: " + status);
					}
				} catch (StcException e) {
					e.printStackTrace();
					if(e.errorCode == -10){
						nodeAlreadyJoined(temp);
					}
				}
			}
			//Join a discovery node
			public int joinDN(String temp)
			{
				try
				{
					if(validateCloudServerStatus()){
						EnumSet<NodeFlags> flags = EnumSet.of(NodeFlags.PUBLISH);
						StcLib lib = serviceManager.getSTCLib();
						if(lib == null)
							return -1;
						int status = lib.joinDiscoveryNode(MultiConnectRegisterApp.id.appId, temp, flags);
						displayToast("joinDiscoveryNode Status:"+status);
						Log.i(LOGC, "Joining Node " + temp + " Status: " + status);
						return status;
					}
				}
				catch (StcException e)
				{
					e.printStackTrace();
					if(e.errorCode == -10){
						nodeAlreadyJoined(temp);
					}
				}
				return -1;
			}
			
			/**
			 * This method is used to leave the join node.
			 * @param Value
			 * 			node value to be removed.
			 */
			public void leaveDN(String value)
			{
				try
				{
					if(validateCloudServerStatus()){
						EnumSet<NodeFlags> flags = EnumSet.of(NodeFlags.PUBLISH);
						StcLib lib = serviceManager.getSTCLib();
						if(lib == null)
							return;
						int status = lib.leaveDiscoveryNode(MultiConnectRegisterApp.id.appId, value, flags);
						displayToast("leaveDiscoveryNode Status:"+status);
						Log.i("", "Leaving Node " + value + " Status: " + status);
					}
				}
				catch (StcException e)
				{
					e.printStackTrace();
				}
			}
			
	/*End**************Discovery Node events**********/	
			//To display android toast message.
			public void displayToast(String value){
				Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
			}
			
			//Display toast- Discovery Node update failed.
			private void displayErrorToast(final String value, final StcDiscoveryNode node){
				
				final AlertDialog.Builder builder = new AlertDialog.Builder(AbstractServiceUsingActivity.this);
				builder.setTitle("Discovery node update failed - "+value);
				builder.setCancelable(true);
				
				builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if(builder!=null){
							dialog.dismiss();
							serviceManager.removeDiscoveryNode(node);
						}
							
					}
				});
				builder.show();
				
			}
}
