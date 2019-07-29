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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	private static final int TYPE_SECTION = 0;
	public static final int TYPE_SONG_IN_QUEUE = 1;
	private static final int TYPE_SONG_IN_ADD = 2;

	private static final String IN_QUEUE_TEXT = "Songs in queue:";
	private static final String ADD_TEXT = "Add to queue:";

	private Context mContext;
	private List<Song> mSongs;
	private MainActivity mMainActivity;
	private boolean mIsSwiping; // Used to ensure only one item can be swiped at a time
	private Map<String, Integer> sectionMap;

	/***
	 * Creates the adapter for holding songs
	 * @param context The context the adapter is being created from
	 * @param songs The initial list of songs to display
	 */
	public SearchAdapter(Context context, List<Song> songs, MainActivity mainActivity) {
		mContext = context;
		mSongs = songs;
		mMainActivity = mainActivity;

		sectionMap = new HashMap<>();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		// Inflate the custom layout
		LayoutInflater inflater = LayoutInflater.from(mContext);

		if(viewType == TYPE_SONG_IN_ADD || viewType == TYPE_SONG_IN_QUEUE) {
			return new SongViewHolder(inflater.inflate(R.layout.item_song_search, parent, false));
		} else {
			return new SectionViewHolder(inflater.inflate(R.layout.item_song_section, parent, false));
		}
	}

	@Override
	public int getItemViewType(int position) {
		if(sectionMap.values().contains(position)) return TYPE_SECTION;
		else if(position < getPosition(ADD_TEXT)) return TYPE_SONG_IN_QUEUE;
		else return TYPE_SONG_IN_ADD;
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if(holder instanceof SongViewHolder) {
			SongViewHolder songViewHolder = (SongViewHolder) holder;
			Song song = mSongs.get(getSongsPosition(position));
			songViewHolder.showSongData(song);
			songViewHolder.showLoading(false);
			if(songViewHolder.getItemViewType() == TYPE_SONG_IN_QUEUE) songViewHolder.ibLike.setVisibility(View.GONE);
			else songViewHolder.ibLike.setVisibility(View.VISIBLE);
		} else if(holder instanceof SectionViewHolder) {
			SectionViewHolder sectionViewHolder = (SectionViewHolder) holder;
			if(position == getPosition(IN_QUEUE_TEXT)) {
				sectionViewHolder.tvSection.setText(IN_QUEUE_TEXT);
			} else if(position == getPosition(ADD_TEXT)) {
				sectionViewHolder.tvSection.setText(ADD_TEXT);
			}
		}
	}

	private int getPosition(String key) {
		Integer temp = sectionMap.get(key);
		return temp == null ? -1 : temp;
	}

	@Override
	public int getItemCount() {
		return mSongs.size() + sectionMap.size();
	}

	private int getSongsPosition(int position) {
		int newPosition = position;
		for(int sectionPos : sectionMap.values()) {
			if(position > sectionPos) newPosition--;
		}
		return newPosition;
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
			} else {
				notInQueue.add(s);
			}
		}
		if(mSongs.size() == 0) {
			if(sectionMap.containsKey(IN_QUEUE_TEXT)) {
				int temp = sectionMap.remove(IN_QUEUE_TEXT);
				notifyItemRemoved(temp);
			}
		} else {
			sectionMap.put(IN_QUEUE_TEXT, 0);
			notifyItemInserted(0);
		}

		sectionMap.put(ADD_TEXT, getItemCount());

		if(notInQueue.size() == 0 && sectionMap.containsKey(ADD_TEXT)) {
			int temp = sectionMap.remove(ADD_TEXT);
			notifyItemRemoved(temp);
		}
		for(Song s : notInQueue) {
			mSongs.add(s);
			notifyItemInserted(mSongs.size() + 1);
		}
	}

	public void clear() {
		mSongs.clear();
		sectionMap.clear();
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
			if(getItemViewType() == TYPE_SONG_IN_QUEUE) return;
			mMainActivity.updateInteractionTime();
			if(mIsAdding) return;
			mIsAdding = true;

			showLoading(true);
			Song song = mSongs.get(getSongsPosition(getAdapterPosition()));
			Party.getCurrentParty().addSong(song, e -> {
				mIsAdding = false;
				mIsSwiping = false;

				if(e == null) {
					int addPosition = sectionMap.get(ADD_TEXT);
					mSongs.remove(song);
					notifyItemRemoved(getAdapterPosition());
					mSongs.add(addPosition - 1, song);
					notifyItemInserted(addPosition++);
					if(addPosition == mSongs.size() + sectionMap.size() - 1) {
						sectionMap.remove(ADD_TEXT);
						notifyItemRemoved(getItemCount() - 1);
					} else {
						sectionMap.put(ADD_TEXT, addPosition);
					}
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