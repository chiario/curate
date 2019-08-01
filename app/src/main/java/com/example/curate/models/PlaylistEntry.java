package com.example.curate.models;

import android.util.Log;

import androidx.annotation.Nullable;

import com.parse.ParseClassName;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

@ParseClassName("PlaylistEntry")
public class PlaylistEntry extends ComparableParseObject {
    private static final String SONG_KEY = "song";
    private static final String SCORE_KEY = "score";
    private static final String ADDED_BY_KEY = "addedBy";
    private static final String LIKED_KEY = "isLikedByUser";
    private static final String PARTY_KEY = "party";

    private boolean isLikedByUser = false;

    public PlaylistEntry() {
        // Required empty constructor
    }

    public PlaylistEntry(JSONObject json) {
        try {
            setObjectId(json.getString("objectId"));
            put(ADDED_BY_KEY, json.getString(ADDED_BY_KEY));
            put(SONG_KEY, new Song(json.getJSONObject(SONG_KEY)));
        } catch(JSONException e) {
            Log.e("big sad", "error: ", e);
        }
    }

    public Song getSong() {
        return (Song) getParseObject(SONG_KEY);
    }

    public Number getScore() {
        return getNumber(SCORE_KEY);
    }

    public boolean isLikedByUser() {
        return isLikedByUser;
    }

    public void setIsLikedByUser(boolean isLikedByUser) {
        this.isLikedByUser = isLikedByUser;
    }

    public boolean contentsEqual(PlaylistEntry other) {
        return getSong().equals(other.getSong()) && isLikedByUser == other.isLikedByUser
                && getAddedBy().equals(other.getAddedBy()) && getScore().equals(other.getScore());
    }

    public String getAddedBy() {
        return getString(ADDED_BY_KEY);
    }
}
