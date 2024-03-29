package com.example.curate.fragments;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.curate.R;
import com.example.curate.activities.MainActivity;
import com.example.curate.adapters.DividerItemDecoration;
import com.example.curate.adapters.ItemTouchHelperCallbacks;
import com.example.curate.adapters.NonPredictiveLinearLayoutManager;
import com.example.curate.adapters.QueueAdapter;
import com.example.curate.models.Party;
import com.example.curate.models.PlaylistEntry;
import com.parse.SaveCallback;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class QueueFragment extends Fragment {
	private QueueAdapter mAdapter;
	private Party mParty;
	private SaveCallback mPlaylistUpdatedCallback;

	@BindView(R.id.rvQueue) RecyclerView rvQueue;
	@BindView(R.id.textContainer) LinearLayout textContainer;
	@BindView(R.id.tvError) TextView tvError;

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
	 * Set up the mAdapter and recycler view when the fragment view is created.
	 * @param view
	 * @param savedInstanceState
	 */
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ButterKnife.bind(this, view);

		mParty = Party.getCurrentParty();

		textContainer.setVisibility(mParty.getPlaylist().isEmpty() ? View.VISIBLE : View.INVISIBLE);

		mAdapter = new QueueAdapter(getContext(), mParty.getPlaylist().getEntries(), (MainActivity) getActivity());
		mAdapter.setHasStableIds(true);

		ItemTouchHelperCallbacks callbacks = new ItemTouchHelperCallbacks(mAdapter, getContext());
		new ItemTouchHelper(callbacks.likeCallback).attachToRecyclerView(rvQueue);
		if(Party.getCurrentParty().isCurrentUserAdmin())
			new ItemTouchHelper(callbacks.deleteCallback).attachToRecyclerView(rvQueue);

		rvQueue.setAdapter(mAdapter);
		rvQueue.setLayoutManager(new NonPredictiveLinearLayoutManager(getContext()));
		rvQueue.addItemDecoration(new DividerItemDecoration(rvQueue.getContext(), R.drawable.divider));
		initializePlaylistUpdateCallback();
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("queue fragment", "PAUSED");
		if(!mParty.isCurrentUserAdmin()) {
			mParty.disconnectFromLiveQuery();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("queue fragment", "RESUMED");
		mParty.reconnectToLiveQuery();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Deregister the callback when this fragment is destroyed
		mParty.getPlaylist().deregisterUpdateCallback(mPlaylistUpdatedCallback);
	}

	private void initializePlaylistUpdateCallback() {
		mPlaylistUpdatedCallback = e -> {
			if(e == null) {
				// TODO: make this thread safe
				List<PlaylistEntry> entries = mParty.getPlaylist().getEntries();
				getActivity().runOnUiThread(() -> {
					mAdapter.notifyPlaylistUpdated();
					textContainer.setVisibility(entries.isEmpty() ? View.VISIBLE : View.INVISIBLE);
				});
			}
		};
		mParty.getPlaylist().registerUpdateCallback(mPlaylistUpdatedCallback);
	}
}
