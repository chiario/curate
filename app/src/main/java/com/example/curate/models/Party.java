package com.example.curate.models;

import android.util.Log;

import androidx.annotation.Nullable;

import com.parse.ParseClassName;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.util.HashMap;

@ParseClassName("Party")
public class Party extends ParseObject {
    private static final String ADMIN_KEY = "admin";
    private static final String CURRENTLY_PLAYING_KEY = "curPlaying";
    private static final String PLAYLIST_LAST_UPDATED_KEY = "playlistLastUpdatedAt";

    private static Party mCurrentParty;

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

        ParseCloud.callFunctionInBackground("addSong", params, (Song s, ParseException e) -> {
            if (e != null) {
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

        ParseCloud.callFunctionInBackground("removeSong", params, (Song s, ParseException e) -> {
            if (e != null) {
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
     * Creates a new party with the current user as the admin
     * @param callback callback to run after the cloud function is executed
     */
    public static void createParty(@Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();

        ParseCloud.callFunctionInBackground("createParty", params, (Party party, ParseException e) -> {
            if (e == null) {
                // Save the created party to the singleton instance
                mCurrentParty = party;
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
}
