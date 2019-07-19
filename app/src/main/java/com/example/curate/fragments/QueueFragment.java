package com.example.curate.fragments;


import android.graphics.Canvas;
import android.graphics.Point;
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
import com.example.curate.adapters.ItemTouchHelperCallbacks;
import com.example.curate.adapters.QueueAdapter;
import com.example.curate.models.Party;
import com.example.curate.models.Song;
import com.parse.SaveCallback;

import butterknife.BindView;
import butterknife.ButterKnife;

public class QueueFragment extends Fragment {
	private static final String DEFAULT_SONG_ID = "7GhIk7Il098yCjg4BQjzvb";

	private QueueAdapter mAdapter;
	private Party mParty;
	private SaveCallback mPlaylistUpdatedCallback;

	@BindView(R.id.rvQueue) RecyclerView rvQueue;

	public QueueFragment() {
		// Required empty public constructor
	}

	/***
	 * Creates a new instance of the QueueFragment
	 * @return The new instance created
	 */
	public static QueueFragment newInstance() {
		QueueFragment fragment = new  QueueFragment();
		fragment.setRetainInstance(true);
		return fragment;
	}

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

		mParty = Party.getCurrentParty();
		mParty.updatePlaylist(e -> {
			if(e == null) {
				mAdapter = new QueueAdapter(getContext(), mParty.getPlaylist());

				Point size = new Point();

				getActivity().getWindowManager().getDefaultDisplay().getSize(size);

				ItemTouchHelperCallbacks callbacks = new ItemTouchHelperCallbacks(mAdapter, size.x, getContext());
				new ItemTouchHelper(callbacks.fullCallback).attachToRecyclerView(rvQueue);

				rvQueue.setAdapter(mAdapter);
				rvQueue.setLayoutManager(new LinearLayoutManager(getContext()));
				rvQueue.addItemDecoration(new DividerItemDecoration(rvQueue.getContext(), R.drawable.divider));

				initializePlaylistUpdateCallback();
			} else {
				Toast.makeText(getContext(), "Could not load playlist", Toast.LENGTH_LONG).show();
			}
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Deregister the callback when this fragment is destroyed
		mParty.deregisterPlaylistUpdateCallback(mPlaylistUpdatedCallback);
	}

	private void initializePlaylistUpdateCallback() {
		mPlaylistUpdatedCallback = e -> {
			if(e == null) {
				mAdapter.notifyDataSetChanged();
			}
		};
		mParty.registerPlaylistUpdateCallback(mPlaylistUpdatedCallback);
	}

	/***
	 * Adds the given song to the queue
	 * @param song Song to add to the queue
	 */
	public void addSong(Song song) {
		mParty.addSong(song, e -> {
            if(e == null) {
                mAdapter.notifyDataSetChanged();
                Toast.makeText(getContext(), "Song Added!", Toast.LENGTH_SHORT).show();
            }
		});
	}
}
