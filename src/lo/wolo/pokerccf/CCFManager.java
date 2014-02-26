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
package lo.wolo.pokerccf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lo.wolo.pokerengine.actions.*;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.intel.stc.events.InviteRequestEvent;
import com.intel.stc.events.InviteResponseEvent;
import com.intel.stc.events.StcException;
import com.intel.stc.events.StcSessionUpdateEvent;
import com.intel.stc.interfaces.IStcActivity;
import com.intel.stc.interfaces.StcConnectionListener;
import com.intel.stc.interfaces.StcLocalSessionUpdateListener;
import com.intel.stc.interfaces.StcSessionUpdateListener;
import com.intel.stc.ipc.STCLoggingLevel;
import com.intel.stc.ipc.STCLoggingMode;
import com.intel.stc.ipc.STCLoggingModule;
import com.intel.stc.lib.StcLib;
import com.intel.stc.slib.StcServiceInet;
import com.intel.stc.utility.StcApplicationId;
import com.intel.stc.utility.StcDiscoveryNode;
import com.intel.stc.utility.StcSession;
import com.intel.stc.utility.StcSocket;

/**
 * This part of the application has the majority of the c3 integration. All c3
 * applications written to the android c3 SDK require a subclass of
 * StcServiceInet but SimpleDiscovery puts the entire engine for communication in
 * this service. That avoids all race conditions as activities bind and unbind,
 * suspend and resume.
 * <p>
 * CCFManager has three sections that are c3 specific. The first are the
 * set of method overrides required by our StcServiceInet superclass. Those are
 * listed just past the comment section "METHODS REQUIRED BY StcServiceInet" and
 * are needed by any subclass of StcServiceInet.
 * <p>
 * The second set of methods handle c3 event call backs. These are in the
 * section "STC NOTIFICATION METHODS" that handles discovery.
 * <p>
 * The third set of methods are in the section "CALLBACKS FROM BUNDLE PARSING"
 * and handle callbacks from the bundle parsing. Both the platform 
 * and STC_Central can start an application for a particular purpose. 
 * That purpose is encoded in the bundle.
 * The SDK handles bundle parsing, and returns the results through callbacks
 * into the IStcActivity.
 */
public class CCFManager extends StcServiceInet implements StcSessionUpdateListener, StcConnectionListener,
		StcLocalSessionUpdateListener, IStcActivity {

	private static final String LOGC = "PokerCCF";
	private boolean bundleParsed = false;
	private Bundle initBundle;
	private ArrayList<ISimpleDiscoveryListener> listeners = new ArrayList<ISimpleDiscoveryListener>();
	private ArrayList<NodeWrapper> discoveryNodeList = new ArrayList<NodeWrapper>();
	
	public ArrayList<Action> actionList;
	public HashMap<UUID, RemoteUser> remoteSessionsMap = new HashMap<UUID, RemoteUser>();
	public ArrayList<RemoteUser> remoteUsersList = new ArrayList<RemoteUser>();
	public static int connection_Counter = 0;
	public static final int MAX_CONNECTION = 10;

	private static final String TAG = "CCFManager";
	
	 public enum SessionState{
		CONNECTED,
		NOTCONNECTED,
		INVITE_SENT,
		TIME_OUT,
		DISCONNECTED
	}
	
	// /
	// / CALLBACKS FROM BUNDLE PARSING
	// /
	@Override
	public void onStartClient(UUID arg0, int arg1) {
		Log.i(LOGC, "onStartClient");
	}

	@Override
	public void onStartNormal() {
		Log.i(LOGC, "onStartNormal");
	}

	@Override
	public void onStartServer(UUID arg0) {
		Log.i(LOGC, "onStartServer");
	}

	// /
	// / STC NOTIFICATION METHODS
	// /
	@Override
	public void sessionUpdated(StcSessionUpdateEvent arg0) {
		postSessionListChanged();
		Log.i(LOGC, "sessionUpdated");
	}

	@Override
	public void localSessionUpdated() {
		Log.i(LOGC, "localSessionUpdated");
	}
	
	//To receive connection response from remote session.
	@Override
	public void connectionCompleted(InviteResponseEvent event) {
		Log.i(LOGC, "connectionCompleted "+event.getSessionGuid()+event.getStatus());
		
		synchronized (remoteSessionsMap) {
			if(remoteSessionsMap.containsKey(event.getSessionGuid())){
				RemoteUser user = remoteSessionsMap.get(event.getSessionGuid());
				try {
					if (event.getStatus() == InviteResponseEvent.InviteStatus.sqtAccepted){
						if(connection_Counter >= MAX_CONNECTION){
							return;
						}
						StcLib lib = getSTCLib();
						if(lib!=null){
							StcSocket socket = lib.getPreparedSocket(event.getSessionGuid(), event.getConnectionHandle());
							user.setDataStream(socket);
							user.setsessionState(SessionState.CONNECTED);
						}
					}else{
						user.setsessionState(event.getStatus() == InviteResponseEvent.InviteStatus.sqtTimeout ? SessionState.TIME_OUT : SessionState.NOTCONNECTED);
					}
					remoteSessionsMap.put(user.getSession().getSessionUuid(), user);
					//ArrayList stuff
					Log.d("CCFManager","User: added");
					
					boolean alreadyInList = false;
					for (RemoteUser u : remoteUsersList) {
						if (user.toString().equals(u.toString())) {
							Log.d(TAG,"RemoteUser already in remoteUserList: " + user.toString());
							alreadyInList = true;
							break;
						}	
					}
					if (!alreadyInList)
						remoteUsersList.add(user);
					Log.d("CCFManager", "size: " + Integer.toString(remoteUsersList.size()));
					Log.d("CCFManager", remoteUsersList.toString());
				} catch (StcException e) {
					Log.e(LOGC, e.toString());
				}
			}
		}
		
		synchronized (listeners) {
			for(ISimpleDiscoveryListener listen :listeners){
				listen.invalidateSessionList();
			}
		}
		connection_Counter = remoteSessionsMap.size();
	}

	//To receive connection request from remote session.
	@Override
	public void connectionRequest(InviteRequestEvent event) {
		Log.i(LOGC, "connectionRequest"+event.getSessionUuid());
		if(connection_Counter >= MAX_CONNECTION){
			return;
		}
		
		try {
			StcLib lib = getSTCLib();
			
			if(lib!=null){
				StcSocket socket = lib.acceptInvitation(event.getSessionUuid(), event.getConnectionHandle());
				synchronized (remoteSessionsMap) {
					if(remoteSessionsMap.containsKey(event.getSessionUuid())){
						RemoteUser user = remoteSessionsMap.get(event.getSessionUuid());
						user.setDataStream(socket);
						user.setsessionState(SessionState.CONNECTED);
						remoteSessionsMap.put(user.getSession().getSessionUuid(), user);
					}
				}
			}
		} catch (StcException e) {
			Log.e(LOGC, e.toString());
		}
		synchronized (listeners) {
			for(ISimpleDiscoveryListener listen :listeners){
				listen.invalidateSessionList();
			}
		}
		connection_Counter = remoteSessionsMap.size();
	}
	
	// /
	// / METHODS REQUIRED BY StcServiceInet
	// /
	@Override
	public Intent GetCloudIntent() {
		
		/*Intent intent = new Intent(getApplicationContext(), CloudAuthorizationActivity.class);
		intent.putExtra("clientID", MultiConnectRegisterApp.id.clientId);
		intent.putExtra("redirectURL", MultiConnectRegisterApp.redirectURL);
		intent.putExtra("appId", MultiConnectRegisterApp.id.appId.toString());*/
		return null;
	}

	@Override
	public Class<?> GetUnboxActivityClass() {
		return null;
	}

	@Override
	public StcApplicationId getAppId() {
		return MultiConnectRegisterApp.id;
	}

	@Override
	public StcConnectionListener getConnListener() {
		return this;
	}

	private void StartAgent(StcLib slib) {
		// Enable this app for Intel CCF Developer Central Online logging.
		//
		// WARNING:  Enabling online debugging should be made only while the product is in a development environment on secure (non-public) networks.
		//				                     It should NOT be enabled for release products as enabling online debugging poses security risks on non-secure networks.
		//				                     Prior to releasing a product, either remove this call or specify OFFLINE logging only.
		// Start the Agent
		UUID appGuid = MultiConnectRegisterApp.id.appId;
		String appName = this.getString(R.string.app_name);

		String logPath = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS).getPath();
		try {
			if (slib != null) {
				if (slib != null) {
					slib.startAgent(appGuid, appName,
							STCLoggingMode.LogMode_Live, logPath
									+ "/multiConnectAgentLog.txt",
							STCLoggingModule.LogModule_All,
							STCLoggingLevel.Info, false);
				}
			}
		} catch (StcException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	//This method will be called by CCF framework, once platform is prepared.
	@Override
	protected void stcLibPrepared(StcLib slib) {
		if (slib == null)
			try {
				throw new Exception(
						"the stclib is null in stcLibPrepared. Have you given your app local socket permissions? Have you given your app a registration object?");
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		slib.setConnectionListener(this);
		slib.setStcSessionListListener(this);
		slib.addLocalSessionListener(this);
		Log.i(LOGC, "registered with the platform");
		
		// bundle will be parsed by parseInitBundle()
		tryParseBundle();
		StartAgent(slib);
//		postSessionListChanged();
	}

	@Override
	protected void stcPlatformMissing() {
		Log.i(LOGC, "stcPlatformMissing");
	}
	
	/**
	 * Save away the invite bundle and try to parse it.
	 * 
	 * @param bundle
	 */
	public void parseInitBundle(Bundle bundle) {
		initBundle = bundle;
		bundleParsed = false;
		tryParseBundle();
	}
	
	/**
	 * Attempt to parse the bundle. We should only do this once. We can only do
	 * this when we have an stclib.
	 */
	private void tryParseBundle() {
		StcLib lib = getSTCLib();
		if (lib == null)
			return;

		synchronized (this) {
			if (bundleParsed)
				return;
			bundleParsed = true;
		}

		lib.parseStartMethod(this, initBundle, this);
	}
	
	//Service callback to inform the UI about sessions list getting updated.
	private void postSessionListChanged() {
		List<StcSession> temp = getSessions();
		
		synchronized (remoteSessionsMap) {
			for(int index=0;index<temp.size();index++){
				StcSession session = temp.get(index);
				
				if(!session.isAvailableCloud() && !session.isAvailableProximity()){
					temp.remove(index);
					if(remoteSessionsMap.containsKey(session.getSessionUuid())){
						remoteSessionsMap.remove(session.getSessionUuid());
					}
				}else{
					if(!remoteSessionsMap.containsKey(session.getSessionUuid())){
						remoteSessionsMap.put(session.getSessionUuid(), new RemoteUser(session, this));
					}
					StcLib lib = this.getSTCLib();
					
					try {
						if(lib!=null && lib.queryLocalSession().getSessionUuid().compareTo(session.getSessionUuid())>0
								&& remoteSessionsMap.containsKey(session.getSessionUuid())){
							RemoteUser user = remoteSessionsMap.get(session.getSessionUuid());
							if(user.getsessionState().equals(SessionState.NOTCONNECTED)){
								if(inviteSession(session)){
									user.setsessionState(SessionState.INVITE_SENT);
									remoteSessionsMap.put(session.getSessionUuid(), user);
								}
							}
						}
					} catch (StcException e) {
						e.printStackTrace();
					}
					
				}
			}
		}
		
		
		synchronized (listeners) {
			for (ISimpleDiscoveryListener listen : listeners)
				listen.sessionsDiscovered();
		}
	}
	

	/***
	 * Gets the current session list. Returns an empty list if stclib is not
	 * initialized or there are no sessions or something else goes wrong.
	 * 
	 * @return
	 */
	public List<StcSession> getSessions() {
		StcLib lib = getSTCLib();
		if (lib != null) {
			try {
				return lib.getSessionListWithAvatar();
			} catch (StcException e) {
				Log.e(LOGC, "unexpected exception", e);
			}
		}
		return new ArrayList<StcSession>();
	}
	
		// /
		// / LISTENER MANAGEMENT AND EVENT PROPAGATION
		// /
	
		//Add listener to communicate between service and the UI.
		public boolean addListener(ISimpleDiscoveryListener listener) {
			synchronized (listeners) {
				if (listeners.contains(listener))
					return false;
				else
				{
					boolean temp = listeners.add(listener);
					return temp;
				}
			}
		}

		//Remove listener to communicate between service and the UI.
		public boolean removeListener(ISimpleDiscoveryListener listener) {
			synchronized (listeners) {
				return listeners.remove(listener);
			}
		}
		
		// /
		// / PUBLIC METHODS FOR SERVICE USERS TO REQUEST ACTIONS
		// /

		/***
		 * Request to invite a session.
		 * 
		 * @param session
		 * @return true if an invitation was sent.
		 */
		public boolean inviteSession(StcSession session) {
				
			if(connection_Counter >= MAX_CONNECTION){
				return false;
			}
			
			if(!session.isAvailableCloud() && !session.isAvailableProximity())
			{
				Log.e(LOGC, "Session is not available or expired");
				return false;
			}
			try {
				Log.i(LOGC, "inviting session " + session.getSessionUuid().toString()
						+ " " + session.getUserName());
				getSTCLib().inviteSession(session.getSessionUuid(), (short) 180);
				return true;
			} catch (StcException e) {
				Log.e(LOGC, "invitation unexpected exception", e);
			}
			return false;
		}
		
		//This will set the callback to read the message sent by remote user.
		public void chatReceived(String username, String line){
			Log.d(TAG,"chatReceived: username: " + username + " line: " + line);
			
			//PARSE
			parseCommand(username,line);
			synchronized (listeners) {
				for (ISimpleDiscoveryListener listen : listeners)
					listen.updatedChatList(username + " : " + line);
			}
		}
		
		/** parses a command received from a user */
		private void parseCommand(String username, String line) {
			int id = getRemoteSessionID(username);
			//Sends 0 allowed actions
			sendActionsAllowed(id,new HashSet<Action>());
			Log.d(TAG,"Username: " + username + " maps to id: " + id);
			
			String [] tokens = line.split(" ");
			if (tokens.length > 1) {
				String command = tokens[0];
				int ammount = Integer.parseInt(tokens[1]);
				if (command.equals("bet")) {
					actionList.set(id, new BetAction(ammount));
				} else if(command.equals("raise")) {
					actionList.set(id, new RaiseAction(ammount));
				} else {
					Log.d(TAG,"command size > 1, not equals raise nor bet :/");
				}
			} else if (tokens.length == 1) {
				String command = tokens[0];
				if (command.equals("fold")) {
					actionList.set(id, new FoldAction());
				} else if (command.equals("check")) {
					actionList.set(id, new CheckAction());
				} else if (command.equals("call")) {
					actionList.set(id, new CallAction());
				} else if (command.equals("allin")) {
					actionList.set(id, new AllInAction());
				} else {
					Log.d(TAG,"command size == 1, not fold check call nor allin:/");
				}
			} else {
				Log.d(TAG, "tokens.size() < 1 :(");
			}		
		}
		
		/** Returns the index of a given RemoteSession username in the LinkedList */
		private int getRemoteSessionID(String username) {
			boolean found = false;
			int i = 0;
			while ( !found && i < remoteUsersList.size()) {
				if (remoteUsersList.get(i).getSession().getUserName().equals(username))
					return i;	
				++i;
			}
			return -1;
		}
		
		//This will set the callback to terminate the connection
		public void remoteSessionDisconnected(RemoteUser user){
			
			synchronized (remoteSessionsMap) {
				if(remoteSessionsMap.containsKey(user.getSession().getSessionUuid())){
					user.setsessionState(SessionState.DISCONNECTED);
					remoteSessionsMap.put(user.getSession().getSessionUuid(), user);
				}
			}
			synchronized (listeners) {
				for(ISimpleDiscoveryListener listen :listeners){
					listen.invalidateSessionList();
				}
			}
			connection_Counter = remoteSessionsMap.size();
		}
		
		//To write chat messages to remote users.
		public void postToConnections(String value){
			synchronized (remoteSessionsMap) {
				Set<UUID> list = remoteSessionsMap.keySet();
				Iterator<UUID> it = list.iterator();
				while(it.hasNext()){
					UUID uid = it.next();
					RemoteUser user = remoteSessionsMap.get(uid);
					if(user.getsessionState().equals(SessionState.CONNECTED)){
						WriteEngine writer = user.getWriter();
						if(writer!=null){
							writer.writeString(value);
						}
					}
				}
			}
		}
		
		//
		///DiscoveryNode related methods
		//
		//To add a discoveryNode.
		public boolean addDiscoveryNode(final NodeWrapper node)
		{
			 //Since discoveryNodeList.contains(node) is returning false, using the below technique to check
			 //whether user exists in the list or not and if exists don't add to discoveryNodeList.
			 for(int index=0 ; index<discoveryNodeList.size();index++){
				 if(discoveryNodeList.get(index).getNodeName().equals(node.getNodeName())){
					 Log.i("", "Node status "+discoveryNodeList.get(index).getNodeStatus());
					 if(discoveryNodeList.get(index).getNodeStatus()){
						 return false;
					 }else{
						 discoveryNodeList.remove(index);
						 Log.i("", "Node removed "+discoveryNodeList.size());
						 break;
					 }
				 }
			 }
			
			discoveryNodeList.add(node);
			synchronized (listeners) {
				for(ISimpleDiscoveryListener listen : listeners){
					listen.updateDiscoveryNodeList(discoveryNodeList);
				}
			}
			
			return true;
		} 
		//To retrieve the instance of discoveryNodeList containing list of joined nodes.
		public ArrayList<NodeWrapper> getDiscoveryNodeList(){
			return discoveryNodeList;
		}
		
		public void removeDiscoveryNode(StcDiscoveryNode node){
			for(int index=0 ; index<discoveryNodeList.size();index++){
				if(discoveryNodeList.get(index).getNodeName().equals(node.getName())){
					discoveryNodeList.remove(index);
					synchronized (listeners) {
						for(ISimpleDiscoveryListener listen : listeners){
							listen.updateDiscoveryNodeList(discoveryNodeList);
						}
					}
					break;
				}
			}
		}
		@Override
		public void onDestroy() {
			connection_Counter = 0;
			exitAllConnections();
			super.onDestroy();
		}
		
		//This method will disconnect all the existing connection before terminating the app service.
		private void exitAllConnections() {
			synchronized (remoteSessionsMap) {
				Set<UUID> list = remoteSessionsMap.keySet();
				Iterator<UUID> it = list.iterator();
				while(it.hasNext()) {
					UUID uid = it.next();
					RemoteUser user = remoteSessionsMap.get(uid);
					if(user.getsessionState().equals(SessionState.CONNECTED)){
						user.remoteDisconnect();
					}
				}
			}
		}
		
		public ArrayList<RemoteUser> getRemoteUsers() {
			Log.d("CCFManager", "remoteUserList return: " + remoteUsersList.toString());
			Log.d("CCFManager", "Size: " + Integer.toString(remoteUsersList.size()));
			return remoteUsersList;
		}
		
		public void sendActionsAllowed(int id, Set<Action> allowedActions) {
			WriteEngine wEngine = getRemoteUsers().get(id).getWriter();
			wEngine.writeString("cmds " + encodeActions(allowedActions));
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private String encodeActions(Set<Action> s) {
			int out = 0;
			for (Action a : s) {
				out += a.getEncode();
			}
			return Integer.toString(out);
		}
}
