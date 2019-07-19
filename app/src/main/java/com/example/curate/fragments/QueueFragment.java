package com.example.curate.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.curate.R;
import com.example.curate.adapters.DividerItemDecoration;
import com.example.curate.adapters.QueueAdapter;
import com.example.curate.adapters.SongTouchHelperCallback;
import com.example.curate.models.Party;
import com.example.curate.models.Song;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 */
public class QueueFragment extends Fragment {
	// Instance variables
	@BindView(R.id.rvQueue) RecyclerView rvQueue;
	private QueueAdapter adapter;
	private List<Song> songs;
	private Party party;

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
				ItemTouchHelper.Callback callback =
						new SongTouchHelperCallback(adapter);
				ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
				touchHelper.attachToRecyclerView(rvQueue);
				rvQueue.setAdapter(adapter);
				rvQueue.setLayoutManager(new LinearLayoutManager(getContext()));
				rvQueue.addItemDecoration(new DividerItemDecoration(rvQueue.getContext(), R.drawable.divider));

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
		party.addSong(song, e -> {
			if(e == null) {
				adapter.notifyItemInserted(adapter.getItemCount() - 1);
				Toast.makeText(getContext(), "Song Added!", Toast.LENGTH_SHORT).show();
			}
		});
	}
}
