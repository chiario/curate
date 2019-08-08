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

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
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

public class PlayerFragment extends Fragment {
    private static final String TAG = "PlayerFragment";
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

    static String mTrackName = "--";
    static String mArtistName = "--";
    boolean isExpanded;
    private Typeface mBoldFont;
    private Typeface mNormalFont;

    public PlayerFragment() {
        // Required empty public constructor
    }

    public static PlayerFragment newInstance() {
        PlayerFragment fragment = new PlayerFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
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

    @Override
    public void onDestroy() {
        removeSongUpdateCallback();
        super.onDestroy();
    }

    public float getHeight() {
        return getResources().getDimension(isExpanded ? R.dimen.bottom_player_client_height_expanded
                : R.dimen.bottom_player_client_height_collapsed);
    }

    /***
     * Set the bottom players expanded state
     * @param isExpanded The new state to be in
     */
    public void setExpanded(boolean isExpanded) {
        this.isExpanded = isExpanded;
        ViewGroup.LayoutParams params = mPlayerBackground.getLayoutParams();
        if(isExpanded) {
            mExpanded.applyTo(mPlayerBackground);
            setVisibility(View.VISIBLE);
            params.height = Math.round(getResources().getDimension(R.dimen.bottom_player_client_height_expanded));
            ibExpandCollapse.setSelected(true);
            mPlayerBackground.setLayoutParams(params);
        } else {
            mCollapsed.applyTo(mPlayerBackground);
            setVisibility(View.GONE);
            params.height = Math.round(getResources().getDimension(R.dimen.bottom_player_client_height_collapsed));
            ibExpandCollapse.setSelected(false);
            mPlayerBackground.setLayoutParams(params);
        }
        updateText();
    }

    public void setVisibility(int visibility) {
        ivAlbum.setVisibility(visibility);
        ibShare.setVisibility(visibility);
    }

    void initFonts() {
        mBoldFont = Typeface.create(ResourcesCompat.getFont(getContext(), R.font.nunito), BOLD);
        mNormalFont = Typeface.create(ResourcesCompat.getFont(getContext(), R.font.nunito), NORMAL);
    }

    void updateText() {
        int flag = SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE;
        if(isExpanded) {
            SpannableStringBuilder builder = new SpannableStringBuilder(mTrackName);
            builder.setSpan(new TypefaceSpan(mBoldFont), 0, builder.length(), flag);
            tvTitle.setText(builder);
            tvArtist.setText(mArtistName);
            tvArtist.setVisibility(View.VISIBLE);
        } else {
            // TODO: Change font here
            tvTitle.setSelected(true);
            SpannableString title = new SpannableString(String.format("%s · ", mTrackName));
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
    void onClickExpandCollapse() {
        setExpanded(!ibExpandCollapse.isSelected());
    }

    @OnClick(R.id.clCurrPlaying)
    void onClickClCurrPlaying() {
        setExpanded(!ibExpandCollapse.isSelected());
    }

    @OnClick(R.id.ibShare)
    void onClickShare() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "https://open.spotify.com/track/" + Party.getCurrentParty().getCurrentSong().getSpotifyId());
        startActivity(Intent.createChooser(intent, "Share this song!"));
    }

    // Song update callback methods are only called in client fragment
    private void initializeSongUpdateCallback() {
        Log.d(TAG, "Initializing song update callback");
        mCurrentSongUpdatedCallback = e -> {
            if (e == null) {
                Song currentSong = mParty.getCurrentSong();
                if (currentSong != null) {
                    try {
                        currentSong.fetchIfNeeded(); // TODO - work around this fetch; add ParseCloud function??
                        mTrackName = currentSong.getTitle();
                        mArtistName = currentSong.getArtist();
                        getActivity().runOnUiThread(() -> {
                            updateText();
                            int radius = (int) getResources().getDimension(R.dimen.button_radius);
                            Log.i("tag", "Radius: " + radius);
                            Glide.with(this)
                                    .load(currentSong.getImageUrl())
                                    .into(ivAlbum);
                        });
                    } catch (ParseException e1) {
                        e1.printStackTrace();
                    }
                }
            } else {
                Log.d(TAG, "Error in song update callback", e);
            }
        };
        mParty.registerCurrentlyPlayingUpdateCallback(mCurrentSongUpdatedCallback);
        mCurrentSongUpdatedCallback.done(null);
    }

    private void removeSongUpdateCallback() {
        if (mParty != null) {
            mParty.deregisterCurrentlyPlayingUpdateCallback(mCurrentSongUpdatedCallback);
        }
    }
}
