package com.example.curate.models;

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

	public interface PartyDeletedListener {
		void onPartyDeleted();
	}

	private List<PartyDeletedListener> partyDeletedListeners;

	public void initialize() {
		ParseLiveQueryClient parseLiveQueryClient = ParseLiveQueryClient.Factory.getClient();
		ParseQuery<ParseUser> parseQuery = ParseQuery.getQuery(ParseUser.class);
		parseQuery.include("currParty");
		parseQuery.whereEqualTo("objectId", ParseUser.getCurrentUser().getObjectId());
		SubscriptionHandling<ParseUser> userHandler = parseLiveQueryClient.subscribe(parseQuery);
		partyDeletedListeners = new ArrayList<>();
		userHandler.handleEvent(SubscriptionHandling.Event.UPDATE, (query, object) -> {
			if(object.get("currParty") == null) {
				for(PartyDeletedListener listener : partyDeletedListeners) {
					listener.onPartyDeleted();
				}
			}
		});
	}

	public void registerPartyDeletedListener(PartyDeletedListener partyDeletedListener) {
		if(partyDeletedListeners == null) initialize();
		partyDeletedListeners.add(partyDeletedListener);
	}

	public void deregisterPartyDeletedCallback(PartyDeletedListener partyDeletedListener) {
		if(partyDeletedListeners == null) return;
		partyDeletedListeners.remove(partyDeletedListener);
	}
}
