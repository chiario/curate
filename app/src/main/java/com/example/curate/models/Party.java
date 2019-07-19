package com.example.curate.models;

import android.util.Log;

import androidx.annotation.Nullable;

import com.parse.FunctionCallback;
import com.parse.ParseClassName;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@ParseClassName("Party")
public class Party extends ParseObject {
    private static final String ADMIN_KEY = "admin";
    private static final String CURRENTLY_PLAYING_KEY = "curPlaying";
    private static final String PLAYLIST_LAST_UPDATED_KEY = "playlistLastUpdatedAt";

    private static Party mCurrentParty;
    private static Song mCurrentSong;

    private List<PlaylistEntry> mPlaylist;

    public Party() {
        mPlaylist = new ArrayList<>();
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
                mCurrentSong = song;
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
     * Gets the party's current song.
     * @return the current song
     */
    public Song getCurrentSong() {
        return mCurrentSong;
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
     * Gets the party's playlist.  Make sure to call updatePlaylist() before getting this value for
     * the first time otherwise it will be empty.
     * @return a list of playlist entries representing this party's playlist
     */
    public List<PlaylistEntry> getPlaylist() {
        return mPlaylist;
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

    /***
     * Deletes the current user's party
     * TODO make sure to check this when there are clients vs admins
     * @param callback callback to run after the cloud function is executed
     */
    public static void deleteParty(@Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        ParseCloud.callFunctionInBackground("deleteParty", params, (ParseUser user, ParseException e) -> {
            if (e == null) {
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
     * Creates a new party with the current user as the admin
     * @param callback callback to run after the cloud function is executed
     */
    public static void createParty(@Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();

        ParseCloud.callFunctionInBackground("createParty", params, (Party party, ParseException e) -> {
            if (e == null) {
                // Save the created party to the singleton instance
                mCurrentParty = party;
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
     * Adds current user to an existing party
     * @param callback callback to run after the cloud function is executed
     * @param partyId the objectId of the party to join
     */
    public static void joinParty(String partyId, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("partyId", partyId);

        ParseCloud.callFunctionInBackground("joinParty", params, (Party party, ParseException e) -> {
            if (e == null) {
                // Save the created party to the singleton instance
                mCurrentParty = party;
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

    /** Current user leaves the current party
     *
     * @param callback
     */

    public static void leaveParty(@Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();

        ParseCloud.callFunctionInBackground("leaveParty", params, (ParseUser user, ParseException e) -> {
            if (e == null) {
                Log.d("Party.java", "User left party");
                mCurrentParty = null;
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
     * Get the party that the user is currently part of, null if the party does not exist
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
     * Creates a new party with the current user as the admin
     * @param callback callback to run after the cloud function is executed
     */
    public static void destroyParty(@Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();

        ParseCloud.callFunctionInBackground("destroyParty", params, (Party party, ParseException e) -> {
            if (e == null) {
                // Remove the current party from the singleton instance
                mCurrentParty = null;
            } else {
                // Log the error if we get one
                Log.e("Party.java", "Could not destroy party!", e);
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
     * Gets the party's admin
     *
     * @return the admin as a ParseUser
     */
    public ParseUser getAdmin() {
        return mCurrentParty.getParseUser("admin"); //TODO - move this to Cloud??
    }
}
