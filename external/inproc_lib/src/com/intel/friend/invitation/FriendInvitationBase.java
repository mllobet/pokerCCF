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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;

import com.intel.inproclib.utility.InProcConstants;
import com.intel.stc.lib.StcLib;
import com.intel.stc.slib.IStcServInetClient;
import com.intel.stc.utility.d;

public abstract class FriendInvitationBase extends FragmentActivity implements
		IFriendInvitationEventListener, IStcServInetClient {
	static final String tag =  "FriendInvitation";

	@Override
	public void onBackPressed() {
		finish();
		doStopService();
		super.onBackPressed();
	}

	private static final int STCLIB_ACTIVITY_REQUEST = 23;

	protected FriendInvitationService mFriendService;
	private FriendInvitationServiceConnection mConnection = new FriendInvitationServiceConnection();
	boolean isBound = false;

	Handler myHandler = new Handler();

	@Override
	protected void onResume() {
		doBindService();
		super.onResume();
	}

	@Override
	protected void onPause() {
		doUnbindService();
		super.onPause();
	}

	abstract protected void onStcLibPrepared();

	private void doBindService() {
		if (!isBound) {
			Intent servIntent = new Intent(FriendInvitationService.SERVICE_NAME);
			isBound = bindService(servIntent, mConnection, 0);
			if (!isBound)
				d.error(tag, "doBindService", "service did not bind.");
		}
	}

	private void doUnbindService() {
		d.print(InProcConstants.INPROC_TAG, tag, "doUnbindService", "Enter");
		if (isBound) {
			isBound = false;
			unbindService(mConnection);
		}
		d.print(InProcConstants.INPROC_TAG, tag, "doUnbindService", "Exit");
	}

	protected void doStartService() {
		Intent servIntent = new Intent(FriendInvitationService.SERVICE_NAME);
		startService(servIntent);
	}

	protected void doStopService() {
		d.print(InProcConstants.INPROC_TAG, tag, "doStopService", "Enter");
		Intent servIntent = new Intent(FriendInvitationService.SERVICE_NAME);

		mConnection.serviceExited();
		doUnbindService();
		stopService(servIntent);
		d.print(InProcConstants.INPROC_TAG, tag, "doStopService", "Exit");
	}

	@Override
	public void platformError() {
	}

	@Override
	public void libPrepared(StcLib lib) {
		if (lib != null) {
			onStcLibPrepared();
		}
	}

	@Override
	public void platformMissing() {
	}

	@Override
	public void requestStartActivityForResult(Intent i) {
		startActivityForResult(i, STCLIB_ACTIVITY_REQUEST);
	}

	public class FriendInvitationServiceConnection implements ServiceConnection {
		boolean serviceStopped = false;

		public void onServiceConnected(ComponentName className, IBinder binder) {
			synchronized (this) {

				mFriendService = (FriendInvitationService) ((FriendInvitationService.StcServInetBinder) binder)
						.getService();
				if (serviceStopped)
					mFriendService.exitService();
				else {
					mFriendService.addListener(FriendInvitationBase.this);
					mFriendService.setLibPreparedCallback(
							FriendInvitationBase.this, myHandler);
				}
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			d.print(InProcConstants.INPROC_TAG, tag, "onServiceDisconnected", "Enter");

			mFriendService.removeListener(FriendInvitationBase.this);
			mFriendService = null;
			d.print(InProcConstants.INPROC_TAG, tag, "onServiceDisconnected", "Exit");
		}

		public void serviceExited() {
			d.print(InProcConstants.INPROC_TAG, tag, "serviceExited", "Enter");
			synchronized (this) {
				serviceStopped = true;
			}
			d.print(InProcConstants.INPROC_TAG, tag, "serviceExited", "Exit");
		}
};

}
