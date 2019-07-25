package com.example.curate.adapters;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.models.Party;
import com.example.curate.models.Song;
import com.parse.ParseException;
import com.parse.SaveCallback;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

	// Instance variables
	private Context context;
	private List<Song> songs;
	private boolean isUpdating;

	/***
	 * Creates the adapter for holding songs
	 * @param context The context the adapter is being created from
	 * @param songs The initial list of songs to display
	 */
	public SearchAdapter(Context context, List<Song> songs) {
		this.context = context;
		this.songs = songs;
	}

	public boolean isUpdating() {
		return isUpdating;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context context = parent.getContext();
		LayoutInflater inflater = LayoutInflater.from(context);

		// Inflate the custom layout
		View contactView = inflater.inflate(R.layout.item_song_search, parent, false);
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
		if(Party.getCurrentParty().contains(song))
			holder.ibLike.setSelected(true);
		else
			holder.ibLike.setSelected(false);
		holder.pbLoading.setVisibility(View.GONE);
		holder.ibLike.setVisibility(View.VISIBLE);
		Glide.with(context).load(song.getImageUrl()).placeholder(R.drawable.ic_album_placeholder).into(holder.ivAlbum);
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
			if(Party.getCurrentParty().contains(s)) {
				continue;
			}
			songs.add(s);
			notifyItemInserted(songs.size() - 1);
		}
	}

	public void onItemAdd(RecyclerView.ViewHolder viewHolder) {
		ViewHolder searchViewHolder = (ViewHolder) viewHolder;
		searchViewHolder.onClickLike(searchViewHolder.ibLike);
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
		@BindView(R.id.pbLoading) ProgressBar pbLoading;

		private boolean isRemoving = false;

		public ViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		@OnClick(R.id.ibLike)
		public void onClickLike(View v) {
			if(isRemoving) return;

			isRemoving = true;
			// Add song to the queue
			if(!v.isSelected()) {
				int index = getAdapterPosition();
				pbLoading.setVisibility(View.VISIBLE);
				ibLike.setVisibility(View.GONE);
				Party.getCurrentParty().addSong(songs.get(getAdapterPosition()), e -> {
					isRemoving = false;
					if(e == null) {
						if(getAdapterPosition() < 0) {
							songs.remove(index);
							notifyItemRemoved(index);
						} else {
							songs.remove(getAdapterPosition());
							notifyItemRemoved(getAdapterPosition());
						}
						Toast.makeText(context, "Song Added", Toast.LENGTH_SHORT).show();
					}
					else {
						pbLoading.setVisibility(View.GONE);
						ibLike.setVisibility(View.VISIBLE);
						Toast.makeText(context, "Could not add song", Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
	}
}