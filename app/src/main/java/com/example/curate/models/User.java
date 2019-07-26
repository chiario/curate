package com.example.curate.models;

import com.example.curate.activities.MainActivity;
import com.parse.FunctionCallback;
import com.parse.ParseClassName;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.livequery.ParseLiveQueryClient;
import com.parse.livequery.SubscriptionHandling;

import java.util.ArrayList;
import java.util.List;

@ParseClassName("_User")
public class User extends ParseUser {

	private static MainActivity mParentActivity;

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

	public void deregisterPartyDeletedCallback(PartyDeletedListener partyDeletedListener) {
		if(partyDeletedListeners == null) return;
		partyDeletedListeners.remove(partyDeletedListener);
	}
}
