package com.example.curate.models;

import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.versionedparcelable.ParcelField;

import com.parse.ParseClassName;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import org.parceler.Parcel;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@ParseClassName("Song")
public class Song extends ComparableParseObject {

	public static final String SPOTIFY_ID_KEY = "spotifyId";
	public static final String TITLE_KEY = "title";
	public static final String ARTIST_KEY = "artist";
	public static final String ALBUM_KEY = "album";
	public static final String IMAGE_URL_KEY = "artUrl";

	public Song() {
		// Required empty constructor
	}

	public Song(ParcelableSong song) {
		put(SPOTIFY_ID_KEY, song.spotifyId);
		put(TITLE_KEY, song.title);
		put(ARTIST_KEY, song.artist);
		put(ALBUM_KEY, song.album);
		put(IMAGE_URL_KEY, song.artUrl);
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

	public String getImageUrl() {
		return getString(IMAGE_URL_KEY);
	}

	public static Parcelable createParcelFromSongs(List<Song> songs) {
		List<ParcelableSong> parcelableSongs = new ArrayList<>();

		for(Song song : songs) {
			parcelableSongs.add(new ParcelableSong(song));
		}

		return Parcels.wrap(parcelableSongs);
	}

	public static List<Song> retrieveSongsFromParcel(Parcelable parcel) {
		List<ParcelableSong> parcelableSongs = Parcels.unwrap(parcel);
		List<Song> songs = new ArrayList<>();

		for(ParcelableSong song : parcelableSongs) {
			songs.add(new Song(song));
		}

		return songs;
	}

	@Parcel
	protected static class ParcelableSong {
		String spotifyId;
		String title;
		String artist;
		String album;
		String artUrl;

		public ParcelableSong() {}

		public ParcelableSong(Song song) {
			spotifyId = song.getSpotifyId();
			title = song.getTitle();
			artist = song.getArtist();
			album = song.getAlbum();
			artUrl = song.getImageUrl();
		}
	}

	/**
	 * This class represents a executeSearch query that can be sent to the Parse server which will then
	 * call the Spotify API and return a list of song results
	 */
	public static class SearchQuery {
		private static int DEFAULT_NUM_RESULTS = 20;

		private List<Song> mResults;
		private String mQuery;
		private int mNumResults;
		private boolean mIsLiveSearch;

		public SearchQuery() {
			mNumResults = DEFAULT_NUM_RESULTS;
			mQuery = "";
			mIsLiveSearch = false;
			mResults = new ArrayList<>();
		}

		/**
		 * Set the number of results the executeSearch should return.  Defaults to 20 if nothing is set.
		 * @param numResults the number of results to return
		 */
		public SearchQuery setLimit(int numResults) {
			mNumResults = numResults;
			return this;
		}

		/**
		 * Sets the search query.  Does not have to be URL encoded.
		 * @param query the query to executeSearch for
		 */
		public SearchQuery setQuery(String query) {
			mQuery = query;
			return this;
		}

		/**
		 * @return the search query
		 */
		public String getQuery() {
			return mQuery;
		}

		/**
		 * Sets if the search is a live search or not.  If the search is live, it will use the cache
		 * on the server to speed up results.
		 * @param isLiveSearch the query to executeSearch for
		 */
		public SearchQuery setIsLive(boolean isLiveSearch) {
			mIsLiveSearch = isLiveSearch;
			return this;
		}

		/**
		 * Gets the executeSearch results.  Should only be called in the callback of the find function
		 * otherwise will not be populated
		 * @return a list of songs that matches the executeSearch query
		 */
		public List<Song> getResults() {
			return mResults;
		}

		/**
		 * Executes the executeSearch query.
		 * @param callback an optional callback to run after the executeSearch has executed.
		 */
		public void find(@Nullable SaveCallback callback) {
			HashMap<String, Object> params = new HashMap<>();
			params.put("query", mQuery);
			params.put("limit", mNumResults);
			params.put("useCache", mIsLiveSearch);

			// Default to an empty result list if the query is empty
			if(mQuery.isEmpty()) {
				// Run the callback if it exists
				if(callback != null) {
					callback.done(null);
				}
				return;
			}

			ParseCloud.callFunctionInBackground("search", params, (List<Song> results, ParseException e) -> {
				if (e == null) {
					// Save the executeSearch results to this object so they can be accessed through getResults()
					mResults = results;
				} else {
					// Log the error if we get one
					Log.e("Song.SearchQuery", "SearchQuery failed: ", e);
				}

				// Run the callback if it exists
				if(callback != null) {
					callback.done(e);
				}
			});
		}

	}
}
