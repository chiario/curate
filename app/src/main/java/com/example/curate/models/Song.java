package com.example.curate.models;

import com.parse.Parse;
import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;

@ParseClassName("Song")
public class Song extends ParseObject {

	private static final String TITLE_KEY = "title";
	private static final String ARTIST_KEY = "artist";
	private static final String IMAGE_URL_KEY = "imageUrl";

	public Song() {
		// Required empty constructor
	}

	public String getTitle() {
		return "The Awesome Song";
//		return getString(TITLE_KEY);
	}

	public String getArtist() {
		return "The Artist";
//		return getString(ARTIST_KEY);
	}

	public String getImageUrl() {
		return getString(IMAGE_URL_KEY);
	}

	public void setTitle(String title) {
		put(TITLE_KEY, title);
	}

	public void setArtist(String artist) {
		put(ARTIST_KEY, artist);
	}

	public void setImageUrl(String imageUrl) {
		put(IMAGE_URL_KEY, imageUrl);
	}

//	public static class Query extends ParseQuery<Song> {
//		public Query() {
//			super(Song.class);
//		}
//
//		public Query
//	}

}
