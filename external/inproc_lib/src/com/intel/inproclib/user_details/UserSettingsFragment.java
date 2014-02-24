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

package com.intel.inproclib.user_details;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.intel.inproclib.R;
import com.intel.mw.PlatformHelper;
import com.intel.stc.events.InviteRequestEvent;
import com.intel.stc.events.InviteResponseEvent;
import com.intel.stc.interfaces.StcConnectionListener;
import com.intel.stclibcc.StcLibCC;

public class UserSettingsFragment extends Fragment implements
		StcConnectionListener {

	private boolean isPaused = false;
	private StcLibCC mLib;

	private View mCurrentView;

	public static UserSettingsFragment getFragment() {

		return new UserSettingsFragment();

	}

	public UserSettingsFragment() {

	}

	/* Fragment Stuff */

	@Override
	public void onResume() {
		super.onResume();
		isPaused = false;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		CreateLib libTask = new CreateLib(inflater);
		libTask.execute();

		mCurrentView = inflater.inflate(R.layout.loading_spinner, null);

		return mCurrentView;
	}

	@Override
	public void onPause() {
		super.onPause();
		isPaused = true;

		if (mLib != null) {
			try {
				mLib.disconnectFromPlatform();
			} catch (Exception e) {
			}
		}
	}

	/* Inner Classes */

	private class CreateLib extends AsyncTask<Void, Void, Void> {

		private StcLibCC tempLib;
		final LayoutInflater mInflator;

		public CreateLib(LayoutInflater inflator) {
			mInflator = inflator;
		}

		@Override
		protected Void doInBackground(Void... params) {

			try {
				tempLib = new StcLibCC(PlatformHelper.GetPath(),
						UserSettingsFragment.this);
			} catch (Exception e) {
				tempLib = null;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// We aren't paused yet.
			if (!isPaused) {
				// Failure
				if (mLib == null) {
					final Activity tempActivity = getActivity();
					if (tempActivity != null) {
						Toast.makeText(tempActivity, "FAIL", Toast.LENGTH_LONG)
								.show();

						tempActivity.finish();
					}
				}
				// Success!
				else {
					mLib = tempLib;

					mCurrentView = mInflator.inflate(R.layout.cloud_webview,
							null);
				}
			}
		}

	}

	/* Ignore All */

	@Override
	public void connectionRequest(InviteRequestEvent ire) {
	}

	@Override
	public void connectionCompleted(InviteResponseEvent ire) {
	}

}
