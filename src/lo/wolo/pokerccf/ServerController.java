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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import lo.wolo.pokerengine.*;
import lo.wolo.pokerengine.actions.*;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.intel.stc.events.StcException;
import com.intel.stc.lib.StcLib;
import com.intel.stc.lib.StcLib.NodeFlags;

public class ServerController extends AbstractServiceUsingActivity implements OnClickListener{

	private static final String TAG = "ServerController";

	private SessionAdapter	sessionAdapter = null;
	private ListView sessionList = null;
	private TextView sessionEmptyText = null;
	private ListView chatList = null;
	ChatAdapter chatAdapter = null;
	private Button discoveryNodesButton = null;
	private Button startButton = null;
	private Dialog platformStartAlert;

	private ServerController myself;

	/*Start**************Activity Lifecycle calls**********/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		myself = this;

		setContentView(R.layout.activity_ccfuicontroller);

		sessionList = (ListView)findViewById(R.id.sessionList);
		sessionEmptyText = (TextView)findViewById(R.id.sessionListEmptyText);
		sessionList.setEmptyView(sessionEmptyText);

		chatList = (ListView)findViewById(R.id.chatList);
		chatAdapter = new ChatAdapter(this);
		chatList.setAdapter(chatAdapter);

		// Buttons!
		discoveryNodesButton = (Button)findViewById(R.id.discoveryButton);
		discoveryNodesButton.setOnClickListener(this);
		startButton = (Button)findViewById(R.id.startButton);
		startButton.setOnClickListener(this);

		doStartService();

		platformStartAlert = ProgressDialog.show(this, "", "Please wait, initializing StcLib platform.");
		platformStartAlert.show();
	}

	@Override
	public void onBackPressed()
	{
		Log.i(LOGC, "back pressed");

		StcLib lib = serviceManager.getSTCLib();

		if(lib!=null && serviceManager.getDiscoveryNodeList().size()!=0){
			for(int i=0; i<serviceManager.getDiscoveryNodeList().size();i++){
				try {
					int status = lib.leaveDiscoveryNode(MultiConnectRegisterApp.id.appId, serviceManager.getDiscoveryNodeList().get(i).getNodeName(), EnumSet.of(NodeFlags.PUBLISH));
					Log.i("", "removing node: "+serviceManager.getDiscoveryNodeList().get(i).getNodeName()+" status:"+status);
				} catch (StcException e) {
					e.printStackTrace();
				}
			}
		}

		finish();
		doStopService();
		super.onBackPressed();
	}

	//This is a callback on button click.
	@Override
	public void onClick(View view) {
		switch(view.getId()){

		case R.id.discoveryButton :
			Intent intent = new Intent(ServerController.this,DiscoveryNodeActivity.class);
			startActivity(intent);
			break;
		case R.id.startButton :
			//HERE IS WHERE THE HACK HAPPENS
			Thread t = new Thread(new Runnable() {

				/** The size of the big blind. */
				private  final int BIG_BLIND = 10;

				/** The starting cash per player. */
				private  final int STARTING_CASH = 500;

				/** Table type (betting structure). */
				private final TableType TABLE_TYPE = TableType.NO_LIMIT;

				/** The table. */
				private Table table;

				/** The players */
				private ArrayList<Player> players;

				public void run() {


					/** Initialize all the players **/
					ArrayList<RemoteUser> remoteUsersList = serviceManager.getRemoteUsers();
					int remoteUsersSize = remoteUsersList.size();
					Log.d("ServerController","remoteUsers hash: " + remoteUsersList.toString());
					Log.d("ServerController","remoteUsers.size(): " + Integer.toString(remoteUsersSize));
					players = new ArrayList<Player>();
					for (int i = 0; i < remoteUsersSize; ++i)
						players.add(null);

					Log.d("ServerController","players size: " + Integer.toString(players.size()));
					for (int i = 0; i < players.size(); ++i)
						players.set(i, new Player(Integer.toString(i), STARTING_CASH, new ClientCCF()));

					/** Initialize the table **/
					table = new Table(TABLE_TYPE, BIG_BLIND, serviceManager, myself);
					for (Player player : players)
						table.addPlayer(player);

					table.run();	    	
				}
			});
			t.start();
			break;
		}
	}

	//Display dialog, if no sessions are connected and user tries to send message
	private void displayNoConnectiondialog(){
		final AlertDialog.Builder builder = new AlertDialog.Builder(ServerController.this);
		builder.setTitle("Warning");
		builder.setMessage("No sessions are connected.");
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
	/*End**************Activity Lifecycle calls**********/

	/*Start**************STC callback.**********/
	@Override
	protected void onStcLibPrepared() {

		if(sessionAdapter == null){
			sessionAdapter = new SessionAdapter(serviceManager, this);

			final StcLib lib = serviceManager.getSTCLib();
			myHandler.post(new Runnable() {
				public void run()
				{
					try {
						if(lib!=null && !lib.isUnboxed()){
							lib.setUserName(android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL);
							lib.setSessionName("Android "+ (isTablet(ServerController.this)?"Tablet" : "Phone"));
							lib.setAvatar(BitmapFactory.decodeResource(getResources(), R.drawable.generic_avatar50x50));
						}
					} catch (StcException e) {
					}

					if(platformStartAlert!=null && platformStartAlert.isShowing()){
						platformStartAlert.dismiss();
					}

					sessionList.setAdapter(sessionAdapter);
				}
			});
		}

		displayToast("Platform Prepared");
	}

	//This method will update the session list.
	@Override
	public void sessionsDiscovered() {
		myHandler.post(new Runnable() {
			public void run()
			{
				if (sessionAdapter != null)
				{
					Log.i(LOGC, "updating list");
					sessionAdapter.setNewSessionList(serviceManager.getSessions());
					sessionAdapter.notifyDataSetChanged();

				}
			}
		});
	}
	/*End**************STC callback.**********/


	//
	/// ISimpleDiscoveryListner overridden methods.
	//

	//This method is used to update the chatlist, once chat message is received from remote session.
	@Override
	public void updatedChatList(final String line) {
		myHandler.post(new Runnable() {

			@Override
			public void run() {
				chatAdapter.addChatLine(line);
				chatAdapter.notifyDataSetChanged();
			}
		});

	}

	@Override
	public void updateDiscoveryNodeList(ArrayList<NodeWrapper> discoveryNodeList) {
		//This is empty method, instead use updateDiscoveryNodeList() of DiscoveryNodeActivity.
	}

	//This method is use to invalidate the connection state of session list.
	@Override
	public void invalidateSessionList() {
		myHandler.post(new Runnable() {

			@Override
			public void run() {
				if (sessionAdapter != null)
				{
					Log.i(LOGC, "invalidateSessionList");
					sessionAdapter.notifyDataSetInvalidated();

				}
			}
		});

	}

	//Display dialog to input userName and Device Name from local user to complete unboxing process.
	private void completeUnboxing(final StcLib lib){
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Unboxing");
		alert.setCancelable(false);

		LayoutInflater inflater = (LayoutInflater)this.getApplicationContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View parent = inflater.inflate(R.layout.unbox_layout, null);
		final EditText userName = (EditText)parent.findViewById(R.id.userName);
		userName.setText(android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+"-MultiConnect");
		final EditText deviceName = (EditText)parent.findViewById(R.id.deviceName);
		deviceName.setText(android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL);
		alert.setView(parent);

		alert.setPositiveButton("Next", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String user = userName.getText().toString();
				String device = deviceName.getText().toString();
				try {
					lib.setUserName(user!=null && !user.equals("") ? user : android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+"-MultiConnect");
					lib.setSessionName(device!=null && !device.equals("") ? device : "Android");
					lib.setAvatar(BitmapFactory.decodeResource(getResources(), R.drawable.generic_avatar50x50));
				} catch (StcException e) {
					Log.e(LOGC, "Error during unboxing process"+e.getLocalizedMessage());
				}
			}
		});

		alert.show();
	}

	//Validating device is a Tablet or Phone.
	private boolean isTablet(Context context) {  
		return (context.getResources().getConfiguration().screenLayout   
				& Configuration.SCREENLAYOUT_SIZE_MASK)    
				>= Configuration.SCREENLAYOUT_SIZE_LARGE; 
	}

	public void sendActionsAllowed(int id, Set<Action> allowedActions) {
		Log.d(TAG,"sendingActionsAllowed");
		WriteEngine wEngine = serviceManager.getRemoteUsers().get(id).getWriter();
		wEngine.writeString(encodeActions(allowedActions));
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

	public void sendCards(int id, Card[] cards) {
		String out = "";
		boolean first = true;
		for (Card c : cards) {
			out += c.hashCode();
			if (first) {
				out += " ";
				first = false;
			}
		}
		Log.d(TAG,"sendingCards: " + out);
		WriteEngine wEngine = serviceManager.getRemoteUsers().get(id).getWriter();
		wEngine.writeString(out);
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
