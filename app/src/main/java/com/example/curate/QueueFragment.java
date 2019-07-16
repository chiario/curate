package com.example.curate;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.curate.models.Song;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 */
public class QueueFragment extends Fragment {


	@BindView(R.id.rvQueue) RecyclerView rvQueue;

	SongAdapter adapter;

	List<Song> songs;

	public QueueFragment() {
		// Required empty public constructor
	}

	public static QueueFragment newInstance() {
		Bundle args = new Bundle();
		QueueFragment fragment = new  QueueFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_queue, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ButterKnife.bind(this, view);
		songs = new ArrayList<Song>();
		adapter = new SongAdapter(getContext(), songs);
		rvQueue.setAdapter(adapter);
		rvQueue.setLayoutManager(new LinearLayoutManager(getContext()));
		rvQueue.addItemDecoration(new DividerItemDecoration(rvQueue.getContext(),
				DividerItemDecoration.VERTICAL));
		loadData();
	}

	public void loadData() {
		ParseQuery<Song> query = ParseQuery.getQuery(Song.class);
		// TODO Infinite pagination vs getting all songs?
		query.findInBackground(new FindCallback<Song>() {
			@Override
			public void done(List<Song> objects, ParseException e) {
				if(e == null) {
					for(int i = 0; i < 30; i++) {
						adapter.addAll(objects);
					}
				}
				else {
					e.printStackTrace();
				}
			}
		});
	}
}
