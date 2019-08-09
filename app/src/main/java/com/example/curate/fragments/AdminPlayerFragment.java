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
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
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
import static com.example.curate.service.PlayerResultReceiver.TITLE_KEY;
import static com.example.curate.service.PlayerResultReceiver.enqueueService;
import static com.example.curate.service.PlayerResultReceiver.initService;
import static com.example.curate.service.PlayerResultReceiver.playNew;
import static com.example.curate.service.PlayerResultReceiver.updatePlayer;

public class AdminPlayerFragment extends PlayerFragment implements PlayerResultReceiver.Receiver {
    private static final String TAG = "AdminPlayerFragment";
    private static final int SPOTIFY_INTENT_CODE = 3;

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
    public void onStart() {
        super.onStart();
        if (isSpotifyInstalled(true)) {
            connectService();
        }
    }

    @Override
    public float getHeight() {
        return getResources().getDimension(isExpanded ? R.dimen.bottom_player_admin_height_expanded
                : R.dimen.bottom_player_admin_height_collapsed);
    }

    @Override
    public void setExpanded(boolean isExpanded) {
        ViewGroup.LayoutParams params = mPlayerBackground.getLayoutParams();
        this.isExpanded = isExpanded;
        if(isExpanded) {
            mExpanded.applyTo(mPlayerBackground);
            params.height = Math.round(getResources().getDimension(R.dimen.bottom_player_admin_height_expanded));
            mPlayerBackground.setLayoutParams(params);
            setVisibility(View.VISIBLE);
            setSeekbar(true);
            setButtonVisibility(View.VISIBLE);
            ibExpandCollapse.setSelected(true);
        } else {
            mCollapsed.applyTo(mPlayerBackground);
            params.height = Math.round(getResources().getDimension(R.dimen.bottom_player_admin_height_collapsed));
            setVisibility(View.GONE);
            setSeekbar(false);
            setButtonVisibility(View.INVISIBLE);
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

    // Methods to update current song details

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

    private void updateSong(Bundle songData) {
        mTrackProgressBar.setDuration(songData.getLong(DURATION_KEY));
        mTrackProgressBar.update(songData.getLong(PLAYBACK_POS_KEY));
        setTrackDetails(songData.getString(TITLE_KEY), songData.getString(ARTIST_KEY));
        setPausedState(songData.getBoolean(PAUSED_KEY));
    }

    private void loadAlbumArt(byte[] byteArray) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        Glide.with(this).load(bitmap).into(ivAlbum);
    }

    // Player functionality methods

    public void onSeekTo(long pos) {
        updatePlayer(getContext(), pos);
    }

    @OnClick(R.id.ivPrev)
    void onRestartSong() {
        onSeekTo(0);
    }

    @OnClick(R.id.ivPlayPause)
    void onPlayPause() {
        enqueueService(getContext(), ACTION_PLAY_PAUSE);
    }

    @OnClick(R.id.ivNext)
    public void onSkipNext() {
        enqueueService(getContext(), ACTION_SKIP);
    }

    public void onPlayNew(String spotifyId) {
        playNew(getContext(), spotifyId);
    }

    // PlayerService methods

    private void connectService() {
        // Sets up receiver so that this fragment can receive results from the service
        PlayerResultReceiver playerResultReceiver = new PlayerResultReceiver(new Handler());
        playerResultReceiver.setReceiver(this);
        // Initialize the PlayerService
        initService(getContext());
    }

    public static void disconnectService(Context context) {
        PlayerResultReceiver.enqueueService(context, ACTION_DISCONNECT);
    }

    /**
     * Method overwritten from the receiver interface to receive results from the PlayerService.
     */
    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        Log.d(TAG, "Received result code " + resultCode + " with data " + resultData);
        if (resultCode == RESULT_INSTALL_SPOTIFY) {
            installSpotify();
        } else if (resultCode == RESULT_OPEN_SPOTIFY) {
            openSpotify();
        } else if (resultData != null) {
            switch (resultCode) {
                case RESULT_PLAY_PAUSE:
                    setPausedState(resultData.getBoolean(PAUSED_KEY));
                    break;
                case RESULT_NEW_SONG:
                    updateSong(resultData);
                    break;
                case RESULT_ALBUM_ART:
                    loadAlbumArt(resultData.getByteArray(IMAGE_KEY));
                    break;
                case RESULT_PLAYBACK:
                    mTrackProgressBar.update(resultData.getLong(PLAYBACK_POS_KEY));
                    setPausedState(resultData.getBoolean(PAUSED_KEY));
                    break;
            }
        }
    }

    // Spotify methods

    private boolean isSpotifyInstalled(boolean promptInstallation) {
        PackageManager pm = getContext().getPackageManager();
        try {
            pm.getPackageInfo("com.spotify.music", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            if (promptInstallation) {
                installSpotify();
            }
            return false;
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
            startActivityForResult(new Intent(Intent.ACTION_VIEW, uri), SPOTIFY_INTENT_CODE);
        } catch (android.content.ActivityNotFoundException ignored) {
            Uri uri = Uri.parse("https://play.google.com/store/apps/details")
                    .buildUpon()
                    .appendQueryParameter("id", appPackageName)
                    .appendQueryParameter("referrer", referrer)
                    .build();
            startActivityForResult(new Intent(Intent.ACTION_VIEW, uri), SPOTIFY_INTENT_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPOTIFY_INTENT_CODE) {
            // Check if Spotify app has been installed but don't start new installation activity
            if (isSpotifyInstalled(false)) {
                connectService();
            }
        }
    }

    private void openSpotify() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("spotify:track:" + getString(R.string.default_song_id)));
        intent.putExtra(Intent.EXTRA_REFERRER,
                Uri.parse("android-app://" + getContext().getPackageName()));
        startActivity(intent);
    }


}
