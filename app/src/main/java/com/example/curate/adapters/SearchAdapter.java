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
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.activities.MainActivity;
import com.example.curate.models.Party;
import com.example.curate.models.Song;
import com.example.curate.utils.NotificationHelper;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

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

	@Override
	public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
		super.onAttachedToRecyclerView(recyclerView);
	}

	/***
	 * Creates the adapter for holding songs
	 * @param context The context the adapter is being created from
	 */
	public SearchAdapter(Context context) {
		mContext = context;
		mSongsInAdd = new ArrayList<>();
		mSongsInQueue = new ArrayList<>();
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
			songViewHolder.showSongData(getSong(position, songViewHolder));
			songViewHolder.showLoading(false);
			if(songViewHolder.getItemViewType() == TYPE_SONG_IN_QUEUE) {
				songViewHolder.ibLike.setVisibility(View.GONE);
			} else {
				songViewHolder.ibLike.setVisibility(View.VISIBLE);
			}
		} else if(holder instanceof SectionViewHolder) {
			SectionViewHolder sectionViewHolder = (SectionViewHolder) holder;
			if(sectionViewHolder.getItemViewType() == TYPE_SECTION_QUEUE) {
				sectionViewHolder.tvSection.setText(IN_QUEUE_TEXT);
			} else {
				sectionViewHolder.tvSection.setText(ADD_TEXT);
			}
		}
	}

	/**
	 * Get the song for a position
	 * @param position - the current adapter position
	 * @param songViewHolder The viewholder which will hold the song
	 * @return the song the viewholder should hold
	 */
	private Song getSong(int position, SongViewHolder songViewHolder) {
		if(songViewHolder.getItemViewType() == TYPE_SONG_IN_QUEUE) {
			return mSongsInQueue.get(getQueuePosition(position));
		} else {
			return mSongsInAdd.get(getAddPosition(position));
		}
	}

	/**
	 * Get the number of displayed sections in the adapter
	 * @return - The number of displayd section headers
	 */
	private int getSectionCount() {
		return (mSectionAdd ? 1 : 0) + (mSectionQueue ? 1 : 0);
	}

	/**
	 * Get the total number of items in the adapter, including section headers
	 * @return - The total number of items
	 */
	@Override
	public int getItemCount() {
		return mSongsInQueue.size() + mSongsInAdd.size() + getSectionCount();
	}

	/**
	 * Gets the position in the mSongsInQueue list from the adapter position
	 * @param position - the current adapter position
	 * @return the position in the mSongsInQueue list
	 */
	private int getQueuePosition(int position) {
		return position - (mSectionQueue ? 1 : 0);
	}

	/**
	 * Gets the position in the mSongsInAdd list from the adapter position
	 * @param position - the current adapter position
	 * @return the position in the mSongsInAdd list
	 */
	private int getAddPosition(int position) {
		return position - mSongsInQueue.size() - getSectionCount();
	}

	/***
	 * Adds all songs from list into the adapter one at a time
	 * @param list Songs to add to the adapter
	 */
	public void addAll(List<Song> list) {
		clear();
		if(mSongsInAdd == null || mSongsInQueue == null || list == null) return;
		for(Song s : list) {
			if(Party.getCurrentParty().getPlaylist().contains(s)) {
				mSongsInQueue.add(s);
			} else {
				mSongsInAdd.add(s);
			}
		}
		notifyDataSetChanged();
		updateSections();
	}

	/**
	 * Updates the section titles, add and removing them if needed
	 */
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
		SongViewHolder vh = (SongViewHolder) viewHolder;
		notifyItemChanged(vh.getAdapterPosition());
		vh.onAdd();
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
			onAdd();
		}

		private void onAdd() {
			if(getItemViewType() == TYPE_SONG_IN_QUEUE || mIsAdding) return;
			NotificationHelper.updateInteractionTime();

			mIsAdding = true;
			showLoading(true);

			int position = getAdapterPosition();
			Party.getCurrentParty().getPlaylist().addEntry(mSong, e -> {
				mIsAdding = false;
				showLoading(false);
				if(e == null) {
					mSongsInAdd.remove(mSong);
					mSongsInQueue.add(mSong);
					notifyItemMoved(position, mSongsInQueue.size());
					updateSections();
					Toast.makeText(mContext, "Song Added", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
				}
			});
		}

		/**
		 * Displays a Song's information in the view
		 * Song is cached in viewholder until updated
		 * @param song the song whose information should be displayed
		 */
		private void showSongData(Song song) {
			mSong = song;
			tvArtist.setText(song.getArtist());
			tvTitle.setText(song.getTitle());
			ibLike.setSelected(false);
			showLoading(mIsAdding);
			Glide.with(mContext)
					.load(song.getImageUrl())
					.placeholder(R.drawable.ic_album_placeholder)
					.into(ivAlbum);
		}

		/**
		 * Shows/hides the loading animation for when a song is being added
		 * @param isLoading if true, show the loading animation; if false, hide it
		 */
		public void showLoading(boolean isLoading) {
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