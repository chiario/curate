package com.example.curate.fragments;


import android.graphics.Canvas;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

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
	@BindView(R.id.progressBar) ProgressBar progressBar;
	@BindView(R.id.clText) ConstraintLayout clText;
	@BindView(R.id.tvError) TextView tvError;

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
	 * Execute a search for the given string
	 * @param searchText the string to search for
	 */
	public void executeSearch(String searchText) {
		progressBar.setVisibility(View.VISIBLE);
		adapter.clear();
		hideText();
		loadData(searchText);
	}

	public void newSearch() {
		progressBar.setVisibility(View.INVISIBLE);
		adapter.clear();
		showText(getString(R.string.new_search));
	}

	@Override
	public void onStart() {
		super.onStart();
		newSearch();
	}

	@Override
	public void onStop() {
		super.onStop();
		adapter.clear();
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
	 * Load the search results
	 * @param searchText String to search for
	 */
	public void loadData(String searchText) {
		Log.d(TAG, String.format("Searched for : %s", searchText));
		final Song.SearchQuery search = new Song.SearchQuery();
		search.setQuery(searchText).setLimit(15).find(e -> {
			progressBar.setVisibility(View.GONE);
			if(e == null) {
				adapter.clear();
				adapter.addAll(search.getResults());
			} else {
				if(e.getMessage().startsWith("400"))
					showText(getString(R.string.no_search_result));
				else
					showText(getString(R.string.search_error));
//				Toast.makeText(getContext(), "Could not search!", Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void showText(String text) {
		clText.setVisibility(View.VISIBLE);
		tvError.setText(text);
	}

	private void hideText() {
		clText.setVisibility(View.GONE);
	}
}
