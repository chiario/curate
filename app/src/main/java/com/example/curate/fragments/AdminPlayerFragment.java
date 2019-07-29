package com.example.curate.fragments;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
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
import androidx.core.content.ContextCompat;

import com.example.curate.PlayerResultReceiver;
import com.example.curate.R;
import com.example.curate.TrackProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.example.curate.ServiceUtils.ACTION_INIT;
import static com.example.curate.ServiceUtils.ACTION_PLAY;
import static com.example.curate.ServiceUtils.ACTION_PLAY_PAUSE;
import static com.example.curate.ServiceUtils.ACTION_SKIP;
import static com.example.curate.ServiceUtils.ACTION_UPDATE;
import static com.example.curate.ServiceUtils.ARTIST_KEY;
import static com.example.curate.ServiceUtils.DURATION_KEY;
import static com.example.curate.ServiceUtils.IMAGE_KEY;
import static com.example.curate.ServiceUtils.PAUSED_KEY;
import static com.example.curate.ServiceUtils.PLAYBACK_POS_KEY;
import static com.example.curate.ServiceUtils.RESULT_ALBUM_ART;
import static com.example.curate.ServiceUtils.RESULT_NEW_SONG;
import static com.example.curate.ServiceUtils.RESULT_PLAYBACK;
import static com.example.curate.ServiceUtils.RESULT_PLAY_PAUSE;
import static com.example.curate.ServiceUtils.SONG_ID_KEY;
import static com.example.curate.ServiceUtils.TITLE_KEY;
import static com.example.curate.ServiceUtils.enqueuePlayer;

public class AdminPlayerFragment extends PlayerFragment implements PlayerResultReceiver.Receiver {
    private static final String TAG = "BottomPlayerAdmin";
    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvArtist) TextView tvArtist;
    @BindView(R.id.ivAlbum) ImageView ivAlbum;
    @BindView(R.id.seek_to) SeekBar mSeekBar;
    @BindView(R.id.ivPlayPause) ImageView mPlayPauseButton;
    @BindView(R.id.clCurrPlaying) ConstraintLayout mPlayerBackground;
    @BindView(R.id.ivPrev) ImageView mSkipPrevButton;
    @BindView(R.id.ivNext) ImageView mSkipNextButton;
    @BindView(R.id.ibExpandCollapse) ImageButton ibExpandCollapse;
    @BindView(R.id.ibShare) ImageButton ibShare;

    private ConstraintSet mCollapsed;
    private ConstraintSet mExpanded;

    private TrackProgressBar mTrackProgressBar;
    private Drawable mSeekbarThumbDrawable;
    private PlayerResultReceiver mPlayerResultReceiver;

    public AdminPlayerFragment() {
        // Required empty public constructor
    }

    public static AdminPlayerFragment newInstance() {
        AdminPlayerFragment fragment = new AdminPlayerFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpService();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bottom_player_admin, container, false);
        ButterKnife.bind(this, view);

        mTrackProgressBar = new TrackProgressBar(this, mSeekBar);

        initFonts();

        mCollapsed = new ConstraintSet();
        mCollapsed.clone(getContext(), R.layout.fragment_bottom_player_admin_collapsed);
        mExpanded = new ConstraintSet();
        mExpanded.clone(getContext(), R.layout.fragment_bottom_player_admin);
        mSeekbarThumbDrawable = mSeekBar.getThumb();
        setExpanded(false);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void setExpanded(boolean isExpanded) {
        ViewGroup.LayoutParams params = mPlayerBackground.getLayoutParams();
        this.isExpanded = isExpanded;
        if(isExpanded) {
            mExpanded.applyTo(mPlayerBackground);
            params.height = Math.round(getResources().getDimension(R.dimen.bottom_player_admin_height_expanded)); //new
            mPlayerBackground.setLayoutParams(params);
            setVisibility(View.VISIBLE);
            setSeekbar(true); //new
            setButtonVisibility(View.VISIBLE);//new
            ibExpandCollapse.setSelected(true);
        } else {
            mCollapsed.applyTo(mPlayerBackground);
            params.height = Math.round(getResources().getDimension(R.dimen.bottom_player_admin_height_collapsed)); //new
            setVisibility(View.GONE);
            setSeekbar(false); //new
            setButtonVisibility(View.INVISIBLE); //new
            ibExpandCollapse.setSelected(false);
            mPlayerBackground.setLayoutParams(params);
        }
        updateText();
    }

    private void setButtonVisibility(int visibility) {
        mSkipNextButton.setVisibility(visibility);
        mSkipPrevButton.setVisibility(visibility);
    }

    private void setSeekbar(final boolean isExpanded) {
        // Disable touch
        mSeekBar.setClickable(true);
        mSeekBar.setOnTouchListener((view, motionEvent) -> !isExpanded);

        if(isExpanded) {
            // Show the thumb
            mSeekBar.setThumb(mSeekbarThumbDrawable);
            int sidePadding = (int) dpToPx(16f);
            mSeekBar.setPadding(sidePadding, 0, sidePadding, 0);
        } else {
            // Hide the thumb
            mSeekBar.setThumb(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.transparent)));
            mSeekBar.setPadding(0, 0, 0, 0);
        }
    }

    private float dpToPx(float dip) {
        Resources r = getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }

    private void setPausedState(boolean isPaused) {
        if (isPaused) {
            mPlayPauseButton.setImageResource(R.drawable.ic_play);
            mTrackProgressBar.pause();
        } else {
            mPlayPauseButton.setImageResource(R.drawable.ic_pause);
            mTrackProgressBar.unpause();
        }
    }

    private void setTrackDetails(String trackName, String artistName) {
        mTrackName = trackName;
        mArtistName = artistName;
        updateText();
    }

    public void onSeekTo(long pos) {
        Bundle bundle = new Bundle();
        bundle.putLong(PLAYBACK_POS_KEY, pos);
        enqueuePlayer(getContext(), mPlayerResultReceiver, ACTION_UPDATE, bundle);
    }

    @OnClick(R.id.ivPrev)
    void onRestartSong() {
        onSeekTo(0);
    }

    @OnClick(R.id.ivPlayPause)
    void onPlayPause() {
        enqueuePlayer(getContext(), mPlayerResultReceiver, ACTION_PLAY_PAUSE, null);
    }

    @OnClick(R.id.ivNext)
    public void onSkipNext() {
        enqueuePlayer(getContext(), mPlayerResultReceiver, ACTION_SKIP, null);
    }

    public void onPlayNew(String spotifyId) {
        Bundle bundle = new Bundle();
        bundle.putString(SONG_ID_KEY, spotifyId);
        enqueuePlayer(getContext(), mPlayerResultReceiver, ACTION_PLAY, bundle);
    }

    // PlayerService methods
    /**
     * This function sets this fragment as a new receiver for the PlayerService
     */
    private void setUpService() {
        mPlayerResultReceiver = new PlayerResultReceiver(new Handler());
        mPlayerResultReceiver.setReceiver(this);
        // This INIT call effectively creates the service by spawning a new worker thread
        enqueuePlayer(getContext(), mPlayerResultReceiver, ACTION_INIT, null);
    }
    /**
     * Method overwritten from the PlayerResultReceiver.
     * Receives results from the PlayerService.
     * @param resultCode determines type of result passed from PlayerService
     * @param resultData data bundle passed from PlayerService
     */
    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        Log.d(TAG, "Received result code " + resultCode + " with data " + resultData);
        if (resultData != null) {
            if (resultCode == RESULT_PLAY_PAUSE) {
                setPausedState(resultData.getBoolean(PAUSED_KEY));
            } else if (resultCode == RESULT_NEW_SONG) {
                String newSongId = resultData.getString(SONG_ID_KEY);
                // Update song information
                mTrackProgressBar.setDuration(resultData.getLong(DURATION_KEY));
                mTrackProgressBar.update(resultData.getLong(PLAYBACK_POS_KEY));
                setTrackDetails(resultData.getString(TITLE_KEY), resultData.getString(ARTIST_KEY));
                setPausedState(resultData.getBoolean(PAUSED_KEY));
            } else if (resultCode == RESULT_ALBUM_ART) {
                // Decode byte array into bitmap
                byte[] byteArray = resultData.getByteArray(IMAGE_KEY);
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                ivAlbum.setImageBitmap(bitmap);
            } else if (resultCode == RESULT_PLAYBACK) {
                mTrackProgressBar.update(resultData.getLong(PLAYBACK_POS_KEY));
                setPausedState(resultData.getBoolean(PAUSED_KEY));
            }
        }
    }
}
