package com.example.curate;


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

	public SearchFragment() {
		// Required empty public constructor
	}

	public static SearchFragment newInstance() {
//		Bundle args = new Bundle();
		SearchFragment fragment = new SearchFragment();
//		fragment.setArguments(args);
		fragment.setRetainInstance(true);
		return fragment;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
		Log.d(TAG, this.searchText);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_search, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ButterKnife.bind(this, view);
		if(savedInstanceState != null)
			searchText = savedInstanceState.getString(KEY_SEARCH);
		songs = new ArrayList<Song>();
		adapter = new SongAdapter(getContext(), songs);
		rvSearch.setAdapter(adapter);
		rvSearch.setLayoutManager(new LinearLayoutManager(getContext()));
		rvSearch.addItemDecoration(new DividerItemDecoration(rvSearch.getContext(),
				DividerItemDecoration.VERTICAL));
		loadData(searchText);
	}

	public void loadData(String searchText) {
		// Todo search spotify API
		Log.d(TAG, String.format("Searched for : %s", searchText));
	}
}
