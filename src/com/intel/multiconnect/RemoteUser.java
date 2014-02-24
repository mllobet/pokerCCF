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

import com.intel.stc.events.StcException;
import com.intel.stc.utility.StcSession;
import com.intel.stc.utility.StcSocket;

/**
 * Wrapper class to bind the Connection established Session users.
 * This class contains the information about socket connection, ReadEngine and WriteEngine.
 */
public class RemoteUser implements IServiceIOListener{

	private StcSession remoteSession = null;
	private StcSocket socket = null;
	private ReadEngine reader = null;
	private WriteEngine writer = null;
	private CCFManager manager = null;
	private CCFManager.SessionState sessionState = CCFManager.SessionState.NOTCONNECTED;
	
	//Constructor to initialize the reference of RemoteUser.
	public RemoteUser(StcSession session, CCFManager manager) {
		remoteSession = session;
		this.manager = manager;
	}

	//To retrieve the StcSession instance.
	public StcSession getSession(){
		return remoteSession;
	}
	
	//To retrieve StcSocket instance.
	public StcSocket getDataStream() {
		return socket;
	}

	//For setting socket connection.
	public void setDataStream(StcSocket socket) {
		this.socket = socket;
		reader = new ReadEngine(socket.getInputStream(), this);
		writer = new WriteEngine(socket.getOutputStream(), this);
	}

	//To receive the incoming message from remote Session.
	@Override
	public void lineReceived(String line) {
		manager.chatReceived(remoteSession.getUserName()+" : "+line);
	}

	//To disconnect the connecton.
	@Override
	public void remoteDisconnect() {
		exitConnection();
		manager.remoteSessionDisconnected(this);
	}
	
	//To retrieve ReadEngine instance.
	public ReadEngine getReader(){
		return reader;
	}
	
	//To retrieve WriteEngine instance.
	public WriteEngine getWriter(){
		return writer;
	}
	
	//Exit process while terminating the connection.
	private void exitConnection() {

		if (reader != null)
			reader.stop();
		if (reader != null)
			reader.stop();
		if (socket != null) {
			try {
				socket.close();
			} catch (StcException e) {}
			socket = null;
		}
	}
	
	//To update Session state.
	public void setsessionState(CCFManager.SessionState state){
		this.sessionState = state;
	}
	
	//To retrieve Session state.
	public CCFManager.SessionState getsessionState(){
		return sessionState;
	}
	
}
