package com.example.curate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.models.Party;
import com.example.curate.models.PlaylistEntry;
import com.example.curate.models.Song;
import com.parse.ParseUser;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {

	// Instance variables
	private Context context;
	private List<PlaylistEntry> playlist;
	private boolean isUpdating;

	/***
	 * Creates the adapter for holding playlist
	 * @param context The context the adapter is being created from
	 * @param playlist The initial playlist to display
	 */
	public QueueAdapter(Context context, List<PlaylistEntry> playlist) {
		this.context = context;
		this.playlist = playlist;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context context = parent.getContext();
		LayoutInflater inflater = LayoutInflater.from(context);
		// Inflate the custom layout
		View contactView = inflater.inflate(R.layout.item_song_queue, parent, false);
		// Return a new holder instance
		ViewHolder viewHolder = new ViewHolder(contactView);
		// Only show delete icon if current user is admin of current party
		boolean isAdmin = Party.getCurrentParty().isCurrentUserAdmin();
		if (isAdmin) {
			viewHolder.ibRemove.setVisibility(View.VISIBLE);
		} else {
			viewHolder.ibRemove.setVisibility(View.GONE);
		}
		return viewHolder;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		Song song = playlist.get(position).getSong();
		holder.tvArtist.setText(song.getArtist());
		holder.tvTitle.setText(song.getTitle());
		holder.ibLike.setSelected(playlist.get(position).isLikedByUser());
		holder.ibRemove.setSelected(false);

		Glide
				.with(context)
				.load(song.getImageUrl())
				.placeholder(R.drawable.ic_album_placeholder)
				.into(holder.ivAlbum);
	}

	@Override
	public long getItemId(int position) {
		Song song = playlist.get(position).getSong();
		return song.getSpotifyId().hashCode();
	}

	@Override
	public int getItemCount() {
		return playlist.size();
	}

	public void onItemRemove(RecyclerView.ViewHolder viewHolder) {
		ViewHolder vh = (ViewHolder) viewHolder;
		vh.onClickRemove(vh.ibRemove);
	}

	public void onItemLike(RecyclerView.ViewHolder viewHolder) {
		ViewHolder vh = (ViewHolder) viewHolder;
		vh.onClickLike(vh.ibLike);
	}

	public boolean isUpdating() {return isUpdating;}

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

		public ViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		@OnClick(R.id.ibDelete)
		public void onClickRemove(View v) {
			if(isUpdating) return;
			isUpdating = true;
			v.setSelected(true);
			Party.getCurrentParty().removeSong(playlist.get(getAdapterPosition()).getSong(), e -> {
				if(e == null) {
					// TODO change this: playlist might reorder
					notifyItemRemoved(getAdapterPosition());
				}
				isUpdating = false;
			});
		}

		@OnClick(R.id.ibLike)
		public void onClickLike(final View v) {
			final PlaylistEntry entry = playlist.get(getAdapterPosition());
			v.setSelected(!entry.isLikedByUser());
			if(entry.isLikedByUser()) {
				Party.getCurrentParty().unlikeSong(entry.getSong().getSpotifyId(), e -> {
					if(e == null) {
						notifyDataSetChanged();
					} else {
						v.setSelected(entry.isLikedByUser());
						Toast.makeText(context, "Could not unlike song!", Toast.LENGTH_SHORT).show();
					}
				});
			} else {
				Party.getCurrentParty().likeSong(entry.getSong().getSpotifyId(), e -> {
					if(e == null) {
						notifyDataSetChanged();
					} else {
						v.setSelected(entry.isLikedByUser());
						Toast.makeText(context, "Could not like song!", Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
	}
}
