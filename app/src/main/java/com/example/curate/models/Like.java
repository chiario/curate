package com.example.curate.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.lang.reflect.Member;

@ParseClassName("Like")
public class Like extends ComparableParseObject {
    private static final String USER_KEY = "user";
    private static final String ENTRY_KEY = "entry";

    public Like() {
        // Required empty constructor
    }

    public PlaylistEntry getEntry() {
        return (PlaylistEntry) getParseObject(ENTRY_KEY);
    }

    public ParseUser getUser() {
        return getParseUser(USER_KEY);
    }
}
