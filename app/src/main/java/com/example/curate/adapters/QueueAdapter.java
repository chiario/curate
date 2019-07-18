package com.example.curate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.models.Party;
import com.example.curate.models.PlaylistEntry;
import com.example.curate.models.Song;
import com.parse.ParseUser;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> implements SongTouchHelperAdapter{

	// Instance variables
	private Context context;
	private List<PlaylistEntry> playlist;
	private OnSongLikedListener onSongLikedListener;

	/***
	 * Creates the adapter for holding playlist
	 * @param context The context the adapter is being created from
	 * @param playlist The initial playlist to display
	 */
	public QueueAdapter(Context context, List<PlaylistEntry> playlist) {
		this.context = context;
		this.playlist = playlist;
	}


	// Todo figure out the future of these listeners
	public interface OnSongLikedListener {
		public void onSongLiked(Song song);
	}

	public void setListener(OnSongLikedListener listener) {
		onSongLikedListener = listener;
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
		boolean isAdmin = ParseUser.getCurrentUser().getObjectId().equals(Party.getCurrentParty().getAdmin().getObjectId());
		if (isAdmin) {
			viewHolder.ibRemove.setVisibility(View.VISIBLE);
		} else {
			viewHolder.ibRemove.setVisibility(View.GONE);
		}
		return viewHolder;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		// Todo load selected state and image into ViewHolder
		Song song = playlist.get(position).getSong();
		holder.tvArtist.setText(song.getArtist());
		holder.tvTitle.setText(song.getTitle());
		holder.ibLike.setSelected(playlist.get(position).isLikedByUser());
		holder.ibRemove.setSelected(false);

		Glide.with(context).load(song.getImageUrl()).placeholder(R.drawable.ic_album_placeholder).into(holder.ivAlbum);
	}

	@Override
	public int getItemCount() {
		return playlist.size();
	}

	public void clear() {
		playlist.clear();
		notifyDataSetChanged();
	}

	@Override
	public void onItemDismiss(int position) {
		playlist.remove(position);
		notifyItemRemoved(position);
	}

	@Override
	public void onItemMove(int fromPosition, int toPosition) {
		// TODO: Hook up to proper cloud code
		if (fromPosition < toPosition) {
			for (int i = fromPosition; i < toPosition; i++) {
				Collections.swap(playlist, i, i + 1);
			}
		} else {
			for (int i = fromPosition; i > toPosition; i--) {
				Collections.swap(playlist, i, i - 1);
			}
		}
		notifyItemMoved(fromPosition, toPosition);
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

		public ViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		@OnClick(R.id.ibDelete)
		public void onClickRemove(View v) {
			v.setSelected(true);
			Party.getCurrentParty().removeSong(playlist.get(getAdapterPosition()).getSong(), e -> {
				if(e == null) {
					// TODO change this: playlist might reorder
					notifyItemRemoved(getAdapterPosition());
				}
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
