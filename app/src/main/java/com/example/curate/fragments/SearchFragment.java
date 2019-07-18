package com.example.curate.fragments;


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

import com.example.curate.R;
import com.example.curate.adapters.SearchAdapter;
import com.example.curate.models.Song;

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

	SearchAdapter adapter;

	List<Song> songs;


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
		adapter.clear();
		loadData(searchText);
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
		adapter = new SearchAdapter(getContext(), songs);
		adapter.setListener((SearchAdapter.OnSongAddedListener) getActivity());
		// Set adapter, layout manger, and item decorations
		rvSearch.setAdapter(adapter);
		rvSearch.setLayoutManager(new LinearLayoutManager(getContext()));
//		rvSearch.addItemDecoration(new DividerItemDecoration(rvSearch.getContext(),
//				DividerItemDecoration.VERTICAL));
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
		Log.d(TAG, String.format("Searched for : %s", searchText));


		final Song.SearchQuery search = new Song.SearchQuery();
		search.setQuery(searchText).setLimit(15).find(e -> {
			if(e == null) {
				adapter.clear();
				adapter.addAll(search.getResults());
			} else {
//				Toast.makeText(getContext(), "Could not search!", Toast.LENGTH_SHORT).show();
			}
		});
	}
}
