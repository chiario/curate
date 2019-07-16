package com.example.curate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.curate.models.Song;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.ViewHolder> {

	// Instance variables
	private Context context;
	private List<Song> songs;
	private View.OnClickListener onClickListener;
	private OnSongLikedListener onSongLikedListener;

	/***
	 * Creates the adapter for holding songs
	 * @param context The context the adapter is being created from
	 * @param songs The initial list of songs to display
	 */
	public QueueAdapter(Context context, List<Song> songs) {
		this.context = context;
		this.songs = songs;
	}

	interface OnSongLikedListener {
		public void onSongAdded(Song song);
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
		Song song = songs.get(position);
		holder.tvArtist.setText(song.getArtist());
		holder.tvTitle.setText(song.getTitle());
		holder.ibLike.setSelected(song.isSelected());

		Glide.with(context).load(song.getImageUrl()).into(holder.ivAlbum);
	}

	@Override
	public int getItemCount() {
		return songs.size();
	}

	/***
	 * Adds all songs from list into the adapter one at a time
	 * @param list Songs to add to the adapter
	 */
	public void addAll(List<Song> list) {
		if(songs == null || list == null) return;
		for(Song s : list) {
			songs.add(s);
			notifyItemInserted(songs.size() - 1);
		}
	}

	public void clear() {
		songs.clear();
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
			songs.remove(getAdapterPosition());
			notifyItemRemoved(getAdapterPosition());
		}

		@OnClick(R.id.ibLike)
		public void onClickLike(View v) {
				// Let the server know that the song was liked
			Song song = songs.get(getAdapterPosition());
			v.setSelected(!song.isSelected());
			song.setSelected(!song.isSelected());
		}
	}
}
