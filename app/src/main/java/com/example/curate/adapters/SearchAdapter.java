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
import androidx.recyclerview.widget.LinearLayoutManager;
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
	private static final int TYPE_SECTION_QUEUE = 0;
	private static final int TYPE_SECTION_ADD = 1;
	public static final int TYPE_SONG_IN_QUEUE = 2;
	private static final int TYPE_SONG_IN_ADD = 3;

	private static final String IN_QUEUE_TEXT = "Songs in queue:";
	private static final String ADD_TEXT = "Add to queue:";


	private Context mContext;
	private List<Song> mSongsInQueue;
	private List<Song> mSongsInAdd;
	private boolean mSectionQueue = false;
	private boolean mSectionAdd = false;
	private MainActivity mMainActivity;
	private RecyclerView rvSearch;

	private boolean mIsSwiping; // Used to ensure only one item can be swiped at a time

	@Override
	public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
		rvSearch = recyclerView;
		super.onAttachedToRecyclerView(recyclerView);
	}

	/***
	 * Creates the adapter for holding songs
	 * @param context The context the adapter is being created from
	 * @param songs The initial list of songs to display
	 */
	public SearchAdapter(Context context, List<Song> songs, MainActivity mainActivity) {
		mContext = context;
		mSongsInAdd = new ArrayList<>();
		mSongsInQueue = new ArrayList<>();
		mMainActivity = mainActivity;
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
		if(position == 0 && mSongsInQueue.size() != 0) return TYPE_SECTION_QUEUE;
		else if(position == mSongsInQueue.size() + (mSongsInQueue.size() == 0 ? 0 : 1)) return TYPE_SECTION_ADD;
		else if(position < mSongsInQueue.size() + 1) return TYPE_SONG_IN_QUEUE;
		else return TYPE_SONG_IN_ADD;
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if(holder instanceof SongViewHolder) {
			SongViewHolder songViewHolder = (SongViewHolder) holder;
			if(songViewHolder.getItemViewType() == TYPE_SONG_IN_QUEUE) {
				songViewHolder.ibLike.setVisibility(View.GONE);
			} else {
				songViewHolder.ibLike.setVisibility(View.VISIBLE);
			}
			songViewHolder.showSongData(getSong(position, songViewHolder));
			songViewHolder.showLoading(false);
		} else if(holder instanceof SectionViewHolder) {
			SectionViewHolder sectionViewHolder = (SectionViewHolder) holder;
			if(sectionViewHolder.getItemViewType() == TYPE_SECTION_QUEUE) {
				sectionViewHolder.tvSection.setText(IN_QUEUE_TEXT);
			} else {
				sectionViewHolder.tvSection.setText(ADD_TEXT);
			}
		}
	}

	private Song getSong(int position, SongViewHolder songViewHolder) {
		if(songViewHolder.getItemViewType() == TYPE_SONG_IN_QUEUE) {
			return mSongsInQueue.get(getQueuePosition(position));
		} else {
			return mSongsInAdd.get(getAddPosition(position));
		}
	}

	private int getSectionCount() {
		return (mSectionAdd ? 1 : 0) + (mSectionQueue ? 1 : 0);
	}

	@Override
	public int getItemCount() {
		return mSongsInQueue.size() + mSongsInAdd.size() + getSectionCount();
	}

	private int getQueuePosition(int position) {
		return position - (mSectionQueue ? 1 : 0);
	}

	private int getAddPosition(int position) {
		return position - mSongsInQueue.size() - getSectionCount();
	}

	/***
	 * Adds all songs from list into the adapter one at a time
	 * @param list Songs to add to the adapter
	 */
	public void addAll(List<Song> list) {
		if(mSongsInAdd == null || mSongsInQueue == null || list == null) return;
		for(Song s : list) {
			if(Party.getCurrentParty().contains(s)) {
				mSongsInQueue.add(s);
			} else {
				mSongsInAdd.add(s);
			}
		}
		notifyDataSetChanged();
		updateSections();
	}

	private void updateSections() {
		if(mSongsInQueue.size() == 0) {
			if(mSectionQueue) {
				mSectionQueue = false;
				notifyItemRemoved(0);
			}
		} else {
			if(!mSectionQueue) {
				mSectionQueue = true;
				notifyItemInserted(0);
				int scroll = Math.max(((LinearLayoutManager) rvSearch.getLayoutManager()).findFirstVisibleItemPosition() - 1, 0);
				rvSearch.smoothScrollToPosition(scroll);
			}
		}

		if(mSongsInAdd.size() == 0) {
			if(mSectionAdd) {
				mSectionAdd = false;
				notifyItemRemoved(getItemCount());
			}
		} else {
			if(!mSectionAdd) {
				mSectionAdd = true;
				notifyItemInserted(mSongsInQueue.size());
			}
		}
	}

	public void clear() {
		mSongsInAdd.clear();
		mSongsInQueue.clear();
		mSectionAdd = false;
		mSectionQueue = false;
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
		private Song mSong;

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
			Party.getCurrentParty().addSong(mSong, e -> {
				mIsAdding = false;
				mIsSwiping = false;

				if(e == null) {
					mSongsInAdd.remove(mSong);
					notifyItemRemoved(getAdapterPosition());
					mSongsInQueue.add(mSong);
					notifyItemInserted(mSongsInQueue.size());
					updateSections();
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
			mSong = song;
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