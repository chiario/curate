package com.example.curate;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

	private List<Song> songs;

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return null;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

	}

	@Override
	public int getItemCount() {
		return 0;
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
