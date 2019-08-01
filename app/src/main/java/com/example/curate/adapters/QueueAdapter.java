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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.activities.MainActivity;
import com.example.curate.models.Party;
import com.example.curate.models.Playlist;
import com.example.curate.models.PlaylistEntry;
import com.example.curate.models.Song;
import com.example.curate.utils.NotificationHelper;
import com.example.curate.utils.EntryListDiffCallback;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class QueueAdapter extends AsyncAdapter<PlaylistEntry, QueueAdapter.ViewHolder> {
	private Context mContext;
	private MainActivity mMainActivity;
	private final Playlist mPlaylist;
	private final Object mMutex = new Object();

	/***
	 * Creates the adapter for holding playlist
	 * @param context The context the adapter is being created from
	 * @param entries The initial playlist to display
	 */
	public QueueAdapter(Context context, List<PlaylistEntry> entries, MainActivity mainActivity) {
		mContext = context;
		mMainActivity = mainActivity;
		mDataset.clear();
		mDataset.addAll(entries);
		mPlaylist = Party.getCurrentParty().getPlaylist();
	}

	@Override
	DiffUtil.Callback getDiffCallback(List<PlaylistEntry> oldList, List<PlaylistEntry> newList) {
		return new EntryListDiffCallback(oldList, newList);
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
		PlaylistEntry entry = mDataset.get(position);
		holder.bindEntry(entry);
	}

	@Override
	public int getItemCount() {
		return mDataset.size();
	}

	public void onItemSwipedRemove(RecyclerView.ViewHolder viewHolder) {
		ViewHolder vh = (ViewHolder) viewHolder;
		vh.remove();
	}

	public void onItemSwipedLike(RecyclerView.ViewHolder viewHolder) {
		ViewHolder vh = (ViewHolder) viewHolder;
		vh.like();
	}

	public void notifyPlaylistUpdated() {
		synchronized (mMutex) {
			List<PlaylistEntry> newEntries = mPlaylist.getEntries();
			update(newEntries);
		}
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

			final SaveCallback callback = e -> {
				isRemoving = false;

				if(e != null) {
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
			NotificationHelper.updateInteractionTime();
			if(isRemoving || isLiking) return;
			isLiking = true;

			final boolean isLiked = mEntry.isLikedByUser();
			final String errorMessage = isLiked ? "Couldn't unlike song!" : "Couldn't like song!";
			final SaveCallback callback = e -> {
				isLiking = false;

                if (e != null) {
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
