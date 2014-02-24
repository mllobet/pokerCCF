package com.intel.friend.invitation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.intel.inproclib.R;
import com.intel.stc.utility.StcSession;

public class SendInvitationDialogFragment extends DialogFragment {
	@Override
	public void onDestroyView() {
		getActivity().finish();
		super.onDestroyView();
	}

	private Context mContext;
	private StcSession mSession;
	private FriendSendInvitationState mFriendSendInvitationState;

	public SendInvitationDialogFragment() {

	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				getContext());
		Drawable d = new BitmapDrawable(getResources(), mSession.getAvatar());
		alertDialogBuilder.setIcon(d);
		Resources res = getResources();
		alertDialogBuilder.setTitle(res
				.getString(R.string.friendsendinvitation_title));
		String message;
		switch (getFriendSendInvitationState()) {
		case ConnectedMakingFriendRequest:
			message = String
					.format(res
							.getString(R.string.friendsendinvitationmsg_makingfriendrequest),
							mSession.getUserName());

			alertDialogBuilder.setMessage(message);
			// set dialog message
			alertDialogBuilder.setNeutralButton(
					res.getString(R.string.cancel_button_label),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							getActivity().finish();
						}
					});

			break;
		case ServerRegistrationSucceededFriendsMade:
			message = String
					.format(res
							.getString(R.string.friendsendinvitationmsg_serverregistrationsucceededfriendsmade),
							mSession.getUserName());

			alertDialogBuilder.setMessage(message);
			// set dialog message
			alertDialogBuilder.setNeutralButton(res.getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							getActivity().finish();
						}
					});

			break;
		case ServerRegistrationFailed:
		case FriendRequestNotAccepted:
		case InviteeNoInternetError:
		case ConnectionFailed:
		case FriendRequestTimedOut:
		case InviteeBusy:
			message = String
					.format(res
							.getString(R.string.friendreceiveinvitationmsg_errorafteracceptinginvitation),
							mSession.getUserName());
			alertDialogBuilder.setMessage(message);
			// set title
			alertDialogBuilder.setCancelable(false).setNegativeButton(
					res.getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							getActivity().finish();
						}
					});

			break;
		default:
			break;
		}

		return alertDialogBuilder.create();
	}

	public void setSession(StcSession mSession) {
		this.mSession = mSession;
	}

	public FriendSendInvitationState getFriendSendInvitationState() {
		return mFriendSendInvitationState;
	}

	public void setFriendSendInvitationState(
			FriendSendInvitationState mFriendSendInvitationState) {
		this.mFriendSendInvitationState = mFriendSendInvitationState;
	}

	public Context getContext() {
		return mContext;
	}

	public void setContext(Context mContext) {
		this.mContext = mContext;
	}
}