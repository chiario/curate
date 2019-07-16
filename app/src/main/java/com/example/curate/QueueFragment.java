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
import android.widget.Toast;

import com.example.curate.models.Party;
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
public class QueueFragment extends Fragment {


	// Instance variables

	@BindView(R.id.rvQueue) RecyclerView rvQueue;
	QueueAdapter adapter;
	List<Song> songs;
	Party party;

	public QueueFragment() {
		// Required empty public constructor
	}

	/***
	 * Creates a new instance of the QueueFragment
	 * @return The new instance created
	 */
	public static QueueFragment newInstance() {
//		Bundle args = new Bundle();
		QueueFragment fragment = new  QueueFragment();
//		fragment.setArguments(args);
		fragment.setRetainInstance(true);
		return fragment;
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
		return inflater.inflate(R.layout.fragment_queue, container, false);
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

		party = Party.getCurrentParty();
		party.updatePlaylist(e -> {
			if(e == null) {
				adapter = new QueueAdapter(getContext(), party.getPlaylist());
				rvQueue.setAdapter(adapter);
				rvQueue.setLayoutManager(new LinearLayoutManager(getContext()));
				rvQueue.addItemDecoration(new DividerItemDecoration(rvQueue.getContext(),
						DividerItemDecoration.VERTICAL));

			} else {
				Toast.makeText(getContext(), "Could not load playlist", Toast.LENGTH_LONG).show();
			}
		});
	}

	/***
	 * Adds the given song to the queue
	 * @param song Song to add to the queue
	 */
	public void addSong(Song song) {
		songs.add(song);
		adapter.notifyItemInserted(adapter.getItemCount() - 1);
	}
}
