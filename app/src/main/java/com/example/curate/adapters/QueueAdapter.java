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
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.activities.MainActivity;
import com.example.curate.models.Party;
import com.example.curate.models.Playlist;
import com.example.curate.models.PlaylistEntry;
import com.example.curate.models.Song;
import com.parse.ParseException;
import com.parse.SaveCallback;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {
	private Context mContext;
	private MainActivity mMainActivity;
	private AsyncListDiffer<PlaylistEntry> mPlaylistDiffer;
	protected RecyclerView rvQueue;

	/***
	 * Creates the adapter for holding playlist
	 * @param context The context the adapter is being created from
	 * @param playlist The initial playlist to display
	 */
	public QueueAdapter(Context context, List<PlaylistEntry> playlist, MainActivity mainActivity) {
		mContext = context;
		mMainActivity = mainActivity;
		mPlaylistDiffer = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<PlaylistEntry>() {
			@Override
			public boolean areItemsTheSame(@NonNull PlaylistEntry oldItem, @NonNull PlaylistEntry newItem) {
				return oldItem.equals(newItem);
			}

			@Override
			public boolean areContentsTheSame(@NonNull PlaylistEntry oldItem, @NonNull PlaylistEntry newItem) {
				return oldItem.contentsEqual(newItem);
			}
		});
		mPlaylistDiffer.submitList(playlist);
	}

	@Override
	public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
		rvQueue = recyclerView;
		super.onAttachedToRecyclerView(recyclerView);
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		// Inflate the custom layout
		LayoutInflater inflater = LayoutInflater.from(mContext);
		View contactView = inflater.inflate(R.layout.item_song_queue, parent, false);

		// Return a new holder instance
		ViewHolder viewHolder = new ViewHolder(contactView);

		// Only show delete icon if current user is admin of current party
		viewHolder.ibRemove.setVisibility(Party.getCurrentParty().isCurrentUserAdmin()
				? View.VISIBLE : View.GONE);
		return viewHolder;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		PlaylistEntry entry = mPlaylistDiffer.getCurrentList().get(position);
		Log.d("QueueAdapter", String.format("Binding entry %s at position %d", entry.getSong().getTitle(), position));
		holder.bindEntry(entry);
	}

	@Override
	public int getItemCount() {
		return mPlaylistDiffer.getCurrentList().size();
	}

	public void onItemSwipedRemove(RecyclerView.ViewHolder viewHolder) {
		ViewHolder vh = (ViewHolder) viewHolder;
		vh.remove();
	}

	public void onItemSwipedLike(RecyclerView.ViewHolder viewHolder) {
		ViewHolder vh = (ViewHolder) viewHolder;
		vh.like();
	}

	public void submitPlaylist(Playlist playlist) {
		mPlaylistDiffer.submitList(playlist.getEntries());
	}

	/***
	 * Internal ViewHolder model for each item.
	 */
	public class ViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.ivAlbum) ImageView ivAlbum;
		@BindView(R.id.tvTitle) TextView tvTitle;
		@BindView(R.id.tvArtist) TextView tvArtist;
		@BindView(R.id.ibLike) ImageButton ibLike;
		@BindView(R.id.ibDelete) ImageButton ibRemove;
		@BindView(R.id.clItem) ConstraintLayout clItem;
		@BindView(R.id.pbLoading) ProgressBar pbLoading;

		boolean isRemoving = false;
		boolean isLiking = false;

		private PlaylistEntry mEntry;

		public ViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
			if (Party.getCurrentParty().isCurrentUserAdmin()) {
				itemView.setOnClickListener(view -> {
					if (mEntry != null) {
						mMainActivity.getBottomPlayerFragment().onPlayNew(mEntry.getSong().getSpotifyId());
					}
				});
			}
		}

		@OnClick(R.id.ibDelete)
		public void onClickRemove() {
			remove();
		}

		private void remove() {
			if(isRemoving || isLiking) return;
			isRemoving = true;
			showLoading(true);
			// TODO fix mass deleting (crash gracefully?)
			// TODO swiping then clicking is not working
			final SaveCallback callback = e -> {
				isRemoving = true;
				if(e == null) {
					isRemoving = false;
					submitPlaylist(Party.getCurrentParty().getPlaylist());
				} else {
					showLoading(false);
					notifyDataSetChanged();
					Toast.makeText(mContext, "Could not remove song", Toast.LENGTH_SHORT).show();
				}
			};
			Party.getCurrentParty().getPlaylist().removeEntry(mEntry, callback);
		}

		@OnClick(R.id.ibLike)
		public void onClickLike() {
			like();
		}

		public void like() {
			mMainActivity.updateInteractionTime();
			if(isRemoving || isLiking) return;
			isLiking = true;

			final boolean isLiked = mEntry.isLikedByUser();
			final String errorMessage = isLiked ? "Couldn't unlike song!" : "Couldn't like song!";
			final SaveCallback callback = e -> {
				if (e == null) {
					isLiking = false;
					submitPlaylist(Party.getCurrentParty().getPlaylist());
				} else {
					ibLike.setSelected(isLiked);
					Toast.makeText(mContext, errorMessage, Toast.LENGTH_SHORT).show();
				}
			};

			ibLike.setSelected(!isLiked);
			if(isLiked) {
				Party.getCurrentParty().getPlaylist().unlikeEntry(mEntry, callback);
			} else {
				Party.getCurrentParty().getPlaylist().likeEntry(mEntry, callback);
			}
		}

		/**
		 * Displays a PlaylistEntry's information in the view
		 * @param entry the PlaylistEntry whose information should be displayed
		 */
		private void bindEntry(PlaylistEntry entry) {
			if(mEntry != null && !mEntry.equals(entry)) {
				isLiking = false;
				isRemoving = false;
			}
			mEntry = entry;
			Song song = mEntry.getSong();
			tvTitle.setText(song.getTitle());
			ibLike.setSelected(mEntry.isLikedByUser());
			showLoading(false);

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(song.getArtist());
			String screenName = entry.getAddedBy();
			if (screenName != null) {
				stringBuilder.append(String.format(" · Added by %s", screenName));
			}

			tvArtist.setText(stringBuilder.toString());

			Glide.with(mContext)
					.load(song.getImageUrl())
					.placeholder(R.drawable.ic_album_placeholder)
					.into(ivAlbum);
		}

		/**
		 * Shows/hides the loading animation for when a entry is being removed
		 * @param isLoading if true, show the loading animation; if false, hide it
		 */
		public void showLoading(boolean isLoading) {
			if(!Party.getCurrentParty().isCurrentUserAdmin()) return;
			pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
			ibRemove.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
		}
	}
}
