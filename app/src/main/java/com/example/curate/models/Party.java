package com.example.curate.models;

import android.util.Log;

import androidx.annotation.Nullable;

import com.parse.FunctionCallback;
import com.parse.ParseClassName;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.livequery.ParseLiveQueryClient;
import com.parse.livequery.SubscriptionHandling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@ParseClassName("Party")
public class Party extends ParseObject {
    private static final String ADMIN_KEY = "admin";
    private static final String JOIN_CODE_KEY = "joinCode";
    private static final String CURRENTLY_PLAYING_KEY = "currPlaying";
    private static final String NAME_KEY = "name";
    private static final String LOCATION_KEY = "location";
    private static final String LOCATION_PERMISSION_KEY = "locationEnabled";

    private static Party mCurrentParty;
    private List<PlaylistEntry> mPlaylist;
    private List<SaveCallback> mPlaylistUpdateCallbacks;

    public Party() {}


    /**
     * Initializes the party object and sets up live queries
     */
    private void initialize() {
        mPlaylist = new ArrayList<>();
        mPlaylistUpdateCallbacks = new ArrayList<>();

        // Set up live query
        ParseLiveQueryClient parseLiveQueryClient = ParseLiveQueryClient.Factory.getClient();
        ParseQuery<Party> parseQuery = ParseQuery.getQuery(Party.class);
        parseQuery.include(CURRENTLY_PLAYING_KEY);
        parseQuery.whereEqualTo("objectId", getObjectId());
        SubscriptionHandling<Party> handler = parseLiveQueryClient.subscribe(parseQuery);

        // Listen for when the party is updated
        handler.handleEvent(SubscriptionHandling.Event.UPDATE, (query, party) -> {
            Song currentlyPlaying = (Song) party.getParseObject(CURRENTLY_PLAYING_KEY);
            if(currentlyPlaying != null) {
                mCurrentParty.put(CURRENTLY_PLAYING_KEY, currentlyPlaying);
            }
            updatePlaylist(e -> {
                for(SaveCallback callback : mPlaylistUpdateCallbacks) {
                    callback.done(e);
                }
            });
        });

    }

    /**
     * Adds a song to the party's playlist
     * @param song the song to add to the playlist
     * @param callback callback to run after the cloud function is executed
     */
    public void addSong(Song song, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(Song.SPOTIFY_ID_KEY, song.getSpotifyId());
        params.put(Song.TITLE_KEY, song.getTitle());
        params.put(Song.ARTIST_KEY, song.getArtist());
        params.put(Song.ALBUM_KEY, song.getAlbum());
        params.put(Song.IMAGE_URL_KEY, song.getImageUrl());

        ParseCloud.callFunctionInBackground("addSong", params, (List<PlaylistEntry> playlist, ParseException e) -> {
            if (e == null) {
                // Preserve the playlist object so that it can be used in an adapter
                mPlaylist.clear();
                mPlaylist.addAll(playlist);
            } else {
                // Log the error if we get one
                Log.e("Party.java", "Could not add song!", e);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * Removes a song from the party's playlist
     * @param song the song to remove from the playlist
     * @param callback callback to run after the cloud function is executed
     */
    public void removeSong(Song song, @Nullable final SaveCallback callback) {
        removeSong(song.getSpotifyId(), callback);
    }

    /**
     * Removes a song from the party's playlist
     * @param spotifyId the spotifyId of the song to remove from the playlist
     * @param callback callback to run after the cloud function is executed
     */
    public void removeSong(String spotifyId, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(Song.SPOTIFY_ID_KEY, spotifyId);

        ParseCloud.callFunctionInBackground("removeSong", params, (List<PlaylistEntry> playlist, ParseException e) -> {
            if (e == null) {
                // Preserve the playlist object so that it can be used in an adapter
                mPlaylist.clear();
                mPlaylist.addAll(playlist);
            } else {
                // Log the error if we get one
                Log.e("Party.java", "Could not remove song!", e);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * Calls on the ParseCloud to remove the next song in the party's playlist and return it
     * @param callback callback to run after the cloud function is executed
     * @return the party's currently playing song
     */
    public void getNextSong(@Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();

        ParseCloud.callFunctionInBackground("getNextSong", params, (FunctionCallback<Song>) (song, e) -> {
            if (e == null) {
                Log.d("Party.java", "got song " + song.getTitle());
                mCurrentParty.put(CURRENTLY_PLAYING_KEY, (Song) Objects.requireNonNull(mCurrentParty.getParseObject(CURRENTLY_PLAYING_KEY)));
            } else {
                Log.e("Party.java", "Could not get the next song");
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * Calls on the ParseCloud to remove the specified song from the playlist and set it as
     * currently playing instead.
     * @param spotifyId the Spotify ID of the song to set as currently playing
     * @param callback callback to run after the cloud function is executed
     * @return the party's currently playing song
     */
    public void setCurrentlyPlaying(String spotifyId, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(Song.SPOTIFY_ID_KEY, spotifyId);

        ParseCloud.callFunctionInBackground("setCurrentlyPlaying", params, (FunctionCallback<Song>) (song, e) -> {
            if (e == null) {
                Log.d("Party.java", "got song " + song.getTitle());
                mCurrentParty.put(CURRENTLY_PLAYING_KEY, (Song) Objects.requireNonNull(mCurrentParty.getParseObject(CURRENTLY_PLAYING_KEY)));
            } else {
                Log.e("Party.java", "Could not set the next song");
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

    /**
     * Get's the party's name
     * @return the name as a string
     */
    public String getName() {
        return getString(NAME_KEY);
    }

    /**
     * Sets the current party name
     * @param name the new name for the party
     * @param callback callback to run after the cloud function is executed
     */
    public static void setPartyName(String name, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(NAME_KEY, name);

        ParseCloud.callFunctionInBackground("setPartyName", params, (Party party, ParseException e) -> {
            if (e == null) {
                mCurrentParty = party;
            } else {
                // Log the error if we get one
                Log.e("Party.java", "Could not change party name!", e);
            }

            // Run the callback if it exists
            if (callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * Likes a song in the party's playlist
     * @param spotifyId the spotifyId of the song to like
     * @param callback callback to run after the cloud function is executed
     */
    public void likeSong(String spotifyId, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(Song.SPOTIFY_ID_KEY, spotifyId);

        ParseCloud.callFunctionInBackground("likeSong", params, (List<PlaylistEntry> playlist, ParseException e) -> {
            if (e == null) {
                // Preserve the playlist object so that it can be used in an adapter
                mPlaylist.clear();
                mPlaylist.addAll(playlist);
            } else {
                // Log the error if we get one
                Log.e("Party.java", "Could not like song!", e);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * Unlikes a song in the party's playlist
     * @param spotifyId the spotifyId of the song to unlike
     * @param callback callback to run after the cloud function is executed
     */
    public void unlikeSong(String spotifyId, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(Song.SPOTIFY_ID_KEY, spotifyId);

        ParseCloud.callFunctionInBackground("unlikeSong", params, (List<PlaylistEntry> playlist, ParseException e) -> {
            if (e == null) {
                // Preserve the playlist object so that it can be used in an adapter
                mPlaylist.clear();
                mPlaylist.addAll(playlist);
            } else {
                // Log the error if we get one
                Log.e("Party.java", "Could not unlike song!", e);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * Updates the party's playlist.  The updated playlist can be accessed by calling getPlaylist()
     * in the callback
     * @param callback callback to run after the cloud function is executed
     */
    public void updatePlaylist(@Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();

        ParseCloud.callFunctionInBackground("getPlaylist", params, (List<PlaylistEntry> playlist, ParseException e) -> {
            if (e == null) {
                // Preserve the playlist object so that it can be used in an adapter
                mPlaylist.clear();
                mPlaylist.addAll(playlist);
            } else {
                // Log the error if we get one
                Log.e("Party.java", "Could not get playlist!", e);
            }

            // Run the callback if it exists
            if(callback != null) {
                callback.done(e);
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
    public static void clearLocation(@Nullable final SaveCallback callback) {
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
     * Gets the party's playlist.  Make sure to call updatePlaylist() before getting this value for
     * the first time otherwise it will be empty.
     * @return a list of playlist entries representing this party's playlist
     */
    public List<PlaylistEntry> getPlaylist() {
        return mPlaylist;
    }

    /**
     * Gets the party's join code.  Other users can join the party through this code.
     * @return the party's join code
     */
    public String getJoinCode() {
        return getString(JOIN_CODE_KEY);
    }

    /**
     * Registers a new callback that is called when the party's playlist changes
     * @param callback
     */
    public void registerPlaylistUpdateCallback(SaveCallback callback) {
        mPlaylistUpdateCallbacks.add(callback);
    }

    /**
     * Deregisters a callback that was added to free up memory
     * @param callback
     */
    public void deregisterPlaylistUpdateCallback(SaveCallback callback) {
        mPlaylistUpdateCallbacks.remove(callback);
    }

    /***
     * Checks if a song is already added to the playlist
     * @param song song to check if added
     * @return true if song is added, false if not
     */
    public boolean contains(Song song) {
        // TODO: Improve efficiency
        for(int i = 0; i < mPlaylist.size(); i++) {
            if(mPlaylist.get(i).getSong().getSpotifyId().equals(song.getSpotifyId()))
                return true;
        }
        return false;
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
            }
            else {
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
     * Creates a new party with the current user as the admin
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
     * Sets the current party's location preferences
     * @param permissions boolean value to set location enabling to
     * @param callback callback to run after the cloud function is executed
     */
    public static void setLocationEnabled(boolean permissions, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(LOCATION_PERMISSION_KEY, permissions);

        ParseCloud.callFunctionInBackground("setLocationEnabled", params, (Party party, ParseException e) -> {
            if (e == null) {
                mCurrentParty = party;
            } else {
                Log.e("Party.java", "Could not set location permissions!", e);
            }

            // Run the callback if it exists
            if (callback != null) {
                callback.done(e);
            }
        });
    }

    /**
     * Gets the current party's location preferences
     * @return the boolean value of location enabling
     */
    public static boolean getLocationEnabled() {
        return mCurrentParty.getBoolean(LOCATION_PERMISSION_KEY);
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

    /**
     * Current user leaves the current party
     * @param callback
     */
    public static void leaveParty(@Nullable final SaveCallback callback) {
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

    /***
     * Deletes the current user's party
     * @param callback callback to run after the cloud function is executed
     */
    public static void deleteParty(@Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        ParseCloud.callFunctionInBackground("deleteParty", params, (ParseUser user, ParseException e) -> {
            if (e == null) {
                // Remove the current party from the singleton instance
                mCurrentParty = null;
                Log.d("Party.java", "Party deleted");
            }
            else {
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
     * @return the party the current user is part of, null if the user is not in a party
     */
    public static Party getCurrentParty() {
        return mCurrentParty;
    }

}
