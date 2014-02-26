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
import android.os.PowerManager;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

	private Button changeLayoutButton = null;

	public ImageView card1 = null;
	public ImageView card2 = null;
	public ImageView card3 = null;
	public ImageView card4 = null;
	public ImageView card5 = null;

	public TextView player1 = null;
	public TextView player2 = null;
	public TextView player3 = null;
	public TextView player4 = null;
	public TextView player5 = null;
	public TextView player6 = null;

	public TextView potView = null;
	public TextView curBetView = null;


	public int card1Hash = -1;
	public int card2Hash = -1;
	public int card3Hash = -1;
	public int card4Hash = -1;
	public int card5Hash = -1;

	protected PowerManager.WakeLock mWakeLock;
	
	public String playerName1 = "";
	public String playerName2 = "";
	public String playerName3 = "";
	public String playerName4 = "";
	public String playerName5 = "";
	public String playerName6 = "";
	
	static final int cardDrawables[] = {
		R.drawable.card_00, R.drawable.card_01, R.drawable.card_02, R.drawable.card_03, R.drawable.card_04,
		R.drawable.card_05, R.drawable.card_06, R.drawable.card_07, R.drawable.card_08, R.drawable.card_09,
		R.drawable.card_10, R.drawable.card_11, R.drawable.card_12, R.drawable.card_13, R.drawable.card_14,
		R.drawable.card_15, R.drawable.card_16, R.drawable.card_17, R.drawable.card_18, R.drawable.card_19,
		R.drawable.card_20, R.drawable.card_21, R.drawable.card_22, R.drawable.card_23, R.drawable.card_24,
		R.drawable.card_25, R.drawable.card_26, R.drawable.card_27, R.drawable.card_28, R.drawable.card_29,
		R.drawable.card_30, R.drawable.card_31, R.drawable.card_32, R.drawable.card_33, R.drawable.card_34,
		R.drawable.card_35, R.drawable.card_36, R.drawable.card_37, R.drawable.card_38, R.drawable.card_39,
		R.drawable.card_40, R.drawable.card_41, R.drawable.card_42, R.drawable.card_43, R.drawable.card_44,
		R.drawable.card_45, R.drawable.card_46, R.drawable.card_47, R.drawable.card_48, R.drawable.card_49,
		R.drawable.card_50, R.drawable.card_51
	};

	private ServerController myself;

	/*Start**************Activity Lifecycle calls**********/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		myself = this;

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.table_layout);
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Poker Table");
		this.mWakeLock.acquire();


		setContentView(R.layout.table_layout);

		card1 = (ImageView)findViewById(R.id.card1);
		card2 = (ImageView)findViewById(R.id.card2);
		card3 = (ImageView)findViewById(R.id.card3);
		card4 = (ImageView)findViewById(R.id.card4);
		card5 = (ImageView)findViewById(R.id.card5);

		player1 = (TextView)findViewById(R.id.player1);
		player2 = (TextView)findViewById(R.id.player2);
		player3 = (TextView)findViewById(R.id.player3);
		player4 = (TextView)findViewById(R.id.player4);
		player5 = (TextView)findViewById(R.id.player5);
		player6 = (TextView)findViewById(R.id.player6);

		potView = (TextView)findViewById(R.id.potView);
		curBetView = (TextView)findViewById(R.id.betView);

		startButton = (Button)findViewById(R.id.startButton);
		startButton.setOnClickListener(this);
		
		discoveryNodesButton = (Button)findViewById(R.id.discoveryButton);
		discoveryNodesButton.setOnClickListener(this);

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


	private void setTableLayout() {
		setContentView(R.layout.table_layout);

		card1 = (ImageView)findViewById(R.id.card1);
		card2 = (ImageView)findViewById(R.id.card2);
		card3 = (ImageView)findViewById(R.id.card3);
		card4 = (ImageView)findViewById(R.id.card4);
		card5 = (ImageView)findViewById(R.id.card5);

		player1 = (TextView)findViewById(R.id.player1);
		player2 = (TextView)findViewById(R.id.player2);
		player3 = (TextView)findViewById(R.id.player3);
		player4 = (TextView)findViewById(R.id.player4);
		player5 = (TextView)findViewById(R.id.player5);
		player6 = (TextView)findViewById(R.id.player6);

		potView = (TextView)findViewById(R.id.potView);
		curBetView = (TextView)findViewById(R.id.betView);

		startButton = (Button)findViewById(R.id.startButton);
		startButton.setOnClickListener(this);

	}


	public void setPlayer(String playerName, int playerNum) {
		if (playerNum == 1) playerName1 = playerName;
		else if (playerNum == 2) playerName2 = playerName;
		else if (playerNum == 3) playerName3 = playerName;
		else if (playerNum == 4) playerName4 = playerName;
		else if (playerNum == 5) playerName5 = playerName;
		else if (playerNum == 6) playerName6 = playerName;
		myHandler.post(new Runnable() {
			public void run () {
				Log.i("lolbug", "Setting names");
				
				player1.setText(playerName1);
				player2.setText(playerName2);
				player3.setText(playerName3);
				player4.setText(playerName4);
				player5.setText(playerName5);
				player6.setText(playerName6);
				Log.i("lolbug", "Finished setting names");
			}
		});
	}
	
	public void setCard(int cardHash, int cardNum) {
		if (cardNum == 1) card1Hash = cardHash;
		else if (cardNum == 2) card2Hash = cardHash;
		else if (cardNum == 3) card3Hash = cardHash;
		else if (cardNum == 4) card4Hash = cardHash;
		else if (cardNum == 5) card5Hash = cardHash;
		Log.i("lolbug", "CardNum: " + cardNum + "  cardHash: " + cardHash);
		myHandler.post(new Runnable() {
			public void run () {
				Log.i("lolbug", "Setting cards images...");
				if (card1Hash < 0) card1.setImageResource(R.drawable.card_back);
				else card1.setImageResource(cardDrawables[card1Hash]);
				if (card2Hash < 0) card2.setImageResource(R.drawable.card_back);
				else card2.setImageResource(cardDrawables[card2Hash]);
				if (card3Hash < 0) card3.setImageResource(R.drawable.card_back);
				else card3.setImageResource(cardDrawables[card3Hash]);
				if (card4Hash < 0) card4.setImageResource(R.drawable.card_back);
				else card4.setImageResource(cardDrawables[card4Hash]);
				if (card5Hash < 0) card5.setImageResource(R.drawable.card_back);
				else card5.setImageResource(cardDrawables[card5Hash]);
			}
		});

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
			Log.i("lolbug", "Start Button pressed");
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
					serviceManager.actionList = new ArrayList<Action>();
					//THIS WAS A NASTY BUG
					for (int i = 0; i < remoteUsersSize; ++i) {
						players.add(null);
						serviceManager.actionList.add(null);
					}

					Log.d("ServerController","players size: " + Integer.toString(players.size()));
					for (int i = 0; i < players.size(); ++i)
						players.set(i, new Player(Integer.toString(i), STARTING_CASH, new ClientCCF()));

					/** Initialize the table **/
					table = new Table(TABLE_TYPE, BIG_BLIND, serviceManager, myself);
					for (Player player : players)
						table.addPlayer(player);
					myself.serviceManager.postToConnections("minbet " + BIG_BLIND/2 + ";");
					table.run();	    	
				}
			});
			t.start();
			break;
		case R.id.changeLayoutButton :
			setTableLayout();
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

					//sessionList.setAdapter(sessionAdapter);
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
				//chatAdapter.addChatLine(line);
				//chatAdapter.notifyDataSetChanged();
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

	/** Sends all actions allowed to a given player id */
	public void sendActionsAllowed(int id, Set<Action> allowedActions) {
		Log.d(TAG,"sendingActionsAllowed");
		sendMessage(id, "cmds " + encodeActions(allowedActions));
	}

	/** it encodes a set of actions to be sent */
	private String encodeActions(Set<Action> s) {
		int out = 0;
		for (Action a : s) {
			out += a.getEncode();
		}
		return Integer.toString(out);
	}

	/** sends cards to a given player id */
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
		sendMessage(id, "cards " + out);
	}

	/** Send a message to a given player id */
	public void sendMessage(int id, String message) {
		Log.d("sendMessage","id: " + id + " " + message);
		WriteEngine wEngine = serviceManager.getRemoteUsers().get(id).getWriter();
		wEngine.writeString(message+";");
	}

	private int betHack;
	//This method is used to update the chatlist, once chat message is received from remote session.
	public void setBetText(int bet) {
		betHack = bet;
		myHandler.post(new Runnable() {

			@Override
			public void run() {
				curBetView.setText("Min Bet: $" + betHack);
			}
		});

	}

	private int potHack;
	//This method is used to update the chatlist, once chat message is received from remote session.
	public void setPotText(int pot) {
		potHack = pot;
		myHandler.post(new Runnable() {

			@Override
			public void run() {
				potView.setText("Big Pot: $" + Integer.toString(potHack));
			}
		});

	}
	
	@Override
    public void onDestroy() {
        this.mWakeLock.release();
        super.onDestroy();
    }
}
