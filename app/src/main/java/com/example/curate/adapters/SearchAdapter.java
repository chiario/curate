package com.example.curate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.activities.MainActivity;
import com.example.curate.models.Party;
import com.example.curate.models.Song;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
	private Context mContext;
	private List<Song> mSongs;
	private MainActivity mMainActivity;
	private boolean mIsSwiping; // Used to ensure only one item can be swiped at a time

	/***
	 * Creates the adapter for holding songs
	 * @param context The context the adapter is being created from
	 * @param songs The initial list of songs to display
	 */
	public SearchAdapter(Context context, List<Song> songs, MainActivity mainActivity) {
		mContext = context;
		mSongs = songs;
		mMainActivity = mainActivity;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		// Inflate the custom layout
		LayoutInflater inflater = LayoutInflater.from(mContext);
		View searchItemView = inflater.inflate(R.layout.item_song_search, parent, false);

		// Return a new holder instance
		return new ViewHolder(searchItemView);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		Song song = mSongs.get(position);
		holder.showSongData(song);
		holder.showLoading(false);
	}

	@Override
	public int getItemCount() {
		return mSongs.size();
	}

	/***
	 * Adds all songs from list into the adapter one at a time
	 * @param list Songs to add to the adapter
	 */
	public void addAll(List<Song> list) {
		if(mSongs == null || list == null) return;
		for(Song s : list) {
			if(Party.getCurrentParty().contains(s)) {
				continue;
			}
			mSongs.add(s);
			notifyItemInserted(mSongs.size() - 1);
		}
	}

	public void clear() {
		mSongs.clear();
		notifyDataSetChanged();
	}

	public void onItemSwipedAdd(RecyclerView.ViewHolder viewHolder) {
		mIsSwiping = true;
		SearchAdapter.ViewHolder vh = (SearchAdapter.ViewHolder) viewHolder;
		vh.onClickAdd(vh.ibLike);
	}

	public boolean isSwiping() {
		return mIsSwiping;
	}

	/***
	 * Internal ViewHolder model for each item.
	 */
	public class ViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.ivAlbum) ImageView ivAlbum;
		@BindView(R.id.tvTitle) TextView tvTitle;
		@BindView(R.id.tvArtist) TextView tvArtist;
		@BindView(R.id.ibLike) ImageButton ibLike;
		@BindView(R.id.pbLoading) ProgressBar pbLoading;

		private boolean mIsAdding = false; // Used to ensure the item can't be added multiple times

		private ViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		@OnClick({R.id.clContainer, R.id.ibLike})
		public void onClickAdd(View v) {
			mMainActivity.updateInteractionTime();
			if(mIsAdding) return;
			mIsAdding = true;

			showLoading(true);
			Party.getCurrentParty().addSong(mSongs.get(getAdapterPosition()), e -> {
				mIsAdding = false;
				mIsSwiping = false;

				if(e == null) {
					mSongs.remove(getAdapterPosition());
					notifyItemRemoved(getAdapterPosition());
					Toast.makeText(mContext, "Song Added", Toast.LENGTH_SHORT).show();
				}
				else {
					showLoading(false);
					Toast.makeText(mContext, "Could not add song", Toast.LENGTH_SHORT).show();
				}
			});
		}

		/**
		 * Displays a Song's information in the view
		 * @param song the song whose information should be displayed
		 */
		private void showSongData(Song song) {
			tvArtist.setText(song.getArtist());
			tvTitle.setText(song.getTitle());
			ibLike.setSelected(false);
			Glide.with(mContext)
					.load(song.getImageUrl())
					.placeholder(R.drawable.ic_album_placeholder)
					.into(ivAlbum);
		}

		/**
		 * Shows/hides the loading animation for when a song is being added
		 * @param isLoading if true, show the loading animation; if false, hide it
		 */
		private void showLoading(boolean isLoading) {
			pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
			ibLike.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
		}
	}
}