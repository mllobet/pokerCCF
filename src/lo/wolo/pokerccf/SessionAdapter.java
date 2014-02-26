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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import lo.wolo.pokerccf.CCFManager.SessionState;

/***
 * Manages the data from the service to show the session list.
 * <p>
 * This code shows use of the StcSession. How to get the name. How to get the avatar.
 * How to get the list of applications that the session has. c3 specific content is 
 * concentrated in the two methods getView and setNewSessionList.
 */
public class SessionAdapter extends BaseAdapter {

	public List<StcSession> sessionList = new ArrayList<StcSession>();
	CCFManager service;
	Context selectActivity;

	/***
	 * Creates the view for one item in the list.
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)service.getApplicationContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.session_row, null);
		} else
			convertView.setVisibility(View.VISIBLE);

		StcSession curSession = null;
		synchronized (sessionList) {
			if (position >= 0 && position < sessionList.size()) 
				curSession = (StcSession)getItem(position);
		}
		
		if( curSession == null ) {
			convertView.setVisibility(View.GONE);
			return convertView;
		}

		/*// get the avatar from the session and put it into the image view
		ImageView avatar = (ImageView)convertView.findViewById(R.id.row_userAvatar);
		if (curSession.getPublicAvatar() == null)
			avatar.setImageResource(R.drawable.generic_avatar);
		else
			avatar.setImageBitmap(curSession.getPublicAvatar());*/

		// get the name from the session and put it into the text view
		TextView userName = (TextView)convertView.findViewById(R.id.row_userName);
		userName.setText(curSession.getUserName());
		
		// Attach the cloud image,if user is discovered over the cloud.
		if(curSession.isAvailableCloud()||!curSession.isAvailableProximity()) {
			ImageView temp = (ImageView)convertView.findViewById(R.id.row_userCloud);
			temp.setImageResource(R.drawable.cloud);
		}

		TextView userStatus = (TextView)convertView.findViewById(R.id.row_userStatus);
		synchronized (service.remoteSessionsMap) {
			if(service.remoteSessionsMap.containsKey(curSession.getSessionUuid())){
				RemoteUser user = service.remoteSessionsMap.get(curSession.getSessionUuid());
				userStatus.setText(user.getsessionState().toString());
			}else{
				userStatus.setText(SessionState.NOTCONNECTED.toString());
			}
		}
		
		final StcSession curSess = curSession;
		convertView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				synchronized (service.remoteSessionsMap){
					if(service.remoteSessionsMap.containsKey(curSess.getSessionUuid())){
						RemoteUser user = service.remoteSessionsMap.get(curSess.getSessionUuid());
						if(user.getsessionState().equals(SessionState.CONNECTED)){
							displayDisconnectDialog(user);
						}else{
							if(CCFManager.connection_Counter >= CCFManager.MAX_CONNECTION){
								displayConnectionFull(curSess);
							}else{
								displayConnectDialog(user);
							}
						}
					}
				}
			}
		});
		return convertView;
	}
	
	/***
	 * Receive the new list and filter it for sessions that are available and sessions
	 * that have this application.
	 * @param newList
	 */
	public void setNewSessionList(List<StcSession> newList)
	{
		synchronized(sessionList) 
		{
			if(newList.size() == 0 )
				sessionList = newList;
			else
			{
				for( int i = newList.size() - 1; i >= 0 ; i-- )
				{
					StcSession session = newList.get(i);
					if( !session.isOnline() )
					{
						// remove if the session is listed as unavailable or busy.
						newList.remove(i);
					} 
					else 
					{
						UUID sessionApps[] = session.getAppList();
						boolean foundApp = false;
						for(UUID sessionApp : sessionApps)
						{
							if(sessionApp!=null && sessionApp.equals(MultiConnectRegisterApp.id.appId))
							{
								foundApp = true;
								break;
							}
						}
						if( !foundApp )
							newList.remove(i);
					}
				}
				sessionList = newList;
				Collections.sort(sessionList);
			}
		}
	}

	public SessionAdapter(CCFManager service, Context selectActivity) {
		this.service = service;
		this.selectActivity = selectActivity;
		setNewSessionList(service.getSessions());
	}

	@Override
	public int getCount() 
	{
		synchronized(sessionList) {
			return sessionList.size();
		}
	}

	@Override
	public Object getItem(int position) 
	{
		synchronized(sessionList) {
			if (sessionList != null && position < sessionList.size() && position >= 0)
				return sessionList.get(position);
			else
				return null;
		}
	}

	@Override
	public long getItemId(int position) 
	{
		return position;
	}

	//Display confirmation dialog to disconnect the user.
	private void displayDisconnectDialog(final RemoteUser user){
		final AlertDialog.Builder builder = new AlertDialog.Builder(selectActivity);
		builder.setTitle("Do you wish to disconnect "+user.getSession().getUserName()+" ?");
		builder.setCancelable(false);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				user.remoteDisconnect();
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				if(builder!=null)
					dialog.dismiss();
			}
		});
		builder.show();
	}
	
	//Display confirmation dialog to connect to remote user.
	private void displayConnectDialog(final RemoteUser user){
		final AlertDialog.Builder builder = new AlertDialog.Builder(selectActivity);
		builder.setTitle("Do you wish to connect to "+user.getSession().getUserName()+" ?");
		builder.setCancelable(false);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				synchronized (service.remoteSessionsMap) {
					if(service.inviteSession(user.getSession())){
						user.setsessionState(SessionState.INVITE_SENT);
						service.remoteSessionsMap.put(user.getSession().getSessionUuid(), user);
					}
				}
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				if(builder!=null)
					dialog.dismiss();
			}
		});
		builder.show();
	}
	
	//Display connection full dialog.
	private void displayConnectionFull(StcSession session){
		final AlertDialog.Builder builder = new AlertDialog.Builder(selectActivity);
		builder.setTitle("Can't connect to "+session.getUserName());
		builder.setMessage("Maximum connection reached.");
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
}
