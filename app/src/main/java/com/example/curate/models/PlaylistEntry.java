package com.example.curate.models;

import android.util.Log;

import androidx.annotation.Nullable;

import com.parse.ParseClassName;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.util.HashMap;

@ParseClassName("PlaylistEntry")
public class PlaylistEntry extends ParseObject {
    private static final String SONG_KEY = "song";
    private static final String SCORE_KEY = "score";
    private static final String PARTY_KEY = "party";

    public PlaylistEntry() {
        // Required empty constructor
    }

    public Song getSong() {
        return (Song) getParseObject(SONG_KEY);
    }

    public Number getScore() {
        return getNumber(SCORE_KEY);
    }

}
