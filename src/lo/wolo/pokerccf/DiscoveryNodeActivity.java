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

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class DiscoveryNodeActivity extends AbstractServiceUsingActivity implements OnClickListener{

	private EditText enterNodeName = null;
	private Button	createJoinNodeButton = null;
	private Button registerButton = null;
	private ListView nodeList = null;
	private static boolean cloudRegistrationFailed = false;
	private static final int START_CLOUD = 129031;
	private NodeListAdapter nodeAdapter = null;
	private TextView sessionRegistrationStatus = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_discovery_node);
		
		enterNodeName = (EditText)findViewById(R.id.enterNodeName);
		enterNodeName.addTextChangedListener(mTextEditorWatcher);
		
		createJoinNodeButton = (Button)findViewById(R.id.createJoinButton);
		createJoinNodeButton.setOnClickListener(this);
		
		registerButton = (Button)findViewById(R.id.registerButton);
		registerButton.setOnClickListener(this);
		
		nodeList = (ListView)findViewById(R.id.nodelist);
		sessionRegistrationStatus = (TextView)findViewById(R.id.userRegistrationStatus);
		
		}
	
		@Override
		protected void onStcLibPrepared() {
			Log.i("", "DiscoveryNodeActivity onStcLibPrepared");
			final StcLib lib = serviceManager.getSTCLib();
			lib.addStcDiscoveryNodeUpdateEventListener(DiscoveryNodeActivity.this);
			if(lib!=null){
				nodeAdapter = new NodeListAdapter(DiscoveryNodeActivity.this, serviceManager);
				myHandler.post(new Runnable() {
					
					@Override
					public void run() {
						try {
							nodeList.setAdapter(nodeAdapter);
							//This is a temporary error handling with reference to CQ-MWG100185178.
							//Once CQ-MWG100185178 is fixed remove flag cloudRegistrationFailed.
							StcSession localSession = lib.queryLocalSession();
							if(localSession.isRegisteredWithCloud() || cloudRegistrationFailed){
								registerButton.setEnabled(false);
							}else{
								registerButton.setEnabled(true);
							}
							
							if(localSession.isRegisteredWithCloud()){
								sessionRegistrationStatus.setText(localSession.getUserName()+" is registered.");
							}else{
								sessionRegistrationStatus.setText(localSession.getUserName()+" is not registered.");
							}
							
						} catch (StcException e) {
							e.printStackTrace();
						}
					}
				});
			}
		}

		@Override
		public void onBackPressed() {
			StcLib lib = serviceManager.getSTCLib();
			if(lib!=null){
				lib.removeStcDiscoveryNodeUpdateEventListener(this);
			}
			cloudRegistrationFailed = false;
			super.onBackPressed();
		}
		
		@Override
		public void onClick(View view) {
			switch(view.getId()){
			case R.id.registerButton:
				Intent intent = new Intent(getApplicationContext(), CloudAuthorizationActivity.class);
				intent.putExtra("clientID", MultiConnectRegisterApp.id.clientId);
				intent.putExtra("redirectURL", MultiConnectRegisterApp.redirectURL);
				intent.putExtra("appId", MultiConnectRegisterApp.id.appId.toString());
				
				startActivityForResult(intent, START_CLOUD);
				break;
			case R.id.createJoinButton:
				joinNode();
				break;
			}
		}

		//This method will be called on clicking join button.
		private void joinNode(){
			String nodeName = enterNodeName.getText().toString().trim();
			int status = joinDN(nodeName);
			if(status==0){
				NodeWrapper node = new NodeWrapper(new StcDiscoveryNode("","",nodeName), false);
				serviceManager.addDiscoveryNode(node);
			}
			enterNodeName.setText(null);
		}
		
		//Textwatcher to enable create and join button.
		private final TextWatcher  mTextEditorWatcher = new TextWatcher() 
		{
	        
	        @Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
	        {

	        }

	        @Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
	        {
	           
	        }

	        @Override
			public void afterTextChanged(Editable s)
	        {
	        	if(s.toString().matches(""))
	        	{
	        		createJoinNodeButton.setEnabled(false);
	        	}
	        	else
	        	{
	        		createJoinNodeButton.setEnabled(true);
	        	}
	        }
		};
		
		@Override
		protected void onActivityResult(final int requestCode, final int resultCode, Intent data) {
			super.onActivityResult(requestCode, resultCode, data);
			if (requestCode == START_CLOUD)
			{
				myHandler.post(new Runnable() {
					
					@Override
					public void run() {
						if (resultCode == StcConstants.STC_RESULT_OK){
							displayToast("Registration Successful.");
						}else{
							displayToast("Unable to complete cloud registration, try later on next launch.");
						}
					}
				});
				//This is a temporary error handling with reference to CQ-MWG100185178.
				//Once CQ-MWG100185178 is fixed remove the below line.
				cloudRegistrationFailed = true;
			}
		}

		//
		/// ISimpleDiscoveryListner overridden methods.
		//
		@Override
		public void updateDiscoveryNodeList(final ArrayList<NodeWrapper> discoveryNodeList) {
			myHandler.post(new Runnable() {
				
				@Override
				public void run() {

					nodeAdapter.setDiscoveryNodeList(discoveryNodeList);
					nodeAdapter.notifyDataSetChanged();
				}
			});
		}

		@Override
		public void invalidateSessionList() {
			
		}
		
		@Override
		public void sessionsDiscovered() {
			
		}

		@Override
		public void updatedChatList(String line) {
			
		}
}
