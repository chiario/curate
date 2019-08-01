package com.example.curate.fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.curate.R;
import com.example.curate.activities.MainActivity;
import com.example.curate.adapters.ItemTouchHelperCallbacks;
import com.example.curate.adapters.SearchAdapter;
import com.example.curate.models.Song;
import com.example.curate.utils.LiveSearchManager;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 */
public class SearchFragment extends Fragment {

	private static final String TAG = "SearchFragment";

	@BindView(R.id.rvSearch) RecyclerView rvSearch;
	@BindView(R.id.progressBar) ProgressBar progressBar;
	@BindView(R.id.textContainer) LinearLayout textContainer;
	@BindView(R.id.tvError) TextView tvError;
	@BindView(R.id.tvDescription) TextView tvDescription;

	SearchAdapter mAdapter;
	LiveSearchManager mLiveSearchManager;

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
	 * @param query the string to search for
	 */
	public void executeSearch(String query) {
		if(!mLiveSearchManager.isSearchComplete()) {
			mLiveSearchManager.cancelPendingSearches();
			progressBar.setVisibility(View.VISIBLE);
			mAdapter.clear();
			hideText();
			loadData(query);
		}
	}

	public void updateLiveSearch(String query) {
		hideText();
		mLiveSearchManager.updateQuery(query);
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
		View view = inflater.inflate(R.layout.fragment_search, container, false);

		return view;
	}

	/***
	 * Set up the mAdapter and recycler view when the fragment view is created.
	 * @param view
	 * @param savedInstanceState
	 */
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ButterKnife.bind(this, view);

		// Create the mAdapter, along with onClick listener for the "add" button
		mAdapter = new SearchAdapter(getContext());

		// Attach Swipe listeners
		ItemTouchHelperCallbacks callbacks = new ItemTouchHelperCallbacks(mAdapter, getContext());
		new ItemTouchHelper(callbacks.addCallback).attachToRecyclerView(rvSearch);

		// Set mAdapter, layout manger, and item decorations
		rvSearch.setAdapter(mAdapter);
		rvSearch.setLayoutManager(new LinearLayoutManager(getContext()));

		mLiveSearchManager = new LiveSearchManager(this::handleSearchDisplay);
	}

	private void handleSearchDisplay(String query, List<Song> results) {
		if(query.isEmpty()) {
			mAdapter.clear();
			showSearchDescription(null);
			showText(getString(R.string.new_search));
		} else if(results.isEmpty()) {
			mAdapter.clear();
			showSearchDescription(null);
			showText(getString(R.string.no_search_result));
		} else {
			showSearchDescription(query);
			hideText();
			mAdapter.addAll(results);
		}
	}

	public void clear() {
		mAdapter.clear();
		handleSearchDisplay("", null);
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
				if(search.getResults().isEmpty()) {
					mAdapter.clear();
					showSearchDescription(null);
					showText(getString(R.string.no_search_result));
				} else {
					showSearchDescription(searchText);
					hideText();
					mAdapter.addAll(search.getResults());
				}
			} else {
				if(e.getMessage().startsWith("400")) {
					showText(getString(R.string.no_search_result));
				} else {
					showText(getString(R.string.search_error));
				}
			}
		});
	}

	private void showText(String text) {
		textContainer.setVisibility(View.VISIBLE);
		tvError.setText(text);
	}

	private void showSearchDescription(@Nullable String query) {
		if(query == null) {
			tvDescription.setVisibility(View.GONE);
		} else {
			String text = String.format("Showing results for \"%s\"", query);
			SpannableStringBuilder builder = new SpannableStringBuilder(text);
			builder.setSpan(new StyleSpan(Typeface.BOLD),
					21, text.length() - 1,
					Spanned.SPAN_INCLUSIVE_INCLUSIVE);
			tvDescription.setText(builder);
			tvDescription.setVisibility(View.VISIBLE);
		}
	}

	private void hideText() {
		textContainer.setVisibility(View.GONE);
	}
}
