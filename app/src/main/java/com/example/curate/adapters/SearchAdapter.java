package com.example.curate.adapters;

import android.content.Context;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	private static final int TYPE_SONG = 0;
	private static final int TYPE_SECTION = 1;

	private Context mContext;
	private List<Song> mSongs;
	private MainActivity mMainActivity;
	protected int mAddToQueuePosition = 1;
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
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		// Inflate the custom layout
		LayoutInflater inflater = LayoutInflater.from(mContext);

		if(viewType == TYPE_SONG) {
			return new SongViewHolder(inflater.inflate(R.layout.item_song_search, parent, false));
		} else {
			return new SectionViewHolder(inflater.inflate(R.layout.item_song_section, parent, false));
		}
	}

	@Override
	public int getItemViewType(int position) {
		if(position == 0 || position == mAddToQueuePosition) return TYPE_SECTION;
		return TYPE_SONG;
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if(holder instanceof SongViewHolder) {
			SongViewHolder songViewHolder = (SongViewHolder) holder;
			Song song = mSongs.get(getSongsPosition(position));
			songViewHolder.showSongData(song);
			songViewHolder.showLoading(false);
			if(position < mAddToQueuePosition) songViewHolder.ibLike.setVisibility(View.GONE);
			else songViewHolder.ibLike.setVisibility(View.VISIBLE);
		} else if(holder instanceof SectionViewHolder) {
			SectionViewHolder sectionViewHolder = (SectionViewHolder) holder;
			if(position == 0) {
				if(mSongs.size() == 0) {
					sectionViewHolder.tvSection.setText("");
				} else if(mAddToQueuePosition == 1) {
					sectionViewHolder.tvSection.setText("No matching songs in your queue");
				} else {
					sectionViewHolder.tvSection.setText("Songs in queue:");
				}
			} else {
				if(position == mSongs.size() + 1 || mSongs.size() == 0) {
					sectionViewHolder.tvSection.setText("");
				} else {
					sectionViewHolder.tvSection.setText("Add to queue:");
				}
			}
		}
	}

	@Override
	public int getItemCount() {
		return mSongs.size() + 2;
	}

	private int getSongsPosition(int position) {
		if(position >= mAddToQueuePosition) return position - 2;
		return position - 1;
	}

	/***
	 * Adds all songs from list into the adapter one at a time
	 * @param list Songs to add to the adapter
	 */
	public void addAll(List<Song> list) {
		if(mSongs == null || list == null) return;
		List<Song> notInQueue = new ArrayList<>();
		for(Song s : list) {
			if(Party.getCurrentParty().contains(s)) {
				mSongs.add(s);
				notifyItemInserted(1);
				mAddToQueuePosition++;
			} else {
				notInQueue.add(s);
			}
		}

		for(Song s : notInQueue) {
			mSongs.add(s);
			notifyItemInserted(mSongs.size() + 1);
		}
	}

	public void clear() {
		mAddToQueuePosition = 1;
		mSongs.clear();
		notifyDataSetChanged();
	}

	public void onItemSwipedAdd(RecyclerView.ViewHolder viewHolder) {
		mIsSwiping = true;
		SongViewHolder vh = (SongViewHolder) viewHolder;
		vh.onClickAdd();
	}

	public boolean isSwiping() {
		return mIsSwiping;
	}

	/***
	 * Internal ViewHolder model for each song.
	 */
	public class SongViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.ivAlbum) ImageView ivAlbum;
		@BindView(R.id.tvTitle) TextView tvTitle;
		@BindView(R.id.tvArtist) TextView tvArtist;
		@BindView(R.id.ibLike) ImageButton ibLike;
		@BindView(R.id.pbLoading) ProgressBar pbLoading;

		private boolean mIsAdding = false; // Used to ensure the item can't be added multiple times

		private SongViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		@OnClick({R.id.clContainer, R.id.ibLike})
		public void onClickAdd() {
			if(getAdapterPosition() < mAddToQueuePosition) return;
			mMainActivity.updateInteractionTime();
			if(mIsAdding) return;
			mIsAdding = true;

			showLoading(true);
			Log.d("TAG", String.format("ap: %d, sp: %d", getAdapterPosition(), getSongsPosition(getAdapterPosition())));
			Song song = mSongs.get(getSongsPosition(getAdapterPosition()));
			Party.getCurrentParty().addSong(song, e -> {
				mIsAdding = false;
				mIsSwiping = false;

				if(e == null) {
					mSongs.remove(song);
					notifyItemRemoved(getAdapterPosition());
					mSongs.add(mAddToQueuePosition - 1, song);
					notifyItemInserted(mAddToQueuePosition++);
					Toast.makeText(mContext, "Song Added", Toast.LENGTH_SHORT).show();
				} else {
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

	/***
	 * Internal ViewHolder model for each section heading
	 */
	public class SectionViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.tvSection) TextView tvSection;

		private SectionViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}
	}
}