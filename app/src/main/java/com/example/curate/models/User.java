package com.example.curate.models;

import android.util.Log;

import com.example.curate.activities.MainActivity;
import com.parse.FunctionCallback;
import com.parse.Parse;
import com.parse.ParseClassName;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.livequery.ParseLiveQueryClient;
import com.parse.livequery.SubscriptionHandling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@ParseClassName("_User")
public class User extends ParseUser {

	private static MainActivity mParentActivity;

	public static String SCREEN_NAME_KEY = "screenName";

	public interface PartyDeletedListener {
		void onPartyDeleted(MainActivity mainActivity);
	}

	private List<PartyDeletedListener> partyDeletedListeners;

	public void initialize(MainActivity mainActivity) {
		mParentActivity = mainActivity;
		ParseLiveQueryClient parseLiveQueryClient = ParseLiveQueryClient.Factory.getClient();
		ParseQuery<ParseUser> parseQuery = ParseQuery.getQuery(ParseUser.class);
		parseQuery.include("currParty");
		parseQuery.whereEqualTo("objectId", ParseUser.getCurrentUser().getObjectId());
		SubscriptionHandling<ParseUser> userHandler = parseLiveQueryClient.subscribe(parseQuery);

		partyDeletedListeners = new ArrayList<>();

		userHandler.handleEvent(SubscriptionHandling.Event.UPDATE, (query, object) -> {
			if(object.get("currParty") == null && partyDeletedListeners != null) {
				for(PartyDeletedListener listener : partyDeletedListeners) {
					listener.onPartyDeleted(mParentActivity);
				}
				partyDeletedListeners = null;
			}
		});
	}

	public void registerPartyDeletedListener(PartyDeletedListener partyDeletedListener,
	                                         MainActivity mainActivity) {
		if(partyDeletedListeners == null) initialize(mainActivity);
		partyDeletedListeners.add(partyDeletedListener);
	}

	public void deregisterPartyDeletedListener(PartyDeletedListener partyDeletedListener) {
		if(partyDeletedListeners == null) return;
		partyDeletedListeners.remove(partyDeletedListener);
	}

	public void setScreenName(String screenName) {
		HashMap<String, Object> params = new HashMap<>();
		params.put(SCREEN_NAME_KEY, screenName);
		ParseCloud.callFunctionInBackground("setScreenName", params, new FunctionCallback<String>() {
			@Override
			public void done(String screenName, ParseException e) {
				if(e != null) {
					Log.e("User.java", "Couldn't set username", e);
				}
			}
		});
	}

	public static String getCurrentScreenName() {
		HashMap<String, Object> params = new HashMap<>();
		try {
			return ParseCloud.callFunction("getCurrentScreenName", params);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
}
