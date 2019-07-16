package com.example.curate;


import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.curate.models.Song;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 */
public class SearchFragment extends Fragment {

	private static final String TAG = "SearchFragment";

	private static final String KEY_SEARCH = "search";

	@BindView(R.id.rvSearch) RecyclerView rvSearch;

	String searchText;

	SongAdapter adapter;

	List<Song> songs;

	OnSongAddedListener listener;

	interface OnSongAddedListener {
		public void onSongAdded(Song song);
	}

	public SearchFragment() {
		// Required empty public constructor
	}

	/***
	 * Create a new instance of the SearchFragment
	 * @return The new instance created
	 */
	public static SearchFragment newInstance() {
//		Bundle args = new Bundle();
		SearchFragment fragment = new SearchFragment();
//		fragment.setArguments(args);
		fragment.setRetainInstance(true);
		return fragment;
	}

	/***
	 * Set search text and search for the given string
	 * @param searchText String to search for
	 */
	public void setSearchText(String searchText) {
		this.searchText = searchText;
		Log.d(TAG, this.searchText);
		loadData(searchText);
	}

	/***
	 * Attach the OnSongAddedListener when onAttach is called with correct context
	 * @param context
	 */
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if(context instanceof OnSongAddedListener)
			listener = (OnSongAddedListener) context;
	}

	/***
	 * Inflate the proper layout
	 * @param inflater
	 * @param container
	 * @param savedInstanceState
	 * @return
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_search, container, false);
	}

	/***
	 * Set up the adapter and recycler view when the fragment view is created.
	 * @param view
	 * @param savedInstanceState
	 */
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ButterKnife.bind(this, view);

		// Recover saved state if applicable
		if(savedInstanceState != null)
			searchText = savedInstanceState.getString(KEY_SEARCH);
		songs = new ArrayList<Song>();

		// Create the adapter, along with onClick listener for the "add" button
		adapter = new SongAdapter(getContext(), songs, new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int i = rvSearch.getChildAdapterPosition((View) view.getParent());
				Song song = songs.get(i);
				listener.onSongAdded(song);
				songs.remove(i);
				adapter.notifyItemRemoved(i);
				Toast.makeText(getContext(), "Song Added!", Toast.LENGTH_SHORT).show();
			}
		});

		// Set adapter, layout manger, and item decorations
		rvSearch.setAdapter(adapter);
		rvSearch.setLayoutManager(new LinearLayoutManager(getContext()));
		rvSearch.addItemDecoration(new DividerItemDecoration(rvSearch.getContext(),
				DividerItemDecoration.VERTICAL));
	}

	/***
	 * Save the current search text so it persists through configuration changes
	 * @param outState
	 */
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_SEARCH, searchText);
	}

	/***
	 * Load the search results
	 * @param searchText String to search for
	 */
	public void loadData(String searchText) {
		// Todo search spotify API
		Log.d(TAG, String.format("Searched for : %s", searchText));

		// For testing, simply loads all songs in "Song" class
		ParseQuery<Song> query = ParseQuery.getQuery(Song.class);
		query.findInBackground(new FindCallback<Song>() {
			@Override
			public void done(List<Song> objects, ParseException e) {
				if(e == null) {
					adapter.addAll(objects);
				}
				else {
					e.printStackTrace();
				}
			}
		});
	}
}
