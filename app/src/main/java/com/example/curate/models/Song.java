package com.example.curate.models;

import android.util.Log;

import androidx.annotation.Nullable;

import com.parse.FunctionCallback;
import com.parse.ParseClassName;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

	public static class Search {
		private static int DEFAULT_NUM_RESULTS = 20;

		private List<Song> mResults;
		private String mQuery;
		private int mNumResults;

		public Search() {
			mNumResults = DEFAULT_NUM_RESULTS;
			mQuery = "";
			mResults = new ArrayList<>();
		}

		public Search setLimit(int numResults) {
			mNumResults = numResults;
			return this;
		}

		public Search setQuery(String query) {
			mQuery = query;
			return this;
		}

		public List<Song> getResults() {
			return mResults;
		}

		public void execute(@Nullable SaveCallback callback) {
			HashMap<String, Object> params = new HashMap<>();
			params.put("query", mQuery);
			params.put("limit", mNumResults);

			ParseCloud.callFunctionInBackground("search", params, (List<Song> results, ParseException e) -> {
				if (e == null) {
					// Save the search results to this object so they can be accessed through getResults()
					mResults = results;
				} else {
					// Log the error if we get one
					Log.e("Song.Search", "Search failed: ", e);
				}

				// Run the callback if it exists
				if(callback != null) {
					callback.done(e);
				}
			});
		}
	}
}
