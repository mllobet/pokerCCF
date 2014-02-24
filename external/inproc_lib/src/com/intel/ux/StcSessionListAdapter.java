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

package com.intel.ux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.intel.inproclib.R;
import com.intel.stc.events.StcException;
import com.intel.stc.events.StcSessionUpdateEvent;
import com.intel.stc.interfaces.StcSessionUpdateListener;
import com.intel.stc.lib.StcLib;
import com.intel.stc.utility.StcSession;

public class StcSessionListAdapter extends BaseAdapter implements StcSessionUpdateListener
{

	public enum RelationshipModes
	{
		ALL, NEW, FAVORITE, NOT_FAVORITE, BLOCKED, MY_DEVICES
	}

	/* PointerList */
	volatile List<StcSession>				showList;

	/* Sorting */
	List<StcSession>						searchList			= new ArrayList<StcSession>();
	List<StcSession>						favoritesList;
	List<StcSession>						newList;
	List<StcSession>						blockedList			= new ArrayList<StcSession>();
	List<StcSession>						notBlockedList		= new ArrayList<StcSession>();
	List<StcSession>						notBlockedNewList	= new ArrayList<StcSession>();
	List<StcSession>						notBlockedFavList	= new ArrayList<StcSession>();
	List<StcSession>						myDevicesList		= new ArrayList<StcSession>();

	/* Current Interaction List */
	List<UUID>							excludedSessionList	= new ArrayList<UUID>();

	RelationshipModes					curMode				= RelationshipModes.ALL;
	RelationshipModes					excluded			= RelationshipModes.ALL;
	boolean								excludeMode			= false;
	String								searchString		= "";
	boolean								searching			= false;

	/* SessionList */
	public final StcLib					sLib;
	public final Context				ctx;
	public final UUID					filterUuid;
	private List<StcSession>				entireCurSessionList;
	private final Object				sessionLock			= new Object();
	private final StcSessionSelectedListener	sessionSelectedListener;

	/* CheckList */
	private boolean						isGallery			= false;
	private Map<UUID, StcSession>			checkedSessionMap		= null;
	private StcSessionCheckedListener				checkListener		= null;
	private boolean						usingCheckBox		= false;
	private int							numToCheck			= -1;
	private final Object				checkLock			= new Object();

	/* Update Variables */
	private Handler						handler;
	private final Object				handleLock			= new Object();

	private int							resId;

	/**
	 * 
	 * Constructor for selecting mutliple sessions via checkboxes.
	 * 
	 * @param sLib
	 *            - a valid STC Lib.
	 * @param ctx
	 *            - Application context.
	 * @param numToCheck
	 *            - Number of sessions allowed to be selected.
	 * @param checkListener
	 *            - Listener to be notified when sessions are selected or
	 *            deselected.
	 * @param appUuid
	 *            - The application uuid that we will filter the sessionlist on, if
	 *            set to null there will be no filtering performed.
	 */
	public StcSessionListAdapter(final StcLib sLib, final Context ctx, int numToCheck, final StcSessionCheckedListener checkListener,
			final UUID appUuid) {
		this(sLib, ctx, null, appUuid);

		if (numToCheck < 0)
			throw new RuntimeException("numToCheck is less than 0 in StcSessionListAdapter constructor");

		usingCheckBox = true;
		this.numToCheck = numToCheck;
		checkedSessionMap = new HashMap<UUID, StcSession>();
		this.checkListener = checkListener;
		this.resId = R.layout.userlistview_row;
	}

	/**
	 * 
	 * Constructor for selecting mutliple sessions via checkboxes.
	 * 
	 * @param sLib
	 *            - a valid STC Lib.
	 * @param ctx
	 *            - Application context.
	 * @param numToCheck
	 *            - Number of sessions allowed to be selected.
	 * @param checkListener
	 *            - Listener to be notified when sessions are selected or
	 *            deselected.
	 * @param appUuid
	 *            - The application uuid that we will filter the sessionlist on, if
	 *            set to null there will be no filtering performed.
	 * 
	 * @param isGallery
	 *            - Flag to show if the view is a gallery view or not. This is
	 *            false by default.
	 */
	public StcSessionListAdapter(final StcLib sLib, final Context ctx, int numToCheck, final StcSessionCheckedListener checkListener,
			final UUID appUuid, boolean isGallery) {
		this(sLib, ctx, null, appUuid);

		if (numToCheck < 0)
			throw new RuntimeException("numToCheck is less than 0 in StcStcSessionListAdapter constructor");

		// if (checkListener == null)
		// throw new
		// RuntimeException("checkLister is null in StcStcSessionListAdapter constructor");

		usingCheckBox = true;
		this.numToCheck = numToCheck;
		checkedSessionMap = new HashMap<UUID, StcSession>();
		this.checkListener = checkListener;
		this.resId = R.layout.userlistview_row;
		this.isGallery = isGallery;
	}

	public StcSessionListAdapter(final StcLib sLib, final Context ctx, int numToCheck, final StcSessionCheckedListener checkListener,
			final UUID appUuid, final int alternativeResId) {
		this(sLib, ctx, null, appUuid);

		if (numToCheck < 0)
			throw new RuntimeException("numToCheck is less than 0 in StcSessionListAdapter constructor");

		// if (checkListener == null)
		// throw new
		// RuntimeException("checkLister is null in StcSessionListAdapter constructor");

		usingCheckBox = true;
		this.numToCheck = numToCheck;
		checkedSessionMap = new HashMap<UUID, StcSession>();
		this.checkListener = checkListener;
		this.resId = alternativeResId;
	}

	public StcSessionListAdapter(final StcLib sLib, final Context ctx, int numToCheck, final StcSessionCheckedListener checkListener,
			final UUID appUuid, boolean isGallery, final int alternativeResId) {
		this(sLib, ctx, null, appUuid);

		if (numToCheck < 0)
			throw new RuntimeException("numToCheck is less than 0 in StcSessionListAdapter constructor");

		// if (checkListener == null)
		// throw new
		// RuntimeException("checkLister is null in StcSessionListAdapter constructor");

		usingCheckBox = true;
		this.numToCheck = numToCheck;
		checkedSessionMap = new HashMap<UUID, StcSession>();
		this.checkListener = checkListener;
		this.resId = alternativeResId;
		this.isGallery = isGallery;
	}

	/**
	 * 
	 * Constructor for single select sessionlist.
	 * 
	 * @param sLib
	 *            - a valid STC Lib.
	 * @param ctx
	 *            - Application context.
	 * @param sessionSelectedListener
	 *            - null if you don't want your list to be clickable, otherwise
	 *            notifies the listener when a session is selected.
	 * @param appUuid
	 *            The application uuid that we will filter the sessionlist on, if
	 *            set to null there will be no filtering performed.
	 */
	public StcSessionListAdapter(final StcLib sLib, final Context ctx, final StcSessionSelectedListener sessionSelectedListener,
			final UUID appUuid, final int alternativeResId) {
		if (sLib == null)
			throw new RuntimeException("StcLib is null in StcSessionListAdapter constructor");

		if (ctx == null)
			throw new RuntimeException("Context is null in StcSessionListAdapter constructor");

		this.filterUuid = appUuid;
		this.sLib = sLib;
		this.ctx = ctx;
		this.sessionSelectedListener = sessionSelectedListener;

		entireCurSessionList = new ArrayList<StcSession>();
		newList = new ArrayList<StcSession>();
		favoritesList = new ArrayList<StcSession>();
		this.resId = alternativeResId;
	}

	/**
	 * 
	 * Constructor for single select sessionlist.
	 * 
	 * @param sLib
	 *            - a valid STC Lib.
	 * @param ctx
	 *            - Application context.
	 * @param sessionSelectedListener
	 *            - null if you don't want your list to be clickable, otherwise
	 *            notifies the listener when a session is selected.
	 * @param appUuid
	 *            The application uuid that we will filter the sessionlist on, if
	 *            set to null there will be no filtering performed.
	 */
	public StcSessionListAdapter(final StcLib sLib, final Context ctx, final StcSessionSelectedListener sessionSelectedListener,
			final UUID appUuid) {
		if (sLib == null)
			throw new RuntimeException("StcLib is null in StcSessionListAdapter constructor");

		if (ctx == null)
			throw new RuntimeException("Context is null in StcSessionListAdapter constructor");

		this.filterUuid = appUuid;
		this.sLib = sLib;
		this.ctx = ctx;
		this.sessionSelectedListener = sessionSelectedListener;

		entireCurSessionList = new ArrayList<StcSession>();
		newList = new ArrayList<StcSession>();
		favoritesList = new ArrayList<StcSession>();

		this.resId = R.layout.userlistview_row;
	}

	// ***************
	// *Modes/Sorting*
	// ***************

	public void changeMode(final RelationshipModes mode)
	{
		synchronized (handleLock)
		{
			if (handler == null)
				return;

			handler.post(new Runnable() {

				@Override
				public void run()
				{
					onUiChangeMode(mode);
				}
			});
		}
	}

	public void setExcludeMode(final boolean excludeMode)
	{
		this.excludeMode = excludeMode;
		this.usingCheckBox = excludeMode;
		checkedSessionMap = new HashMap<UUID, StcSession>();
		changeMode(curMode);
	}

	public boolean getExcludeMode()
	{
		return excludeMode;
	}

	public RelationshipModes getCurrentMode()
	{
		return curMode;
	}

	// Changes in Mode reset search string.
	public void onUiChangeMode(final RelationshipModes mode)
	{
		synchronized (sessionLock)
		{
			curMode = mode;
			searching = false;

			if (excludeMode)
			{
				switch (mode)
				{
				case NOT_FAVORITE:
					showList = newList;
					break;
				case ALL:
					showList = notBlockedList;
					break;
				case NEW:
					showList = notBlockedNewList;
					break;
				case FAVORITE:
					showList = notBlockedFavList;
					break;
				}
			}
			else
			{
				switch (mode)
				{
				case FAVORITE:
					showList = favoritesList;
					break;
				case NEW:
					showList = newList;
					break;
				case BLOCKED:
					showList = blockedList;
					break;
				case MY_DEVICES:
					showList = myDevicesList;
					break;
				case ALL:
				default:
					showList = entireCurSessionList;
					break;
				}
			}

			removeExcluded();

			notifyDataSetChanged();
		}
	}

	public void setExcludeModeOnUi(final boolean excludeMode)
	{
		this.excludeMode = excludeMode;
		this.usingCheckBox = excludeMode;
		checkedSessionMap = new HashMap<UUID, StcSession>();
		onUiChangeMode(curMode);
	}

	public void setExcludedSessionList(final List<UUID> sessionList)
	{
		synchronized (handleLock)
		{
			if (handler == null)
				return;

			handler.post(new Runnable() {

				@Override
				public void run()
				{
					onUiSetExcludedList(sessionList);
				}
			});
		}
	}

	public void onUiSetExcludedList(List<UUID> sessionList)
	{
		excludedSessionList = sessionList;
		onUiChangeMode(curMode);
		removeExcluded();
		onUiSetSearchString(searchString);
	}

	public void setSearchString(final String searchString)
	{
		synchronized (handleLock)
		{
			if (handler == null)
				return;

			handler.post(new Runnable() {

				@Override
				public void run()
				{
					onUiSetSearchString(searchString);
				}
			});
		}
	}

	public void onUiSetSearchString(final String search)
	{
		synchronized (sessionLock)
		{
			if (search.compareTo("") == 0)
			{
				onUiChangeMode(curMode);
				return;
			}

			boolean searchComplete = false;
			char c, d;

			// If we were already searching, check to see if we are looking
			// for the same string with appended characters.
			if (searching)
			{
				searchString = searchString.replace("\\", "\\\\");
				searchString = searchString.replace("[", "\\[");
				searchString = searchString.replace("^", "\\^");
				searchString = searchString.replace("$", "\\$");
				searchString = searchString.replace(".", "\\.");
				searchString = searchString.replace("|", "\\|");
				searchString = searchString.replace("?", "\\?");
				searchString = searchString.replace("*", "\\*");
				searchString = searchString.replace("+", "\\+");
				searchString = searchString.replace("(", "\\(");
				searchString = searchString.replace(")", "\\)");
				searchString = searchString.replace("{", "\\{");
				searchString = searchString.replace("}", "\\}");

				String[] tempArray = search.split(searchString);
				// Continueing search!
				if (tempArray.length > 0 && tempArray[0].compareTo("") == 0)
				{
					searchComplete = true;
					searchString = search;
					for (int i = (searchList.size() - 1); i >= 0; i--)
					{
						String tempUserName = searchList.get(i).getUserName();

						boolean complete = false;
						int searchStringIndex = 0;
						for (int j = 0; j < tempUserName.length(); j++)
						{
							c = Character.toLowerCase(tempUserName.charAt(j));
							d = Character.toLowerCase(searchString.charAt(searchStringIndex));

							if (c == d)
								searchStringIndex++;

							if (searchStringIndex == searchString.length())
							{
								complete = true;
								break;
							}
						}

						if (!complete)
							searchList.remove(i);
					}
				}
			}

			// New search!
			if (!searchComplete)
			{
				onUiChangeMode(curMode);

				searching = true;
				searchString = search;
				searchList.clear();

				int searchSize = showList.size();

				for (int i = 0; i < searchSize; i++)
				{
					String tempUserName;

					StcSession tempSession = showList.get(i);

					tempUserName = tempSession.getUserName();

					boolean complete = false;
					int searchStringIndex = 0;
					for (int j = 0; j < tempUserName.length(); j++)
					{
						c = Character.toLowerCase(tempUserName.charAt(j));
						d = Character.toLowerCase(searchString.charAt(searchStringIndex));
						if (c == d)
							searchStringIndex++;

						if (searchStringIndex == searchString.length())
						{
							complete = true;
							break;
						}
					}

					if (complete)
						searchList.add(tempSession);
				}
			}
			showList = searchList;
			removeExcluded();
		}// End sync
		notifyDataSetChanged();
	}

	public void stopSearching()
	{
		synchronized (handleLock)
		{
			if (handler == null)
				return;
			handler.post(new Runnable() {

				@Override
				public void run()
				{
					onUiStopSearching();
				}
			});
		}
	}

	public void onUiStopSearching()
	{
		onUiChangeMode(curMode);
	}

	public void setResId(int resid)
	{
		this.resId = resid;
		notifyDataSetChanged();
	}

	// **************
	// * BaseAdapter*
	// **************

	@Override
	public int getCount()
	{
		synchronized (sessionLock)
		{
			if (showList != null)
				return showList.size();
			else
				return 0;
		}
	}

	@Override
	public Object getItem(int index)
	{
		synchronized (sessionLock)
		{
			if (showList != null && showList.size() > index && index >= 0)
				return showList.get(index);
			else
				return null;
		}
	}

	@Override
	public long getItemId(int arg0)
	{
		return arg0;
	}

	@Override
	public View getView(int index, View convertView, ViewGroup parent)
	{
		if (convertView == null)
		{
			LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(resId, null);
		}
		else
		{
			convertView.setVisibility(View.VISIBLE);
		}

		// Do we have a valid session?
		final StcSession session = (StcSession) getItem(index);
		if (session == null)
		{
			convertView.setVisibility(View.GONE);
			return convertView;
		}

		ImageView cert = null;
		try
		{
			cert = (ImageView) convertView.findViewById(R.id.ulv_security_ribbon);
		}
		catch (Exception e)
		{
			cert = null;
		}

		// Get views
		final ImageView avatar = (ImageView) convertView.findViewById(R.id.ulv_avatar);
		final TextView screenName = (TextView) convertView.findViewById(R.id.ulv_screenname);
		final TextView deviceName = (TextView) convertView.findViewById(R.id.ulv_devicename);
		final CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.ulv_checkbox);
		final ImageView favoriteIcon = (ImageView) convertView.findViewById(R.id.ulv_friendicon);
		final ImageView blockedIcon = (ImageView) convertView.findViewById(R.id.ulv_blockedicon);

		if (screenName != null)
		{
			screenName.setSelected(true);
			screenName.setText(session.getUserName());
		}
		
		if (deviceName != null)
		{
			deviceName.setSelected(true);
			deviceName.setText(session.getSessionName());
		}

		if (cert != null)
		{
			if (session.isRegisteredWithCloud())
			{
				cert.setVisibility(View.VISIBLE);

				//this is being done for automation testing
				cert.setContentDescription(String.format("%s is Registered", session.getUserName()));
				//////////////////////////////////////////////////
			}
			else
				cert.setVisibility(View.GONE);
		}

		// Are we using checkboxes?
		if (!usingCheckBox)
		{
			if (checkBox != null)
				checkBox.setVisibility(View.GONE);
			if (sessionSelectedListener != null)
			{
				convertView.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v)
					{
						sessionSelectedListener.stcSessionSelected(session);
					}
				});
			}
		}
		else
		{
			if (checkBox != null)
				checkBox.setVisibility(View.VISIBLE);
			synchronized (checkLock)
			{
				StcSession flag = checkedSessionMap.get(session.getSessionUuid());
				if (flag != null)
					checkBox.setChecked(true);
				else
					checkBox.setChecked(false);
			}
			checkBox.setClickable(!isGallery);
			if (checkListener != null)
			{
				checkBox.setOnClickListener(new CheckClickListener(session, checkBox));
				convertView.setOnClickListener(new CheckClickListener(session, checkBox));
				checkBox.setOnLongClickListener(new CheckClickListener(session, checkBox));
				convertView.setOnLongClickListener(new CheckClickListener(session, checkBox));
				checkBox.setHapticFeedbackEnabled(false);
				convertView.setHapticFeedbackEnabled(false);
			}
		}

		if (avatar != null)
		{
			avatar.setContentDescription("session_list_avatar");
			Bitmap b = session.getAvatar();
			if (b == null)
				avatar.setImageResource(R.drawable.generic_avatar);
			else
				avatar.setImageBitmap(b);
		}

		/*
		if (screenName != null)
			screenName.setText(session.getUserName());
		if (deviceName != null)
			deviceName.setText(session.getSessionName());
			*/
		
		if (blockedIcon != null)
		{
			if (session.getBlocked())		
			{
				blockedIcon.setVisibility(View.VISIBLE);
				blockedIcon.setContentDescription("user_list_blocked_true");
			}
			else
			{
				blockedIcon.setVisibility(View.INVISIBLE);
				blockedIcon.setContentDescription("user_list_blocked_false");
			}
		}
		if (favoriteIcon != null)
		{
			favoriteIcon.setVisibility(View.INVISIBLE);
			favoriteIcon.setContentDescription("user_list_favorite_false");
		}

		return convertView;
	}

	// ***********************
	// * UI Thread Updating. *
	// ***********************

	/**
	 * 
	 * This method should be called when you want to start activily using your
	 * adapter. It grabs a new sessionlist and clears the checked list if
	 * applicable.
	 * 
	 * @param handler
	 *            - handler to post the Sessionlist updates to the ui thread with.
	 */
	public void startListening(final Handler handler)
	{
		synchronized (handleLock)
		{
			this.handler = handler;
		}

		this.handler.post(new Runnable() {
			@Override
			public void run()
			{
				onUiStartListening(handler);
			}
		});
	}

	/**
	 * Must be called from the UI thread. Gets a new sessionlist, and clears the
	 * checkbox list if applicable.
	 */
	public void onUiStartListening(final Handler handler)
	{
		synchronized (handleLock)
		{
			this.handler = handler;
		}

		synchronized (sessionLock)
		{
			if (usingCheckBox)
			{
				synchronized (checkLock)
				{
					checkedSessionMap.clear();
				}
			}
			// new AsyncTask<Void, Void, Void>() {
			// @Override
			// protected Void doInBackground(Void... params)
			// {
			try
			{
				sLib.setStcSessionListListener(StcSessionListAdapter.this);
				entireCurSessionList = sLib.getSessionListWithAvatar();
			}
			catch (StcException e)
			{
				entireCurSessionList = new ArrayList<StcSession>();
			}
			// return null;
			// }

			// @Override
			// protected void onPostExecute(Void result)
			// {
			synchronized (handleLock)
			{
				if (handler != null)
				{
					fixSessionList();
					curMode = RelationshipModes.ALL;
				}
			}
			// }
			// }.execute(null);

		} // sessionLock

		notifyDataSetChanged();
	}

	public void stopListening()
	{
		synchronized (handleLock)
		{
			this.handler = null;
		}

		sLib.removeStcSessionListListener(this);
	}

	// *****************
	// * Adapter State *
	// *****************

	public void clearLists()
	{
		searchList.clear();
		favoritesList.clear();
		newList.clear();
		blockedList.clear();
		notBlockedList.clear();
		notBlockedNewList.clear();
		notBlockedFavList.clear();
		myDevicesList.clear();
	}

	public void setChecked(final UUID sessionUuid)
	{
		if (!usingCheckBox)
			return;

		synchronized (handleLock)
		{
			if (handler == null)
				return;

			handler.post(new Runnable() {

				@Override
				public void run()
				{
					toggleCheckBox(sessionUuid, false);
				}
			});
		}
	}

	public void onUiSetChecked(final UUID sessionUuid)
	{
		toggleCheckBox(sessionUuid, false);
	}

	public void removeCheck(final UUID sessionUuid)
	{
		if (!usingCheckBox)
			return;

		synchronized (handleLock)
		{
			if (handler == null)
				return;

			handler.post(new Runnable() {

				@Override
				public void run()
				{
					toggleCheckBox(sessionUuid, true);
				}
			});
		}
	}

	public void onUiRemoveCheck(final UUID sessionUuid)
	{
		toggleCheckBox(sessionUuid, true);
	}

	private void toggleCheckBox(final UUID sessionUuid, final boolean toRemove)
	{
		synchronized (checkLock)
		{
			final StcSession tempSession = checkedSessionMap.get(sessionUuid);

			// Trying to remove a session that isn't checked.
			if (toRemove && tempSession == null)
				return;
			// Trying to add a session that is already there.
			else if (!toRemove && tempSession != null)
				return;

			StcSession checkSession = null;
			try
			{
				checkSession = sLib.querySession(sessionUuid);
				if (checkSession == null)
					return;
			}
			catch (StcException e)
			{
				return;
			}

			if (toRemove)
			{
				checkedSessionMap.remove(sessionUuid);
				if (checkListener != null)
					checkListener.stcSessionUnchecked(checkSession);
			}
			else
			{
				checkedSessionMap.put(sessionUuid, checkSession);
				if (checkListener != null)
					checkListener.stcSessionChecked(checkSession);
			}
		} // checkLock
	}

	public List<StcSession> getChecked()
	{
		if (!usingCheckBox)
			return null;

		synchronized (checkLock)
		{
			final Set<UUID> i = checkedSessionMap.keySet();
			final ArrayList<StcSession> checkedSessionList = new ArrayList<StcSession>();
			for (UUID uuid : i)
			{
				StcSession temp = checkedSessionMap.get(uuid);
				if (temp != null)
					checkedSessionList.add(temp);
			}

			return checkedSessionList;
		}
	}

	// ***********
	// * Updates *
	// ***********

	@Override
	public void sessionUpdated(StcSessionUpdateEvent ue)
	{
		synchronized (handleLock)
		{
			if (handler == null)
				return;

			final UUID sessionUuid = ue.GetSessionUuid();

			StcSession intermediateSession = null;
			try
			{
				intermediateSession = sLib.querySession(sessionUuid);
				if (intermediateSession == null)
					return;
			}
			catch (StcException e)
			{
				return;
			}

			final StcSession tempSession = intermediateSession;

			handler.post(new Runnable() {

				@Override
				public void run()
				{
					uiUpdateList(tempSession);
				}
			});
		}
	}

	private void uiUpdateList(StcSession tempSession)
	{
		synchronized (sessionLock)
		{
			boolean found = false;
			int i = 0;
			for (StcSession iterSession : entireCurSessionList)
			{
				if (iterSession.getSessionUuid().compareTo(tempSession.getSessionUuid()) == 0)
				{
					found = true;
					break;
				}
				i++;
			}

			// session wasn't in our list and isn't available
			if (!found && !tempSession.isAvailable())
				return;

			// session wasn't in our list and is available.
			else if (!found && tempSession.isAvailable())
			{
				if (excludedSessionList == null || !excludedSessionList.contains(tempSession.getSessionUuid()))
				{
					entireCurSessionList.add(tempSession);

					newList.add(tempSession);
					if (tempSession.getBlocked())
						blockedList.add(tempSession);
					else
					{
						notBlockedList.add(tempSession);
						notBlockedNewList.add(tempSession);
					}
					if (tempSession.isSelf())
						myDevicesList.add(tempSession);
				}

				sortAll();
				notifyDataSetChanged();
				return;
			}

			// session is in our list and is no longer available
			else if (found && !tempSession.isAvailable())
			{
				// remove the checked session from the list if checked.
				synchronized (checkLock)
				{
					if (usingCheckBox)
					{
						final StcSession tempCheckSession = checkedSessionMap.remove(tempSession.getSessionUuid());
						if (tempCheckSession != null && checkListener != null)
							checkListener.stcSessionUnchecked(tempSession);
					}
				}
				entireCurSessionList.remove(i);
				findRemove(tempSession.getSessionUuid());

				notifyDataSetChanged();
				return;
			}

			// session is in our list and has been updated.
			else if (found && tempSession.isAvailable())
			{
				entireCurSessionList.remove(i);
				findRemove(tempSession.getSessionUuid());
				if (excludedSessionList == null || !excludedSessionList.contains(tempSession.getSessionUuid()))
				{
					entireCurSessionList.add(tempSession);
					newList.add(tempSession);
					if (tempSession.getBlocked())
						blockedList.add(tempSession);
					else
						notBlockedList.add(tempSession);
					if (tempSession.isSelf())
						myDevicesList.add(tempSession);
				}
				sortAll();
				notifyDataSetChanged();
				return;
			}
		}// sessionLock
	}

	// *********
	// *Utility*
	// *********

	private void fixSessionList()
	{
		synchronized (sessionLock)
		{
			for (int i = entireCurSessionList.size() - 1; i >= 0; i--)
			{
				final StcSession tempSession = entireCurSessionList.get(i);
				if (!tempSession.isAvailable())
				{
					entireCurSessionList.remove(i);
					continue;
				}

				if (filterUuid != null)
				{
					final UUID[] appList = tempSession.getAppList();
					boolean found = false;
					for (int j = 0; j < appList.length; j++)
					{
						if (appList[j].compareTo(filterUuid) == 0)
						{
							found = true;
							break;
						}
					}

					if (!found)
					{
						entireCurSessionList.remove(i);
						continue;
					}
				}

				newList.add(tempSession);
				if (tempSession.getBlocked())
					blockedList.add(tempSession);
				else
				{
					notBlockedList.add(tempSession);
					notBlockedNewList.add(tempSession);
				}
				if (tempSession.isSelf())
					myDevicesList.add(tempSession);
			}
			sortAll();
			showList = entireCurSessionList;
			removeExcluded();
		}
	}

	private void findRemove(UUID sessionUuid)
	{
		synchronized (sessionLock)
		{
			for (int i = favoritesList.size() - 1; i >= 0; i--)
			{
				final StcSession tempSession = favoritesList.get(i);
				if (tempSession.getSessionUuid().compareTo(sessionUuid) == 0)
				{
					favoritesList.remove(i);
					break;
				}
			}
			for (int i = newList.size() - 1; i >= 0; i--)
			{
				final StcSession tempSession = newList.get(i);
				if (tempSession.getSessionUuid().compareTo(sessionUuid) == 0)
				{
					newList.remove(i);
					break;
				}
			}
			for (int i = myDevicesList.size() - 1; i >= 0; i--)
			{
				final StcSession tempSession = myDevicesList.get(i);
				if (tempSession.getSessionUuid().compareTo(sessionUuid) == 0)
				{
					myDevicesList.remove(i);
					break;
				}
			}
			for (int i = blockedList.size() - 1; i >= 0; i--)
			{
				final StcSession tempSession = blockedList.get(i);
				if (tempSession.getSessionUuid().compareTo(sessionUuid) == 0)
				{
					blockedList.remove(i);
					return;
				}
			}
			for (int i = notBlockedList.size() - 1; i >= 0; i--)
			{
				final StcSession tempSession = notBlockedList.get(i);
				if (tempSession.getSessionUuid().compareTo(sessionUuid) == 0)
				{
					notBlockedList.remove(i);
					break;
				}
			}
			for (int i = notBlockedNewList.size() - 1; i >= 0; i--)
			{
				final StcSession tempSession = notBlockedNewList.get(i);
				if (tempSession.getSessionUuid().compareTo(sessionUuid) == 0)
				{
					notBlockedNewList.remove(i);
					return;
				}
			}
			for (int i = notBlockedFavList.size() - 1; i >= 0; i--)
			{
				final StcSession tempSession = notBlockedFavList.get(i);
				if (tempSession.getSessionUuid().compareTo(sessionUuid) == 0)
				{
					notBlockedFavList.remove(i);
					return;
				}
			}
		}
	}

	private void sortAll()
	{
		synchronized (sessionLock)
		{
			Collections.sort(entireCurSessionList);
			Collections.sort(favoritesList);
			Collections.sort(newList);
			Collections.sort(blockedList);
			Collections.sort(notBlockedFavList);
			Collections.sort(notBlockedList);
			Collections.sort(notBlockedNewList);
			Collections.sort(myDevicesList);
		}
	}

	private void removeExcluded()
	{
		synchronized (sessionLock)
		{
			if (excludedSessionList == null)
				return;
			for (UUID eUUID : excludedSessionList)
			{
				for (int i = 0; i < showList.size(); i++)
				{
					StcSession session = showList.get(i);
					if (eUUID.compareTo(session.getSessionUuid()) == 0)
					{
						showList.remove(session);
						break;
					}
				}
			}
		}
		notifyDataSetChanged();
	}

	public boolean usingChecked()
	{
		return usingCheckBox;
	}

	// **************
	// * Interfaces *
	// **************

	public interface StcSessionSelectedListener
	{
		public void stcSessionSelected(StcSession session);
	}

	public interface StcSessionCheckedListener
	{
		public void stcSessionChecked(StcSession session);

		public void stcSessionUnchecked(StcSession session);
	}

	// *****************
	// * Inner Classes *
	// *****************

	private class CheckClickListener implements OnClickListener, OnLongClickListener
	{

		private final StcSession	session;
		private final CheckBox	checkBox;

		public CheckClickListener(StcSession session, CheckBox checkBox) {
			this.session = session;
			this.checkBox = checkBox;
		}

		@Override
		public void onClick(View v)
		{
			if (v.getId() != checkBox.getId())
			{
				// This wasn't the checkbox clicked, so toggle the checkBox
				checkBox.toggle();
			}
			toggleCheck(v);
		}

		@Override
		public boolean onLongClick(View v)
		{
			checkBox.toggle();
			toggleCheck(v);
			return true;
		}

		private void toggleCheck(View v)
		{
			final boolean isChecked = checkBox.isChecked();

			synchronized (checkLock)
			{
				// We're trying to check this box.
				if (isChecked)
				{
					final int prevChecked = checkedSessionMap.size();

					// We have room to check more sessions.
					if (prevChecked < numToCheck)
					{
						final StcSession temp = checkedSessionMap.get(session.getSessionUuid());

						checkBox.setChecked(true);

						// session is already checked.
						if (temp != null)
							return;

						checkedSessionMap.put(session.getSessionUuid(), session);
						if (checkListener != null)
							checkListener.stcSessionChecked(session);
					}

					// We Can't allow any more sessions to be checked.
					else
						checkBox.setChecked(false);
				}

				// We're unchecking this box.
				else
				{
					final StcSession tempSession = checkedSessionMap.remove(session.getSessionUuid());
					checkBox.setChecked(false);

					if (tempSession == null)
						return;

					if (checkListener != null)
						checkListener.stcSessionUnchecked(session);
				}
			}
		}

	}
}