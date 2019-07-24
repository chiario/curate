package com.example.curate.fragments;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.models.Party;
import com.example.curate.models.Song;
import com.example.curate.utils.LocationManager;
import com.example.curate.utils.SpotifyPlayer;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.parse.ParseException;
import com.parse.SaveCallback;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerContext;
import com.spotify.protocol.types.PlayerState;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.graphics.Typeface.BOLD;
import static android.graphics.Typeface.NORMAL;

public class BottomPlayerClientFragment extends Fragment {
    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvArtist) TextView tvArtist;
    @BindView(R.id.ivAlbum) ImageView ivAlbum;
    @BindView(R.id.clCurrPlaying) ConstraintLayout mPlayerBackground;
    @BindView(R.id.ibExpandCollapse) ImageButton ibExpandCollapse;

    private SaveCallback mCurrentSongUpdatedCallback;
    private Party mParty;

    private ConstraintSet mCollapsed;
    private ConstraintSet mExpanded;

    private String mTrackName = "--";
    private String mArtistName = "--";
    private boolean isExpanded;

    public BottomPlayerClientFragment() {
        // Required empty public constructor
    }

    public static BottomPlayerClientFragment newInstance() {
        BottomPlayerClientFragment fragment = new BottomPlayerClientFragment();
        return fragment;
    }

    /***
     * Set the bottom players expanded state
     * @param isExpanded The new state to be in
     */
    private void setExpanded(boolean isExpanded) {
        this.isExpanded = isExpanded;
        ViewGroup.LayoutParams params = mPlayerBackground.getLayoutParams();
        if(isExpanded) {
            mExpanded.applyTo(mPlayerBackground);
            ivAlbum.setVisibility(View.VISIBLE);
            params.height = Math.round(getResources().getDimension(R.dimen.bottom_player_client_height_expanded));
            ibExpandCollapse.setSelected(true);
            mPlayerBackground.setLayoutParams(params);
        }
        else {
            mCollapsed.applyTo(mPlayerBackground);
            ivAlbum.setVisibility(View.GONE);
            params.height = Math.round(getResources().getDimension(R.dimen.bottom_player_client_height_collapsed));
            ibExpandCollapse.setSelected(false);
            mPlayerBackground.setLayoutParams(params);
        }
        updateText();
    }

    private void updateText() {
        int flag = SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE;
        if(isExpanded) {
            tvTitle.setText(mTrackName);
            tvTitle.setTypeface(null, BOLD);
            tvArtist.setText(mArtistName);
            tvArtist.setVisibility(View.VISIBLE);
        }
        else {
            tvTitle.setSelected(true);
            tvTitle.setTypeface(null, NORMAL);
            SpannableString title = new SpannableString(String.format("%s - ", mTrackName));
            SpannableString artist = new SpannableString(mArtistName);
            title.setSpan(new StyleSpan(BOLD), 0, title.length(), flag);
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(title);
            builder.append(artist);
            tvTitle.setText(builder);
            tvArtist.setVisibility(View.INVISIBLE);
        }
    }

    @OnClick(R.id.ibExpandCollapse)
    public void onClickExpandCollapse(View v) {
        setExpanded(!ibExpandCollapse.isSelected());
    }

    @OnClick(R.id.clCurrPlaying)
    public void onClickClCurrPlaying(View v) {
        setExpanded(!ibExpandCollapse.isSelected());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bottom_player_client, container, false);
        ButterKnife.bind(this, view);
        mParty = Party.getCurrentParty();

        initializeSongUpdateCallback();
        mCollapsed = new ConstraintSet();
        mCollapsed.clone(getContext(), R.layout.fragment_bottom_player_client_collapsed);
        mExpanded = new ConstraintSet();
        mExpanded.clone(getContext(), R.layout.fragment_bottom_player_client);
        setExpanded(false);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeSongUpdateCallback();
    }

    private void initializeSongUpdateCallback() {
        mCurrentSongUpdatedCallback = e -> {
            if (e == null) {
                Song currentSong = Party.getCurrentParty().getCurrentSong();
                if (currentSong != null) {
                    try {
                        currentSong.fetchIfNeeded(); // TODO - work around this fetch; add ParseCloud function??
                        mTrackName = currentSong.getTitle();
                        mArtistName = currentSong.getArtist();
                        updateText();
                        Glide.with(this).load(currentSong.getImageUrl()).into(ivAlbum);
                    } catch (ParseException e1) {
                        e1.printStackTrace();
                    }
                }
            } else {
                Log.d("BottomPlayerClient", "Error in song update callback", e);
            }
        };
        mParty.registerPlaylistUpdateCallback(mCurrentSongUpdatedCallback);
    }

    private void removeSongUpdateCallback() {
        mParty.deregisterPlaylistUpdateCallback(mCurrentSongUpdatedCallback);
    }
}
