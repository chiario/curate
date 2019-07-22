package com.example.curate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.SelectFrament;
import com.example.curate.models.Party;
import com.example.curate.models.Song;
import com.parse.ParseException;
import com.parse.SaveCallback;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PartyAdapter extends RecyclerView.Adapter<PartyAdapter.ViewHolder> {

	// Instance variables
	private Context mContext;
	private List<Party> mParties;
	private SelectFrament.OnOptionSelected mListener;

	/***
	 * Creates the adapter for holding mParties
	 * @param context The context the adapter is being created from
	 * @param parties The initial list of parties to display
	 */
	public PartyAdapter(Context context, List<Party> parties, SelectFrament.OnOptionSelected listener) {
		mContext = context;
		mParties = parties;
		mListener = listener;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context context = parent.getContext();
		LayoutInflater inflater = LayoutInflater.from(context);

		// Inflate the custom layout
		View contactView = inflater.inflate(R.layout.item_party, parent, false);
		// Return a new holder instance
		ViewHolder viewHolder = new ViewHolder(contactView);
		return viewHolder;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		// Todo load selected state and image into ViewHolder
		Party party = mParties.get(position);
	}

	@Override
	public int getItemCount() {
		return mParties.size();
	}

	/***
	 * Internal ViewHolder model for each item.
	 */
	public class ViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.btnJoin) Button btnJoin;

		public ViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		@OnClick(R.id.btnJoin)
		public void onClickJoin(View v) {
			Party party = mParties.get(getAdapterPosition());
			Party.joinParty(party.getJoinCode(), e -> {
				if(e == null) {
					mListener.onPartyObtained();
				} else {
					Toast.makeText(mContext, "Could not join that party!", Toast.LENGTH_SHORT).show();
				}
			});
		}
	}
}