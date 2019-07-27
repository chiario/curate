package com.example.curate.fragments;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.models.Party;
import com.example.curate.models.Song;
import com.parse.ParseException;
import com.parse.SaveCallback;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.graphics.Typeface.BOLD;
import static android.graphics.Typeface.NORMAL;

public class BottomPlayerFragment extends Fragment {
    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvArtist) TextView tvArtist;
    @BindView(R.id.ivAlbum) ImageView ivAlbum;
    @BindView(R.id.clCurrPlaying) ConstraintLayout mPlayerBackground;
    @BindView(R.id.ibExpandCollapse) ImageButton ibExpandCollapse;
    @BindView(R.id.ibShare) ImageButton ibShare;

    private SaveCallback mCurrentSongUpdatedCallback;
    private Party mParty;

    private ConstraintSet mCollapsed;
    private ConstraintSet mExpanded;

    private String mTrackName = "--";
    private String mArtistName = "--";
    private boolean isExpanded;
    private Typeface mBoldFont;
    private Typeface mNormalFont;

    public BottomPlayerFragment() {
        // Required empty public constructor
    }

    public static BottomPlayerFragment newInstance() {
        BottomPlayerFragment fragment = new BottomPlayerFragment();
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
            ibShare.setVisibility(View.VISIBLE);
            params.height = Math.round(getResources().getDimension(R.dimen.bottom_player_client_height_expanded));
            ibExpandCollapse.setSelected(true);
            mPlayerBackground.setLayoutParams(params);
        }
        else {
            mCollapsed.applyTo(mPlayerBackground);
            ivAlbum.setVisibility(View.GONE);
            ibShare.setVisibility(View.GONE);
            params.height = Math.round(getResources().getDimension(R.dimen.bottom_player_client_height_collapsed));
            ibExpandCollapse.setSelected(false);
            mPlayerBackground.setLayoutParams(params);
        }
        updateText();
    }

    private void initFonts() {
        mBoldFont = Typeface.create(ResourcesCompat.getFont(getContext(), R.font.nunito), BOLD);
        mNormalFont = Typeface.create(ResourcesCompat.getFont(getContext(), R.font.nunito), NORMAL);
    }

    private void updateText() {
        int flag = SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE;
        if(isExpanded) {
            SpannableStringBuilder builder = new SpannableStringBuilder(mTrackName);
            builder.setSpan(new TypefaceSpan(mBoldFont), 0, builder.length(), flag);
            tvTitle.setText(builder);
            tvArtist.setText(mArtistName);
            tvArtist.setVisibility(View.VISIBLE);
        }
        else {
            tvTitle.setSelected(true);
            SpannableString title = new SpannableString(String.format("%s - ", mTrackName));
            SpannableString artist = new SpannableString(mArtistName);
            title.setSpan(new TypefaceSpan(mBoldFont), 0, title.length(), flag);
            artist.setSpan(new TypefaceSpan(mNormalFont), 0, artist.length(), flag);
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(title);
            builder.append(artist);
            tvTitle.setText(builder);
            tvArtist.setVisibility(View.INVISIBLE);
        }
    }

    @OnClick(R.id.ibExpandCollapse)
    public void onClickExpandCollapse() {
        setExpanded(!ibExpandCollapse.isSelected());
    }

    @OnClick(R.id.clCurrPlaying)
    public void onClickClCurrPlaying() {
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

        initFonts();
        initializeSongUpdateCallback();
        mCollapsed = new ConstraintSet();
        mCollapsed.clone(getContext(), R.layout.fragment_bottom_player_client_collapsed);
        mExpanded = new ConstraintSet();
        mExpanded.clone(getContext(), R.layout.fragment_bottom_player_client);
        setExpanded(false);
        return view;
    }

    @OnClick(R.id.ibShare)
    public void onClickShare() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "https://open.spotify.com/track/" + Party.getCurrentParty().getCurrentSong().getSpotifyId());
        startActivity(Intent.createChooser(intent, "Share this song!"));
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
        mCurrentSongUpdatedCallback.done(null);
    }

    private void removeSongUpdateCallback() {
        mParty.deregisterPlaylistUpdateCallback(mCurrentSongUpdatedCallback);
    }
}
