package com.example.curate.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.curate.R;
import com.example.curate.models.Party;
import com.example.curate.service.PlayerResultReceiver;
import com.example.curate.utils.TrackProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.example.curate.service.PlayerResultReceiver.ACTION_DISCONNECT;
import static com.example.curate.service.PlayerResultReceiver.ACTION_PLAY_PAUSE;
import static com.example.curate.service.PlayerResultReceiver.ACTION_SKIP;
import static com.example.curate.service.PlayerResultReceiver.ARTIST_KEY;
import static com.example.curate.service.PlayerResultReceiver.DURATION_KEY;
import static com.example.curate.service.PlayerResultReceiver.IMAGE_KEY;
import static com.example.curate.service.PlayerResultReceiver.PAUSED_KEY;
import static com.example.curate.service.PlayerResultReceiver.PLAYBACK_POS_KEY;
import static com.example.curate.service.PlayerResultReceiver.RESULT_ALBUM_ART;
import static com.example.curate.service.PlayerResultReceiver.RESULT_INSTALL_SPOTIFY;
import static com.example.curate.service.PlayerResultReceiver.RESULT_NEW_SONG;
import static com.example.curate.service.PlayerResultReceiver.RESULT_OPEN_SPOTIFY;
import static com.example.curate.service.PlayerResultReceiver.RESULT_PLAYBACK;
import static com.example.curate.service.PlayerResultReceiver.RESULT_PLAY_PAUSE;
import static com.example.curate.service.PlayerResultReceiver.SONG_ID_KEY;
import static com.example.curate.service.PlayerResultReceiver.TITLE_KEY;
import static com.example.curate.service.PlayerResultReceiver.checkConnection;
import static com.example.curate.service.PlayerResultReceiver.enqueuePlayer;
import static com.example.curate.service.PlayerResultReceiver.playNew;
import static com.example.curate.service.PlayerResultReceiver.updatePlayer;

public class AdminPlayerFragment extends PlayerFragment implements PlayerResultReceiver.Receiver {
    private static final String TAG = "AdminPlayerFragment";
    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvArtist) TextView tvArtist;
    @BindView(R.id.ivAlbum) ImageView ivAlbum;
    @BindView(R.id.seek_to) SeekBar mSeekBar;
    @BindView(R.id.ivPlayPause) ImageView mPlayPauseButton;
    @BindView(R.id.clCurrPlaying) ConstraintLayout mPlayerBackground;
    @BindView(R.id.ivPrev) ImageView mSkipPrevButton;
    @BindView(R.id.ivNext) ImageView mSkipNextButton;
    @BindView(R.id.ibExpandCollapse) ImageButton ibExpandCollapse;


    private ConstraintSet mCollapsed;
    private ConstraintSet mExpanded;

    private Party mParty;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bottom_player_admin, container, false);
        ButterKnife.bind(this, view);

        mParty = Party.getCurrentParty();

        initFonts();
        setProgressBar();

        mCollapsed = new ConstraintSet();
        mCollapsed.clone(getContext(), R.layout.fragment_bottom_player_admin_collapsed);
        mExpanded = new ConstraintSet();
        mExpanded.clone(getContext(), R.layout.fragment_bottom_player_admin);
        setExpanded(false);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpService();
    }

    @Override
    public float getHeight() {
        return getResources().getDimension(isExpanded ? R.dimen.bottom_player_admin_height_expanded
                : R.dimen.bottom_player_admin_height_collapsed);
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

    private void setProgressBar() {
        mTrackProgressBar = new TrackProgressBar(this, mSeekBar);
        mSeekbarThumbDrawable = mSeekBar.getThumb();
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
        updatePlayer(getContext(), pos);
    }

    @OnClick(R.id.ivPrev)
    void onRestartSong() {
        onSeekTo(0);
    }

    @OnClick(R.id.ivPlayPause)
    void onPlayPause() {
        enqueuePlayer(getContext(), ACTION_PLAY_PAUSE);
    }

    @OnClick(R.id.ivNext)
    public void onSkipNext() {
        enqueuePlayer(getContext(), ACTION_SKIP);
    }

    public void onPlayNew(String spotifyId) {
        playNew(getContext(), spotifyId);
    }

    // PlayerService methods
    /**
     * Sets this fragment as a PlayerService receiver and enqueues an action to connect Spotify remote
     */
    private void setUpService() {
        PlayerResultReceiver playerResultReceiver = new PlayerResultReceiver(new Handler());
        playerResultReceiver.setReceiver(this);
        checkSpotifyInstalled();
        checkConnection(getContext());
    }
    /**
     * Method overwritten from the PlayerResultReceiver to receive results from the PlayerService.
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
                Glide.with(this).load(bitmap).into(ivAlbum);
            } else if (resultCode == RESULT_PLAYBACK) {
                mTrackProgressBar.update(resultData.getLong(PLAYBACK_POS_KEY));
                setPausedState(resultData.getBoolean(PAUSED_KEY));
            }
        } else if (resultCode == RESULT_INSTALL_SPOTIFY) {
            installSpotify();
        } else if (resultCode == RESULT_OPEN_SPOTIFY) {
            openSpotify();
        }
    }


    private void checkSpotifyInstalled() {
        PackageManager pm = getContext().getPackageManager();
        try {
            pm.getPackageInfo("com.spotify.music", 0);
            Log.d(TAG, "Spotify app is installed");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Spotify app is not installed");
            installSpotify();
        }
    }

    private void installSpotify() {
        final String appPackageName = "com.spotify.music";
        final String referrer = "adjust_campaign=PACKAGE_NAME&adjust_tracker=ndjczk&utm_source=adjust_preinstall";
        try {
            Uri uri = Uri.parse("market://details")
                    .buildUpon()
                    .appendQueryParameter("id", appPackageName)
                    .appendQueryParameter("referrer", referrer)
                    .build();
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (android.content.ActivityNotFoundException ignored) {
            Uri uri = Uri.parse("https://play.google.com/store/apps/details")
                    .buildUpon()
                    .appendQueryParameter("id", appPackageName)
                    .appendQueryParameter("referrer", referrer)
                    .build();
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }

    /**
     * Intent to open Spotify app in order to log in.
     */
    private void openSpotify() {
        Log.d(TAG, "opening spotify");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("spotify:track:" + getString(R.string.default_song_id)));
        intent.putExtra(Intent.EXTRA_REFERRER,
                Uri.parse("android-app://" + getContext().getPackageName()));
        startActivity(intent);
    }

    public static void disconnect(Context context) {
        PlayerResultReceiver.enqueuePlayer(context, ACTION_DISCONNECT);
    }
}
