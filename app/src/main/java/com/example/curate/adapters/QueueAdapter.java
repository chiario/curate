package com.example.curate.adapters;

import android.content.Context;
import android.os.SystemClock;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.activities.MainActivity;
import com.example.curate.models.Party;
import com.example.curate.models.Playlist;
import com.example.curate.models.PlaylistEntry;
import com.example.curate.models.Song;
import com.example.curate.utils.NotificationHelper;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {
	private Context mContext;
	private MainActivity mMainActivity;
	private final Playlist mPlaylist;
	private List<PlaylistEntry> mDataset;

	/***
	 * Creates the adapter for holding playlist
	 * @param context The context the adapter is being created from
	 * @param entries The initial playlist to display
	 */
	public QueueAdapter(Context context, List<PlaylistEntry> entries, MainActivity mainActivity) {
		mContext = context;
		mMainActivity = mainActivity;
		mDataset = new ArrayList<>(entries);
		mPlaylist = Party.getCurrentParty().getPlaylist();
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
		mDataset.clear();
		List<PlaylistEntry> entries = mPlaylist.getEntries();
		mDataset.addAll(entries);
		notifyDataSetChanged();
	}

	@Override
	public long getItemId(int position) {
		return mDataset.get(position).getObjectId().hashCode();
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

			final int index = mDataset.indexOf(mEntry);
			if (index >= 0) {
				mDataset.remove(index);
				notifyItemRemoved(index);
			}

			final SaveCallback callback = e -> {
				isRemoving = false;

				if(e != null) {
					notifyPlaylistUpdated();
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
				if(e == null) {
					ibLike.setSelected(!isLiked);
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
			Log.d("Queue", "Binding " + entry.getSong().getTitle());
			if(mEntry != null && !mEntry.equals(entry)) {
				isLiking = false;
				isRemoving = false;
			}
			mEntry = entry;
			Song song = mEntry.getSong();
			tvTitle.setText(song.getTitle());
			ibLike.setSelected(mEntry.isLikedByUser());

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
	}
}
