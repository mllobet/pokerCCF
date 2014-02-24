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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;

import com.intel.inproclib.R;
import com.intel.stc.utility.StcSession;

public class FriendReceiveInvitationActivity extends FriendInvitationBase {

	public static class ToastFrientInvitationDialogFragment extends
			DialogFragment {

		public static ToastFrientInvitationDialogFragment newInstance() {
			ToastFrientInvitationDialogFragment frag = new ToastFrientInvitationDialogFragment();
			Bundle args = new Bundle();
			frag.setArguments(args);
			return frag;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
					getActivity());
			Drawable d = new BitmapDrawable(getResources(),
					mSession.getAvatar());
			alertDialogBuilder.setIcon(d);
			Resources res = getResources();
			alertDialogBuilder.setTitle(res
					.getString(R.string.friendreceiveinvitation_title));
			String message = String
					.format(res
							.getString(R.string.friendreceiveinvitationmsg_invitationreceived),
							mSession.getUserName());
			alertDialogBuilder.setMessage(message);
			alertDialogBuilder
					.setPositiveButton(R.string.accept_button_text,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									((FriendReceiveInvitationActivity) getActivity())
											.doPositiveClick();
								}
							})
					.setNegativeButton(R.string.ignore_button_text,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									((FriendReceiveInvitationActivity) getActivity())
											.doNegativeClick();
								}
							}).create();
			return alertDialogBuilder.create();
		}
	}

	void showToast() {
		DialogFragment newFragment = ToastFrientInvitationDialogFragment
				.newInstance();
		newFragment.show(getSupportFragmentManager(), "dialog");
	}

	public void doPositiveClick() {
		// Do stuff here.
		Log.i("FragmentAlertDialog", "Positive click!");
		SendFriendInvitationRequest();
		// finish();
	}

	public void doNegativeClick() {
		// Do stuff here.
		Log.i("FragmentAlertDialog", "Negative click!");
		mFriendService
				.writeBuffer((int) FriendInvitationMessage.InvitationNotAccepted
						.getValue());
		finish();
	}

	static StcSession mSession;
	private boolean mInvited;
	FriendReceiveInvitationState mFriendReceiveInvitationState = null;

	void SendFriendInvitationRequest() {

		myHandler.post(new Runnable() {
			public void run() {
				if (mFriendService.addBuddy() >= 0) {
					mFriendReceiveInvitationState = FriendReceiveInvitationState.RegistrationSucceededFriendsMade;
					mFriendService
							.writeBuffer((int) FriendInvitationMessage.AddFriendSucceeded
									.getValue());
					ShowSuccess();
				} else {
					mFriendReceiveInvitationState = FriendReceiveInvitationState.ErrorAfterAcceptingInvitation;
					mFriendService
							.writeBuffer((int) FriendInvitationMessage.AnErrorOccurred
									.getValue());

					ShowError();
				}
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle bundle = this.getIntent().getExtras();
		String userName = null;
		if (bundle != null) {
			mInvited = bundle.getBoolean("invited");

		}
		setContentView(R.layout.friend_receive_invitation);

	}

	@Override
	protected void onDestroy() {
		doStopService();
		super.onDestroy();
	}

	void ShowSuccess() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		// set title
		Resources res = getResources();
		Drawable d = new BitmapDrawable(getResources(), mSession.getAvatar());
		alertDialogBuilder.setIcon(d);
		alertDialogBuilder.setTitle(res
				.getString(R.string.friendreceiveinvitation_title));
		String message = String
				.format(res
						.getString(R.string.friendreceiveinvitationmsg_registrationsucceededfriendsmade),
						mSession.getUserName());

		// set dialog message
		alertDialogBuilder
				.setMessage(message)
				.setCancelable(false)
				.setPositiveButton(res.getString(R.string.ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								FriendReceiveInvitationActivity.this.finish();

							}
						});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();

	}

	void ShowError() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		// set title
		Resources res = getResources();
		Drawable d = new BitmapDrawable(getResources(), mSession.getAvatar());
		alertDialogBuilder.setIcon(d);
		alertDialogBuilder.setTitle(res
				.getString(R.string.friendreceiveinvitation_title));
		String message = String
				.format(res
						.getString(R.string.friendreceiveinvitationmsg_errorafteracceptinginvitation),
						mSession.getUserName());

		// set dialog message
		alertDialogBuilder
				.setMessage(message)
				.setCancelable(false)
				.setPositiveButton(res.getString(R.string.ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								FriendReceiveInvitationActivity.this.finish();

							}
						});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();

	}

	@Override
	public void onBackPressed() {
		finish();
		doStopService();
		super.onBackPressed();
	}

	@Override
	public void dataReceived(int line) {
		myHandler.post(new Runnable() {
			public void run() {
				if (mInvited) {
				}
			}
		});
	}

	@Override
	public void remoteDisconnect() {
		myHandler.post(new Runnable() {
			public void run() {
				finish();
			}
		});
	}

	public void writeLine(View view) {
	}

	@Override
	public void onStcLibPrepared() {
		myHandler.post(new Runnable() {
			public void run() {
				mSession = mFriendService.getInitiatorSession();
				mFriendReceiveInvitationState = FriendReceiveInvitationState.InvitationReceived;
				showToast();
			}
		});
	}

	@Override
	public void sessionListChanged() {
	}

	@Override
	public void connected(boolean didConnect) {
	}

	@Override
	public void localSessionChanged() {
	}
}
