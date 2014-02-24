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
package com.intel.friend.invitation;

import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;

import com.intel.stc.events.StcException;
import com.intel.stc.utility.StcSession;

public class FriendSendInvitationActivity extends FriendInvitationBase
		implements IFriendInvitationEventListener {
	static final String LOGC = WriteEngine.class.getCanonicalName();
	Bundle mBundle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBundle = this.getIntent().getExtras();
		mBundle = this.getIntent().getExtras();
		doStartService();

	}

	@Override
	protected void onDestroy() {
		doStopService();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		finish();
		doStopService();
		super.onBackPressed();
	}

	FriendSendInvitationState mFriendSendInvitationState = FriendSendInvitationState.ConnectedMakingFriendRequest;
	StcSession mSession;
	SendInvitationDialogFragment mSendDialogMessage = null;

	void setUIForCurrentState() {
		if (mSendDialogMessage == null) {
			mSendDialogMessage = new SendInvitationDialogFragment();
		} else {
			//mSendDialogMessage.dismiss();
			mSendDialogMessage = null;
			mSendDialogMessage = new SendInvitationDialogFragment();
		}
		mSendDialogMessage.setSession(mSession);
		mSendDialogMessage.setContext(this);
		mSendDialogMessage
				.setFriendSendInvitationState(mFriendSendInvitationState);
		mSendDialogMessage.show(getSupportFragmentManager(), "dialog");

	}

	public void inviteSession(StcSession session) {
		if (mFriendService.inviteSession(session)) {
			mSession = session;
			mFriendSendInvitationState = FriendSendInvitationState.ConnectedMakingFriendRequest;
			setUIForCurrentState();
		}
	}

	// /
	// / ISimpleChatEventListener methods
	// /

	public void sessionListChanged() {

	}

	@Override
	public void connected(final boolean didConnect) {
		myHandler.post(new Runnable() {
			public void run() {
				if (!didConnect) {
					doStopService();
					finish();
				}
			}

		});
	}

	Handler mHandler = new Handler();

	public void dataReceived(int value) {

		final int statsCode = value;
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				mHandler.post(new Runnable() { // This thread runs in the UI
					@Override
					public void run() {
						FriendInvitationMessage msg = FriendInvitationMessage
								.values()[statsCode];
						switch (msg) {
						case InvitationAccepted:
							mFriendSendInvitationState = FriendSendInvitationState.InvitationAccepted;
							setUIForCurrentState();
							break;
						case AnErrorOccurred:
							mFriendSendInvitationState = FriendSendInvitationState.ServerRegistrationFailed;
							setUIForCurrentState();
							break;
						case InvitationNotAccepted:
							mFriendSendInvitationState = FriendSendInvitationState.FriendRequestNotAccepted;
							setUIForCurrentState();
							break;
						case InvitationAcceptedButInternetNotAvailable:
							// interop application ignores it
							break;
						case AddFriendSucceeded:
							if (mFriendService.addBuddy() >= 0) {
								mFriendSendInvitationState = FriendSendInvitationState.ServerRegistrationSucceededFriendsMade;
								mFriendService
								.writeBuffer((int) FriendInvitationMessage.AddFriendSucceeded
										.getValue());
						} else {
								mFriendSendInvitationState = FriendSendInvitationState.ServerRegistrationFailed;
								mFriendService
										.writeBuffer((int) FriendInvitationMessage.AnErrorOccurred
												.getValue());
							}
							setUIForCurrentState();
							break;
						default:
							mFriendSendInvitationState = FriendSendInvitationState.FriendRequestTimedOut;
							setUIForCurrentState();
							break;
						}

					}
				});
			}
		};
		new Thread(runnable).start();

	}

	public void remoteDisconnect() {
		mFriendSendInvitationState = FriendSendInvitationState.ConnectionFailed;
		setUIForCurrentState();
	}


	@Override
	public void onStcLibPrepared() {
		mFriendService.parseInitBundle(mBundle);

		myHandler.post(new Runnable() {
			public void run() {

				StcSession session;
				try {
					if (mBundle != null) {
						String sessionStr = mBundle.getString("session");
						if (sessionStr != null) {
							UUID sessionUuid = UUID.fromString(sessionStr);
							session = mFriendService.getSTCLib().querySession(
									sessionUuid);

							inviteSession(session);
						}
					}
				} catch (StcException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void localSessionChanged() {

	}
}
