package com.example.curate.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.BaseRequestOptions;
import com.example.curate.R;
import com.example.curate.models.Party;
import com.example.curate.models.PlaylistEntry;
import com.example.curate.models.Song;
import com.parse.ParseException;
import com.parse.SaveCallback;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {

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
		return viewHolder;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		// Todo load selected state and image into ViewHolder
		Song song = playlist.get(position).getSong();
		holder.tvArtist.setText(song.getArtist());
		holder.tvTitle.setText(song.getTitle());
		holder.ibLike.setSelected(song.isSelected());
		holder.ibDelete.setSelected(false);

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

	/***
	 * Internal ViewHolder model for each item.
	 */
	public class ViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.ivAlbum) ImageView ivAlbum;
		@BindView(R.id.tvTitle) TextView tvTitle;
		@BindView(R.id.tvArtist) TextView tvArtist;
		@BindView(R.id.ibLike) ImageButton ibLike;
		@BindView(R.id.ibDelete) ImageButton ibDelete;

		public ViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		@OnClick(R.id.ibDelete)
		public void onClickDelete(View v) {
			v.setSelected(true);
			Party.getCurrentParty().removeSong(playlist.get(getAdapterPosition()).getSong(), new SaveCallback() {
				@Override
				public void done(ParseException e) {
					if(e == null) {
						notifyItemRemoved(getAdapterPosition());
					}
				}
			});
		}

		@OnClick(R.id.ibLike)
		public void onClickLike(View v) {
			// TODO: Let the server know that the song was liked
			Song song = playlist.get(getAdapterPosition()).getSong();
			v.setSelected(!song.isSelected());
			song.setSelected(!song.isSelected());
		}
	}
}
