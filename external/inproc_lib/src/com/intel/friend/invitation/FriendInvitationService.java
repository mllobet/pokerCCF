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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.intel.inproclib.R;
import com.intel.inproclib.utility.InProcConstants;
import com.intel.startup.CloudAuthorizationActivity;
import com.intel.startup.NewUnbox;
import com.intel.stc.events.InviteRequestEvent;
import com.intel.stc.events.InviteResponseEvent;
import com.intel.stc.events.StcException;
import com.intel.stc.events.StcSessionUpdateEvent;
import com.intel.stc.interfaces.IStcActivity;
import com.intel.stc.interfaces.StcConnectionListener;
import com.intel.stc.interfaces.StcLocalSessionUpdateListener;
import com.intel.stc.interfaces.StcSessionUpdateListener;
import com.intel.stc.lib.GadgetRegistration;
import com.intel.stc.lib.StcLib;
import com.intel.stc.slib.StcServiceInet;
import com.intel.stc.utility.StcApplicationId;
import com.intel.stc.utility.StcConstants;
import com.intel.stc.utility.StcSession;
import com.intel.stc.utility.StcSocket;
import com.intel.stc.utility.d;

public class FriendInvitationService extends StcServiceInet implements
		StcSessionUpdateListener, StcConnectionListener,
		IDataStreamEventListener, StcLocalSessionUpdateListener, IStcActivity {

	static final String tag =  "FriendInvitation";

	public enum FriendInvitationState {

		NOT_CONNECTED, // we have not yet connected
		INVITE_SENT, // we have sent an invite.
		INVITE_RECEIVED, // we have received an invite.
		CONNECTED, // we are connected
		CONNECTION_CLOSED // we have been connected, but are no longer.
	};

	// lines of text received from the other side
	ArrayList<IFriendInvitationEventListener> mListeners = new ArrayList<IFriendInvitationEventListener>();
	StcSocket mSocket = null;
	WriteEngine mWriteDataStream;
	ReadEngine mReadDataStream;

	Bundle mInitBundle;
	boolean mBundleParsed = false;
	FriendInvitationState mState = FriendInvitationState.NOT_CONNECTED;

	// Application Registration
	public static final String					LAUNCH_INTENT		= "com.intel.friend.invitation";
	public static final String					SERVICE_NAME		= "com.intel.friend.invitation.FriendInvitationtService";
	private static final UUID					appId				= UUID.fromString(StcConstants.FRIEND_INVITATION_UUID);
	private static final String					clientId			= "5c549b0cbb39ee419cad87550d877911";
	private static final String					clientSecret		= "a6b0e97e2ce15804";
	private static final String					redirectUrl			= "http://www.intel.com/robots.txt";
	private static final boolean				allowCloudTransport	= true;

	static final StcApplicationId				id					= new StcApplicationId(appId, clientId,
																			clientSecret,
																			allowCloudTransport);

	public static final String FLAG_INVITE_USERS = "FLAG_INVITE_USERS";
	public static final String FLAG_CLEAR_SERVICE = "FLAG_CLEAR_SERVICE";
	public static final String FLAG_INVITED = "FLAG_INVITED";
	public static final String NUM_USERS = "NUM_USERS";
	StcLib msLib = null;
	public static final String USER_ID_PREFIX = "USER_ID";
	public static final String ABORT_ID = "ABORT_ID";
	InviteRequestEvent ire = null;

	static public GadgetRegistration getGadgetRegistration(Context context) {
		String appName = context.getString(R.string.app_name);
		String appDescription = context.getString(R.string.app_description);

		return new GadgetRegistration(appName, R.drawable.ic_launcher,
				StcConstants.FRIEND_INVITATION_UUID, appDescription,
				LAUNCH_INTENT, 2, R.string.schat_inv_text,
				R.string.timeout_toast_text, 0, context);
	}

	public Bundle mBundle = null;
	private boolean mInvited;
	private StcSession mInitiatorSession;
	static UUID mSessionUuid;

	public static void requestBuddy(Context context, StcSession session) {
		mSessionUuid = session.getSessionUuid();
		Intent i = new Intent(context, FriendSendInvitationActivity.class);
		i.putExtra("reason", 0);
		i.putExtra("session", session.getSessionUuid().toString());
		context.startActivity(i);

	}

	public int addBuddy() {
		int value = -1;
//		try {
//			if (mSessionUuid != null) {
//				value = getSTCLib().subscribeByUser(mSessionUuid);
//
//			}
//		} catch (StcException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		return value;
	}

	public int removeBuddy() {
		int value = -1;
//		try {
//			if (mSessionUuid != null) {
//				value = getSTCLib().unsubscribeByUser(mSessionUuid);
//
//			}
//		} catch (StcException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		return value;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {

			mBundle = intent.getExtras();
			if (mBundle != null) {
				mInvited = mBundle.getBoolean(FLAG_INVITED);

				// INVITED
				if (mInvited) {
					int handle = mBundle.getInt(StcConstants.STC_NOTE_HANDLE);
					String userIdString = mBundle
							.getString(StcConstants.STC_NOTE_SESSION_UUID);
					// UUID userId = null;
					if (userIdString != null) {
						try {
							mSessionUuid = UUID.fromString(userIdString);
						} catch (IllegalArgumentException e) {
							return Service.START_STICKY;
						}
					}

					ire = new InviteRequestEvent(this, mSessionUuid, handle,
							getAppId().appId);
					synchronized (this) {
						if (msLib != null)
							connectionRequest(ire);
					}
				} // end INVITED

			}
		}
		return Service.START_STICKY;
	}

	@Override
	protected void stcLibPrepared(StcLib slib) {
		if (slib == null)
			throw new FriendInvitationError(
					"the stclib is null in stcLibPrepared. Have you given your app local socket permissions? Have you given your app a registration object?");
		msLib = slib;
		slib.setConnectionListener(this);
		slib.setStcSessionListListener(this);
		slib.addLocalSessionListener(this);
		tryParseBundle();
		if (slib != null && ire != null && mInvited) {
			try {
				mInitiatorSession = slib.querySession(mSessionUuid);
				connectionRequest(ire);
			} catch (StcException e) {
				e.printStackTrace();
			}
		}

		postSessionListChanged();
		getApplicationContext();
	}

	@Override
	protected void stcPlatformMissing() {
		// TODO Auto-generated method stub

	}

	@Override
	public StcApplicationId getAppId() {
		return id;
	}

	@Override
	public StcConnectionListener getConnListener() {
		return this;
	}

	// /
	// / STC NOTIFICATION METHODS
	// /

	public void sessionUpdated(StcSessionUpdateEvent arg) {
		postSessionListChanged();
	}

	@Override
	public void localSessionUpdated() {
		postLocalSessionChanged();
	}

	public void connectionCompleted(InviteResponseEvent arg) {
		boolean connectionComplete = false;
		synchronized (mState) {
			if (arg.getStatus() == InviteResponseEvent.InviteStatus.sqtAccepted) {
				try {
					mSocket = getSTCLib().getPreparedSocket(
							arg.getSessionGuid(), arg.getConnectionHandle());

					InputStream iStream = mSocket.getInputStream();
					mReadDataStream = new ReadEngine(iStream, this);

					OutputStream oStream = mSocket.getOutputStream();
					mWriteDataStream = new WriteEngine(oStream, this);

					connectionComplete = true;
				} catch (StcException e) {
					d.error(InProcConstants.INPROC_TAG, tag, "connectionCompleted", e);
				}
			}

			if (connectionComplete)
				mState = FriendInvitationState.CONNECTED;
			else
				mState = FriendInvitationState.CONNECTION_CLOSED;
		}
		postConnected(connectionComplete);
	}

	public void connectionRequest(InviteRequestEvent arg) {
		doConnectionRequest(arg.getSessionUuid(), arg.getConnectionHandle());
	}

	private void doConnectionRequest(UUID uuid, int handle) {
		synchronized (mState) {
			if (mState == FriendInvitationState.NOT_CONNECTED) {
				boolean connected = false;
				try {
					if (mState == FriendInvitationState.CONNECTED)
						getSTCLib().rejectInvitation(handle);
					else
						mSocket = getSTCLib().acceptInvitation(uuid, handle);

					InputStream iStream = mSocket.getInputStream();
					mReadDataStream = new ReadEngine(iStream, this);

					OutputStream oStream = mSocket.getOutputStream();
					mWriteDataStream = new WriteEngine(oStream, this);
					connected = true;
				} catch (StcException e) {
					d.error(InProcConstants.INPROC_TAG, tag, "doConnectionRequest", e);
				}

				if (connected) {
					mState = FriendInvitationState.CONNECTED;
				} else {
					mState = FriendInvitationState.CONNECTION_CLOSED;
				}

				if (mInvited) {
					showNotification();
				}

			}
		}

	}

	static final boolean mHoneyComb = android.os.Build.VERSION.SDK_INT >= 11;
	static final boolean mJellyBean = android.os.Build.VERSION.SDK_INT >= 16;

	private void showNotification() {
		if (mJellyBean) {
			showV16Notification();
		} else if (mHoneyComb) {
			showV11Notification();
		} else {
			showV4Notification();
		}
	}

	private void showV4Notification() {
		Intent intent = new Intent(this, FriendReceiveInvitationActivity.class);
		PendingIntent pendingIndent = PendingIntent.getActivity(this, 0,
				intent, 0);
		Notification notify = new Notification();

		notify.defaults |= Notification.DEFAULT_ALL;
		notify.flags = Notification.FLAG_AUTO_CANCEL;

		notify.when = System.currentTimeMillis();
		notify.contentIntent = pendingIndent;

		notify.icon = R.drawable.ccf_icon;

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notify.flags |= Notification.FLAG_AUTO_CANCEL;

		notificationManager.notify(mInitiatorSession.getSessionUuid()
				.hashCode(), notify);

	}

	@TargetApi(11)
	public void showV11Notification() {
		Intent intent = new Intent(this, FriendReceiveInvitationActivity.class);
		PendingIntent pendingIndent = PendingIntent.getActivity(this, 0,
				intent, 0);

		Resources res = getResources();
		String message = String
				.format(res
						.getString(R.string.friendreceiveinvitationmsg_invitationreceived),
						mInitiatorSession.getUserName());
		int height = (int) res
				.getDimension(android.R.dimen.notification_large_icon_height);
		int width = (int) res
				.getDimension(android.R.dimen.notification_large_icon_width);
		Bitmap avatar = Bitmap.createScaledBitmap(
				mInitiatorSession.getAvatar(), width, height, false);
		Notification notify = new Notification.Builder(this)
				.setWhen(System.currentTimeMillis())
				.setDefaults(Notification.DEFAULT_ALL)
				.setContentTitle(
						res.getString(R.string.friendreceiveinvitation_title))
				.setContentText(message).setSmallIcon(R.drawable.ccf_icon)
				.setLargeIcon(avatar).setContentIntent(pendingIndent)
				.getNotification();
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notify.flags |= Notification.FLAG_AUTO_CANCEL;

		notificationManager.notify(mInitiatorSession.getSessionUuid()
				.hashCode(), notify);

	}

	@TargetApi(16)
	public void showV16Notification() {
		Intent intent = new Intent(this, FriendReceiveInvitationActivity.class);
		PendingIntent pendingIndent = PendingIntent.getActivity(this, 0,
				intent, 0);

		Resources res = getResources();
		String message = String
				.format(res
						.getString(R.string.friendreceiveinvitationmsg_invitationreceived),
						mInitiatorSession.getUserName());
		int height = (int) res
				.getDimension(android.R.dimen.notification_large_icon_height);
		int width = (int) res
				.getDimension(android.R.dimen.notification_large_icon_width);
		Bitmap avatar = Bitmap.createScaledBitmap(
				mInitiatorSession.getAvatar(), width, height, false);
		Notification notify = new Notification.Builder(this)
				.setWhen(System.currentTimeMillis())
				.setDefaults(Notification.DEFAULT_ALL)
				.setContentTitle(
						res.getString(R.string.friendreceiveinvitation_title))
				.setContentText(message).setSmallIcon(R.drawable.ccf_icon)
				.setLargeIcon(avatar).setContentIntent(pendingIndent).build();
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notify.flags |= Notification.FLAG_AUTO_CANCEL;

		notificationManager.notify(mInitiatorSession.getSessionUuid()
				.hashCode(), notify);

	}

	// /
	// / PUBLIC METHODS FOR SERVICE USERS TO REQUEST ACTIONS
	// /

	public boolean inviteSession(StcSession session) {
		synchronized (mState) {
			if (mState != FriendInvitationState.NOT_CONNECTED)
				return false;

			if (!session.isAvailable()) {
				return false;
			}
			try {
				getSTCLib().inviteSession(session.getSessionUuid(), (short) 20);
				return true;
			} catch (StcException e) {
				d.error(InProcConstants.INPROC_TAG, tag, "inviteSession", e);
			}

		}

		return false;
	}

	public List<StcSession> getSessions() {
		StcLib lib = getSTCLib();
		if (lib != null) {
			try {
				return lib.getSessionListWithAvatar();
			} catch (StcException e) {
				d.error(InProcConstants.INPROC_TAG, tag, "unexpected exception", e);
			}
		}
		return new ArrayList<StcSession>();
	}

	public void writeBuffer(int statusCode) {

		mWriteDataStream.writeBuffer(mWriteDataStream
				.constructStatusPacket(statusCode));
	}

	public void exitService() {
		d.print(InProcConstants.INPROC_TAG, tag, "exitService", "Enter");
		synchronized (mState) {
			mState = FriendInvitationState.CONNECTION_CLOSED;

			if (mWriteDataStream != null)
				mWriteDataStream.stop();
			if (mReadDataStream != null)
				mReadDataStream.stop();
			if (mSocket != null) {
				try {
					mSocket.close();
				} catch (StcException e) {
					d.error(InProcConstants.INPROC_TAG, tag, "exitService", e);
				}
				mSocket = null;
			}
		}
		d.print(InProcConstants.INPROC_TAG, tag, "exitService", "exit");
		stopSelf();
	}

	public void rejectInvite(int handle) {
		StcLib lib = getSTCLib();
		try {
			if (lib != null)
				lib.rejectInvitation(handle);
		} catch (StcException e) {
			d.error(InProcConstants.INPROC_TAG, tag, "rejectInvite", e);
		}
	}

	public void parseInitBundle(Bundle bundle) {
		mInitBundle = bundle;
		mBundleParsed = false;
		tryParseBundle();
	}

	private void tryParseBundle() {
		StcLib lib = getSTCLib();
		if (lib == null)
			return;

		synchronized (this) {
			if (mBundleParsed)
				return;
			mBundleParsed = true;
		}

		lib.parseStartMethod(this, mInitBundle, this);
	}

	// /
	// / LISTENER MANAGEMENT AND EVENT PROPAGATION
	// /
	public boolean addListener(IFriendInvitationEventListener listener) {
		synchronized (mListeners) {
			if (mListeners.contains(listener))
				return false;
			else {
				boolean ret = mListeners.add(listener);
				return ret;
			}
		}
	}

	public boolean removeListener(IFriendInvitationEventListener listener) {
		synchronized (mListeners) {
			return mListeners.remove(listener);
		}
	}

	private void postConnected(final boolean connected) {
		if (!connected) {
			statusAvailable(FriendInvitationMessage.AnErrorOccurred.getValue());
			this.stopSelf();

		}
	}

	private void postSessionListChanged() {
		synchronized (mListeners) {
			for (IFriendInvitationEventListener l : mListeners)
				l.sessionListChanged();
		}
	}

	private void postLocalSessionChanged() {
		synchronized (mListeners) {
			for (IFriendInvitationEventListener l : mListeners)
				l.localSessionChanged();
		}
	}

	public void statusAvailable(int line) {
		synchronized (mListeners) {
			for (IFriendInvitationEventListener l : mListeners)
				l.dataReceived(line);
		}
	}

	public void remoteDisconnect() {
		d.print(InProcConstants.INPROC_TAG, tag, "remoteDisconnect", "Enter");

		synchronized (mListeners) {
			for (IFriendInvitationEventListener l : mListeners) {
				l.remoteDisconnect();
			}
		}
		exitService();
		d.print(InProcConstants.INPROC_TAG, tag, "remoteDisconnect", "Exit");
	}

	// /
	// / CALLBACKS FROM BUNDLE PARSING
	// /

	@Override
	public void onStartNormal() {
	}

	@Override
	public void onStartServer(UUID sessionUuid) {
	}

	@Override
	public void onStartClient(UUID inviterUuid, int inviteHandle) {
		doConnectionRequest(inviterUuid, inviteHandle);
	}

	// /
	// / SERVICE LIFECYCLE
	// /

	@Override
	public void onDestroy() {

		d.print(InProcConstants.INPROC_TAG, tag, "onDestroy", "Enter/Exit");
		if (mWriteDataStream != null)
			mWriteDataStream.stop();
		if (mReadDataStream != null)
			mReadDataStream.stop();

		super.onDestroy();
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public Intent GetCloudIntent() {
		Intent intent = new Intent(getApplicationContext(), CloudAuthorizationActivity.class);
		intent.putExtra("clientID", id.clientId);
		intent.putExtra("redirectURL", redirectUrl);
		return intent;
	}

	@Override
	public Class<?> GetUnboxActivityClass() {
		return NewUnbox.class;
	}

	public StcSession getInitiatorSession() {

		return mInitiatorSession;
	}
}
