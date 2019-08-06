package com.example.curate.models;

import android.util.Log;

import androidx.annotation.Nullable;

import com.parse.FunctionCallback;
import com.parse.ParseClassName;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.livequery.ParseLiveQueryClient;
import com.parse.livequery.SubscriptionHandling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@ParseClassName("Party")
public class Party extends ComparableParseObject {
    private static final String ADMIN_KEY = "admin";
    private static final String JOIN_CODE_KEY = "joinCode";
    private static final String CURRENTLY_PLAYING_KEY = "currPlaying";
    private static final String PLAYLIST_LAST_UPDATED_KEY = "playlistLastUpdatedAt";
    private static final String NAME_KEY = "name";
    private static final String LOCATION_KEY = "location";
    private static final String LOCATION_PERMISSION_KEY = "locationEnabled";
    private static final String CACHED_PLAYLIST_KEY = "cachedPlaylist";
    private static final String USER_LIMIT_KEY = "userLimit";
    private static final String SONG_LIMIT_KEY = "songLimit";
    private static final String USER_COUNT_KEY = "userCount";

    private static Party mCurrentParty;

    private final Playlist mPlaylist;
    private final List<SaveCallback> mCurrentlyPlayingUpdateCallbacks;
    private ParseLiveQueryClient mLiveQueryClient;

    public Party() {
        mPlaylist = new Playlist();
        mCurrentlyPlayingUpdateCallbacks = new ArrayList<>();
    }

    /**
     * Initializes the party object and sets up live queries
     */
    private void initialize() {
        String cachedPlaylist = getString(CACHED_PLAYLIST_KEY);
        Date cachedTime = getDate(PLAYLIST_LAST_UPDATED_KEY);
        if(cachedPlaylist != null && cachedTime != null) {
            mPlaylist.updateFromCache(cachedTime, cachedPlaylist);
        }

        initializeLiveQuery();
    }

    public void initializeLiveQuery() {
        mLiveQueryClient = ParseLiveQueryClient.Factory.getClient();

        // Set up live query
        ParseQuery<Party> parseQuery = ParseQuery.getQuery(Party.class);
        parseQuery.include(CURRENTLY_PLAYING_KEY);
        parseQuery.whereEqualTo("objectId", getObjectId());
        parseQuery.selectKeys(Arrays.asList(CACHED_PLAYLIST_KEY, CURRENTLY_PLAYING_KEY, PLAYLIST_LAST_UPDATED_KEY));
        SubscriptionHandling<Party> handler = mLiveQueryClient.subscribe(parseQuery);

        // Listen for when the party is updated
        handler.handleEvent(SubscriptionHandling.Event.UPDATE, (query, party) -> {
            handleCurrentlyPlayingUpdate((Song) party.getParseObject(CURRENTLY_PLAYING_KEY));
            handleUserCountUpdate(party.getNumber(USER_COUNT_KEY));
            handlePlaylistUpdate(party.getDate(PLAYLIST_LAST_UPDATED_KEY), party.getString(CACHED_PLAYLIST_KEY));
        });
    }

    public void reconnectToLiveQuery() {
        mLiveQueryClient.reconnect();
    }

    public void disconnectFromLiveQuery() {
        mLiveQueryClient.disconnect();
    }

    private void handleUserCountUpdate(Number userCount) {
        put(USER_COUNT_KEY, userCount);
    }

    private void handleCurrentlyPlayingUpdate(Song newCurrentlyPlaying) {
        if(newCurrentlyPlaying != null) {
            Song oldCurrentlyPlaying = (Song) get(CURRENTLY_PLAYING_KEY);

            try {
                newCurrentlyPlaying.fetchIfNeeded();
                if(oldCurrentlyPlaying != null)
                    oldCurrentlyPlaying.fetchIfNeeded();
            } catch (ParseException e) {
                Log.e("Party.java", "Couldn't fetch current song data");
            }

            // Check if the currently playing song changed
            if(oldCurrentlyPlaying == null || !oldCurrentlyPlaying.equals(newCurrentlyPlaying)) {
                mCurrentParty.put(CURRENTLY_PLAYING_KEY, newCurrentlyPlaying);

                // Run callbacks
                for(SaveCallback callback : mCurrentlyPlayingUpdateCallbacks) {
                    callback.done(null);
                }
            }
        }
    }

    private void handlePlaylistUpdate(Date timestamp, String newCachedPlaylist) {
        if(newCachedPlaylist != null && timestamp != null) {
            mPlaylist.updateFromCache(timestamp, newCachedPlaylist);
        }
    }


    // Current party lifecycle methods

    /**
     * Gets the user's current party if it exists
     * @param callback callback to run after the cloud function is executed
     */
    public static void getExistingParty(@Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();

        ParseCloud.callFunctionInBackground("getCurrentParty", params, (Party party, ParseException e) -> {
            if (e == null) {
                if (party == null) {
                    Log.e("Party.java", "User's party has been deleted!");
                } else {
                    // Save the created party to the singleton instance
                    mCurrentParty = party;
                    mCurrentParty.initialize();
                }
            } else {
                // Log the error if we get one
                Log.e("Party.java", "Could not get current party!", e);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * @return the party the current user is part of, null if the user is not in a party
     */
    public static Party getCurrentParty() {
        return mCurrentParty;
    }

    /**
     * Creates a new party with the current user as the admin
     * @param callback callback to run after the cloud function is executed
     */
    public static void createParty(String name, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(NAME_KEY, name);

        ParseCloud.callFunctionInBackground("createParty", params, (Party party, ParseException e) -> {
            if (e == null) {
                // Save the created party to the singleton instance
                mCurrentParty = party;
                mCurrentParty.initialize();
            } else {
                // Log the error if we get one
                Log.e("Party.java", "Could not create party!", e);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * Adds current user to an existing party
     * @param callback callback to run after the cloud function is executed
     * @param joinCode the join code of the party to join
     */
    public static void joinParty(String joinCode, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(JOIN_CODE_KEY, joinCode);

        ParseCloud.callFunctionInBackground("joinParty", params, (Party party, ParseException e) -> {
            if (e == null) {
                // Save the created party to the singleton instance
                mCurrentParty = party;
                mCurrentParty.initialize();
            } else {
                // Log the error if we get one
                Log.e("Party.java", "Could not join party!", e);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });
    }

    public static void partyDeleted() {
        mCurrentParty = null;
    }

    /***
     * Deletes the current user's party
     * @param callback callback to run after the cloud function is executed
     */
    public void deleteParty(@Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        ParseCloud.callFunctionInBackground("deleteParty", params, (ParseUser user, ParseException e) -> {
            if (e == null) {
                // Remove the current party from the singleton instance
                mCurrentParty = null;
                Log.d("Party.java", "Party deleted");
            } else {
                // Log the error if we get one
                Log.e("Party.java", "Could not delete party!", e);
            }
            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * Current user leaves the current party
     * @param callback
     */
    public void leaveParty(@Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();

        ParseCloud.callFunctionInBackground("leaveParty", params, (ParseUser user, ParseException e) -> {
            if (e == null) {
                // Remove the current party from the singleton instance
                mCurrentParty = null;
                Log.d("Party.java", "User left party");
            } else {
                Log.e("Party.java", "Could not leave party", e);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });

    }

    /**
     * @return the true if the logged in user is the admin of their party
     */
    public boolean isCurrentUserAdmin() {
        String currentUserId = ParseUser.getCurrentUser().getObjectId();
        String adminId = getParseUser(ADMIN_KEY).getObjectId();
        return currentUserId.equals(adminId);
    }

    /**
     * Get's the party's name
     * @return the name as a string
     */
    public String getName() {
        return getString(NAME_KEY);
    }

    /**
     * Gets the party's join code.  Other users can join the party through this code.
     * @return the party's join code
     */
    public String getJoinCode() {
        return getString(JOIN_CODE_KEY);
    }


    // Location methods

    /**
     * Gets a list of parties near a location
     * @param location the location near which to find parties
     * @param callback callback to run after the cloud function is executed
     */
    public static void getNearbyParties(ParseGeoPoint location, @Nullable final FunctionCallback<List<Party>> callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(LOCATION_KEY, location);
        // TODO: maybe make location a little more accurate
        params.put("maxDistance", 5); //miles lol

        ParseCloud.callFunctionInBackground("getNearbyParties", params, (List<Party> parties, ParseException e) -> {
            if (e != null) {
                // Log the error if we get one
                Log.e("Party.java", "Could not get nearby parties!", e);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(parties, e);
            }
        });
    }

    /**
     * Updates the party's location
     * @param callback callback to run after the cloud function is executed
     */
    public void updatePartyLocation(ParseGeoPoint location, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(LOCATION_KEY, location);

        ParseCloud.callFunctionInBackground("updatePartyLocation", params, (Party party, ParseException e) -> {
            if (e != null) {
                // Log the error if we get one
                Log.e("Party.java", "Could not update party location!", e);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * Sets the party's location to undefined
     * @param callback callback to run after the cloud function is executed
     */
    public void clearLocation(@Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();

        ParseCloud.callFunctionInBackground("clearPartyLocation", params, (Party party, ParseException e) -> {
            if (e != null) {
                // Log the error if we get one
                Log.e("Party.java", "Could not clear party location!", e);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * Gets the current party's location preferences
     * @return the boolean value of location enabling
     */
    public boolean getLocationEnabled() {
        return mCurrentParty.getBoolean(LOCATION_PERMISSION_KEY);
    }

    // Playlist methods

    /**
     * Gets the party's playlist.  Make sure to call updatePlaylist() before getting this value for
     * the first time otherwise it will be empty.
     * @return a list of playlist entries representing this party's playlist
     */
    public Playlist getPlaylist() {
        return mPlaylist;
    }

    /**
     * Registers a new callback that is called when the party's current song changes
     * @param callback
     */
    public void registerCurrentlyPlayingUpdateCallback(SaveCallback callback) {
        mCurrentlyPlayingUpdateCallbacks.add(callback);
    }

    /**
     * Deregisters a callback that was added to free up memory
     * @param callback
     */
    public void deregisterCurrentlyPlayingUpdateCallback(SaveCallback callback) {
        mCurrentlyPlayingUpdateCallbacks.remove(callback);
    }

    /**
     * Calls on the ParseCloud to set the specified song as currently playing.
     *
     * @param spotifyId the Spotify ID of the song to set as currently playing
     * @param callback to run after the cloud function is executed
     */
    public void setCurrentlyPlayingSong(String spotifyId, @Nullable final SaveCallback callback) {
        Log.d("Party.java", "set song");
        HashMap<String, Object> params = new HashMap<>();
        params.put(Song.SPOTIFY_ID_KEY, spotifyId);

        ParseCloud.callFunctionInBackground("setCurrentlyPlayingSong", params,
                (FunctionCallback<Song>) (song, e) -> {
            if (e != null) {
                Log.e("Party.java", "Could not set the next song");
            } else {
                mCurrentParty.put(CURRENTLY_PLAYING_KEY, song);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * Calls on the ParseCloud to delete the specified song from the playlist and set it as
     * the currently playing song.
     *
     * @param entry the entry to set as currently playing
     * @param callback to run after the cloud function is executed
     */
    public void setCurrentlyPlayingEntry(PlaylistEntry entry, @Nullable final SaveCallback callback) {
        Log.d("Party.java", "set entry");
        HashMap<String, Object> params = new HashMap<>();
        params.put(Song.SPOTIFY_ID_KEY, entry.getSong().getSpotifyId());

        ParseCloud.callFunctionInBackground("setCurrentlyPlayingEntry", params, (FunctionCallback<Song>) (song, e) -> {
            if (e != null) {
                Log.e("Party.java", "Could not set the next song");
            } else {
                mCurrentParty.put(CURRENTLY_PLAYING_KEY, song);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * Gets the party's current song.
     * @return the current song
     */
    public Song getCurrentSong() {
        return (Song) mCurrentParty.getParseObject(CURRENTLY_PLAYING_KEY);
    }

    public Number getPartyUserCount() {
        return getNumber(USER_COUNT_KEY);
    }

    // Settings methods
    /***
     * Saves all settings for the current party
     * @param locationEnabled whether location is enabled
     * @param partyName name for the party
     * @param callback callback to be called after server response
     */
    public void saveSettings(boolean locationEnabled, String partyName, int userLimit, int songLimit,
                             @Nullable final SaveCallback callback) {
        // First check if the settings have changed
        boolean hasLocationChanged = locationEnabled != mCurrentParty.getLocationEnabled();
        boolean hasNameChanged = !partyName.equals(mCurrentParty.getName());
        boolean hasUserLimitChanged = userLimit != mCurrentParty.getUserLimit() && userLimit != 0;
//        boolean hasSongLimitChanged = songLimit != mCurrentParty.getSongLimit();

        HashMap<String, Object> params = new HashMap<>();
        params.put(LOCATION_PERMISSION_KEY, hasLocationChanged ? locationEnabled : null);
        params.put(NAME_KEY, hasNameChanged ? partyName : null);
        params.put(USER_LIMIT_KEY, hasUserLimitChanged ? userLimit : null);
//        params.put(SONG_LIMIT_KEY, hasSongLimitChanged ? songLimit : null);
        params.put(SONG_LIMIT_KEY, null); // TODO - undo this change after demo
        ParseCloud.callFunctionInBackground("savePartySettings", params,
                (Party party, ParseException e) -> {
            if (e == null) {
                mCurrentParty.put(NAME_KEY, partyName);
                mCurrentParty.put(LOCATION_PERMISSION_KEY, locationEnabled);
                mCurrentParty.put(USER_LIMIT_KEY, userLimit);
                mCurrentParty.put(SONG_LIMIT_KEY, songLimit);
            } else {
                Log.e("Party.java", "Could not set location permissions!", e);
            }

            // Run the callback if it exists
            if (callback != null) {
                callback.done(e);
            }
        });
    }

    public int getUserLimit() {
        return getInt(USER_LIMIT_KEY);
    }

    /*public int getSongLimit() {
        return mCurrentParty.getInt(SONG_LIMIT_KEY);
    }*/
}
