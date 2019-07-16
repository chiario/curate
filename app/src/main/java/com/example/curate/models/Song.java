package com.example.curate.models;

import com.parse.ParseClassName;
import com.parse.ParseObject;

@ParseClassName("Song")
public class Song extends ParseObject {

	protected static final String SPOTIFY_ID_KEY = "spotifyId";
	protected static final String TITLE_KEY = "title";
	protected static final String ARTIST_KEY = "artist";
	protected static final String ALBUM_KEY = "album";
	protected static final String IMAGE_URL_KEY = "artUrl";
	private boolean isSelected;

	public Song() {
		// Required empty constructor
	}

	public String getSpotifyId() {
		return getString(SPOTIFY_ID_KEY);
	}

	public String getTitle() {
		return getString(TITLE_KEY);
	}

	public String getArtist() {
		return getString(ARTIST_KEY);
	}

	public String getAlbum() {
		return getString(ALBUM_KEY);
	}

	public boolean isSelected() {
		return isSelected;
	}

	public String getImageUrl() {
		return getString(IMAGE_URL_KEY);
	}

	public void setSpotifyId(String spotifyId) {
		put(SPOTIFY_ID_KEY, spotifyId);
	}

	public void setTitle(String title) {
		put(TITLE_KEY, title);
	}

	public void setArtist(String artist) {
		put(ARTIST_KEY, artist);
	}

	public void setAlbum(String album) {
		put(ALBUM_KEY, album);
	}

	public void setImageUrl(String imageUrl) {
		put(IMAGE_URL_KEY, imageUrl);
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}
}
