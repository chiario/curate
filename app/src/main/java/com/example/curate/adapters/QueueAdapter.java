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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.activities.MainActivity;
import com.example.curate.models.Party;
import com.example.curate.models.PlaylistEntry;
import com.example.curate.models.Song;
import com.parse.SaveCallback;

import java.text.ParseException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {
	private Context mContext;
	private List<PlaylistEntry> mPlaylist;
	private MainActivity mMainActivity;
	private boolean mIsSwiping; // Used to ensure only one item can be swiped at a time

	/***
	 * Creates the adapter for holding playlist
	 * @param context The context the adapter is being created from
	 * @param playlist The initial playlist to display
	 */
	public QueueAdapter(Context context, List<PlaylistEntry> playlist, MainActivity mainActivity) {
		mContext = context;
		mPlaylist = playlist;
		mMainActivity = mainActivity;
		mIsSwiping = false;
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
		PlaylistEntry entry = mPlaylist.get(position);
		holder.showEntryData(entry);
		holder.showLoading(false);
	}

	@Override
	public long getItemId(int position) {
		Song song = mPlaylist.get(position).getSong();
		return song.getSpotifyId().hashCode();
	}

	@Override
	public int getItemCount() {
		return mPlaylist.size();
	}

	public void onItemSwipedRemove(RecyclerView.ViewHolder viewHolder) {
		mIsSwiping = true;
		ViewHolder vh = (ViewHolder) viewHolder;
		vh.onClickRemove();
	}

	public void onItemSwipedLike(RecyclerView.ViewHolder viewHolder) {
		mIsSwiping = true;
		ViewHolder vh = (ViewHolder) viewHolder;
		vh.onClickLike(vh.ibLike);
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
		@BindView(R.id.ibDelete) ImageButton ibRemove;
		@BindView(R.id.clItem) ConstraintLayout clItem;
		@BindView(R.id.pbLoading) ProgressBar pbLoading;

		private boolean isUpdating;

		public ViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}


		@OnClick(R.id.ibDelete)
		public void onClickRemove() {
			if(isUpdating) return;
			isUpdating = true;
			showLoading(true);
			// TODO fix mass deleting (crash gracefully?)
			Party.getCurrentParty().removeSong(mPlaylist.get(getAdapterPosition()).getSong(), e -> {
				isUpdating = false;
				mIsSwiping = false;

				if(e == null) {
					notifyDataSetChanged();
				} else {
					showLoading(false);
					Toast.makeText(mContext, "Could not remove song", Toast.LENGTH_SHORT).show();
				}
			});
		}

		@OnClick(R.id.ibLike)
		public void onClickLike(final View v) {
			mMainActivity.updateInteractionTime();
			if(isUpdating) return;
			isUpdating = true;

			final PlaylistEntry entry = mPlaylist.get(getAdapterPosition());
			final boolean isLiked = entry.isLikedByUser();
			final String errorMessage = isLiked ? "Couldn't unlike song!" : "Couldn't like song!";
			final SaveCallback callback = e -> {
				isUpdating = false;
				mIsSwiping = false;

				if (e == null) {
					notifyDataSetChanged();
				} else {
					v.setSelected(isLiked);
					Toast.makeText(mContext, errorMessage, Toast.LENGTH_SHORT).show();
				}
			};

			v.setSelected(!isLiked);
			if(isLiked) {
				Party.getCurrentParty().unlikeSong(entry.getSong().getSpotifyId(), callback);
			} else {
				Party.getCurrentParty().likeSong(entry.getSong().getSpotifyId(), callback);
			}
		}

		/**
		 * Displays a PlaylistEntry's information in the view
		 * @param entry the PlaylistEntry whose information should be displayed
		 */
		private void showEntryData(PlaylistEntry entry) {
			Song song = entry.getSong();
			tvArtist.setText(song.getArtist());
			tvTitle.setText(song.getTitle());
			ibLike.setSelected(entry.isLikedByUser());
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
