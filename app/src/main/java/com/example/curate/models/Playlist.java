package com.example.curate.models;

import android.util.Log;

import androidx.annotation.Nullable;

import com.parse.ParseCloud;
import com.parse.ParseDecoder;
import com.parse.ParseException;
import com.parse.SaveCallback;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Playlist {
    private static final String ENTRY_ID = "entryId";

    private List<PlaylistEntry> mEntries;
    private List<Like> mLikes;
    private String mPrevCachedValue;
    private final Object mEntryMutex = new Object();

    public Playlist(String cachedPlaylist) {
        mLikes = updateLikes();
        mEntries = new ArrayList<>();
        updateFromCache(cachedPlaylist);
    }

    /***
     * Checks if a song is already added to the playlist
     * @param song song to check if added
     * @return true if song is added, false if not
     */
    public boolean contains(Song song) {
        synchronized (mEntryMutex) {
            for(PlaylistEntry entry : mEntries) {
                if(entry.getSong().equals(song)) return true;
            }
            return false;
        }
    }

    /**
     * Likes a song in the party's playlist
     * @param entry the entry in the playlist to like
     * @param callback callback to run after the cloud function is executed
     */
    public void likeEntry(PlaylistEntry entry, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(ENTRY_ID, entry.getObjectId());

        ParseCloud.callFunctionInBackground("likeSong", params, (Like like, ParseException e) -> {
            if (e == null) {
                mLikes.add(like);
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
     * @param entry the entry in the playlist to unlike
     * @param callback callback to run after the cloud function is executed
     */
    public void unlikeEntry(PlaylistEntry entry, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(ENTRY_ID, entry.getObjectId());

        ParseCloud.callFunctionInBackground("unlikeSong", params, (Like like, ParseException e) -> {
            if (e == null) {
                mLikes.remove(like);
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
     * Adds a song to the party's playlist
     * @param song the song to add to the playlist
     * @param callback callback to run after the cloud function is executed
     */
    public void addEntry(Song song, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(Song.SPOTIFY_ID_KEY, song.getSpotifyId());
        params.put(Song.TITLE_KEY, song.getTitle());
        params.put(Song.ARTIST_KEY, song.getArtist());
        params.put(Song.ALBUM_KEY, song.getAlbum());
        params.put(Song.IMAGE_URL_KEY, song.getImageUrl());

        ParseCloud.callFunctionInBackground("addSong", params, (List<PlaylistEntry> playlist, ParseException e) -> {
            synchronized (mEntryMutex) {
                if (e == null) {
                    mEntries = playlist;
                } else {
                    // Log the error if we get one
                    Log.e("Party.java", "Could not add song!", e);
                }

                // Run the callback if it exists
                if (callback != null) {
                    callback.done(e);
                }
            }
        });
    }

    /**
     * Removes a song from the party's playlist
     * @param entry the entry to remove from the playlist
     * @param callback callback to run after the cloud function is executed
     */
    public void removeEntry(PlaylistEntry entry, @Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(ENTRY_ID, entry.getObjectId());

        ParseCloud.callFunctionInBackground("removeSong", params, (List<PlaylistEntry> playlist, ParseException e) -> {
            synchronized (mEntryMutex) {
                if (e == null) {
                    mEntries = playlist;
                } else {
                    // Log the error if we get one
                    Log.e("Party.java", "Could not remove song!", e);
                }

                // Run the callback if it exists
                if (callback != null) {
                    callback.done(e);
                }
            }
        });
    }

    public List<PlaylistEntry> getEntries() {
        return mEntries;
    }

    /**
     * Updates the party's playlist from the parse server.  The updated playlist can be accessed by
     * calling getEntries() in the callback.  This method should not be called unless it is
     * absolutely necessary!
     * @param callback callback to run after the cloud function is executed
     */
    @Deprecated
    public void update(@Nullable final SaveCallback callback) {
        HashMap<String, Object> params = new HashMap<>();

        ParseCloud.callFunctionInBackground("getPlaylist", params, (List<PlaylistEntry> playlist, ParseException e) -> {
            synchronized (mEntryMutex) {
                if (e == null) {
                    mEntries = playlist;
                } else {
                    // Log the error if we get one
                    Log.e("Party.java", "Could not get playlist!", e);
                }

                // Run the callback if it exists
                if (callback != null) {
                    callback.done(e);
                }
            }
        });
    }

    /**
     * Updates the party's likes from the parse server.  This method is called synchronously since
     * it is called in Party.initialize which is run in a separate thread.
     */
    private List<Like> updateLikes() {
        HashMap<String, Object> params = new HashMap<>();

        try {
            return ParseCloud.callFunction("getLikes", params);
        } catch (ParseException e) {
            Log.e("Playlist.java", "Couldn't get likes", e);
            return new ArrayList<>();
        }
    }

    public void updateFromCache(String cachedPlaylist) {
        synchronized (mEntryMutex) {
            // If the cache hasn't changed don't update the playlist
            if(mPrevCachedValue != null && mPrevCachedValue.equals(cachedPlaylist)) {
                return;
            }

            try {
                JSONArray playlistJson = new JSONArray(cachedPlaylist);
                mEntries = new ArrayList<>();

                for (int i = 0; i < playlistJson.length(); i++) {
                    // Create entry object from JSON
                    PlaylistEntry entry = PlaylistEntry.fromJSON(playlistJson.getJSONObject(i),
                            PlaylistEntry.class.getSimpleName(), ParseDecoder.get());

                    entry.setIsLikedByUser(isEntryLiked(entry));
                    mEntries.add(entry);
                }

                mPrevCachedValue = cachedPlaylist;
            } catch (JSONException e) {
                Log.e("Playlist.java", "Couldn't parse cached playlist", e);
            }
        }
    }

    private boolean isEntryLiked(PlaylistEntry entry) {
        for(Like like : mLikes) {
            if(like.getEntry().equals(entry)) return true;
        }
        return false;
    }

    public boolean isEmpty() {
        synchronized (mEntryMutex) {
            return mEntries.isEmpty();
        }
    }

}
