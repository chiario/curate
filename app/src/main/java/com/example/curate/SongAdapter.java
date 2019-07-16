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

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

	private Context context;
	private List<Song> songs;

	public SongAdapter(Context context, List<Song> songs) {
		this.context = context;
		this.songs = songs;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context context = parent.getContext();
		LayoutInflater inflater = LayoutInflater.from(context);

		// Inflate the custom layout
		View contactView = inflater.inflate(R.layout.item_song, parent, false);

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

		holder.ibLike.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				view.setSelected(!song.isSelected());
				song.setSelected(!song.isSelected());
			}
		});
	}

	@Override
	public int getItemCount() {
		return songs.size();
	}

	public void addAll(List<Song> list) {
		if(songs == null || list == null) return;
		for(Song s : list) {
			songs.add(s);
			notifyItemInserted(songs.size() - 1);
		}
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.ivAlbum) ImageView ivAlbum;
		@BindView(R.id.tvTitle) TextView tvTitle;
		@BindView(R.id.tvArtist) TextView tvArtist;
		@BindView(R.id.ibLike) ImageButton ibLike;

		public ViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

	}
}
