package com.example.curate.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.fonts.Font;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
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
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.example.curate.PlayerResultReceiver;
import com.example.curate.PlayerService;
import com.example.curate.R;
import com.example.curate.models.Party;
import com.example.curate.utils.LocationManager;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.graphics.Typeface.BOLD;
import static android.graphics.Typeface.NORMAL;
import static com.example.curate.PlayerService.ACTION_INIT;
import static com.example.curate.PlayerService.ACTION_UPDATE;
import static com.example.curate.PlayerService.ARTIST_KEY;
import static com.example.curate.PlayerService.DURATION_KEY;
import static com.example.curate.PlayerService.IMAGE_KEY;
import static com.example.curate.PlayerService.PAUSED_KEY;
import static com.example.curate.PlayerService.PLAYBACK_POS_KEY;
import static com.example.curate.PlayerService.RESULT_ALBUM_ART;
import static com.example.curate.PlayerService.RESULT_NEW_SONG;
import static com.example.curate.PlayerService.RESULT_PLAY_PAUSE;
import static com.example.curate.PlayerService.RESULT_SEEK;
import static com.example.curate.PlayerService.TITLE_KEY;

public class BottomPlayerAdminFragment extends Fragment implements PlayerResultReceiver.Receiver {
    private static final String TAG = "BottomPlayerAdmin";
    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvArtist) TextView tvArtist;
    @BindView(R.id.ivAlbum) ImageView ivAlbum;
    @BindView(R.id.seek_to) SeekBar mSeekBar;
    @BindView(R.id.play_pause_button) ImageView mPlayPauseButton;
    @BindView(R.id.clCurrPlaying) ConstraintLayout mPlayerBackground;
    @BindView(R.id.skip_prev_button) ImageView mSkipPrevButton;
    @BindView(R.id.skip_next_button) ImageView mSkipNextButton;
    @BindView(R.id.ibExpandCollapse) ImageButton ibExpandCollapse;
    @BindView(R.id.ibShare) ImageButton ibShare;

    private ConstraintSet mCollapsed;
    private ConstraintSet mExpanded;

    private LocationManager mLocationManager;
    private TrackProgressBar mTrackProgressBar;

    private String mTrackName = "--";
    private String mArtistName = "--";
    private boolean isExpanded;
    private Drawable mSeekbarThumbDrawable;
    private Typeface mBoldFont;
    private Typeface mNormalFont;

    private LocationCallback mLocationCallback = null;

    public PlayerResultReceiver mPlayerResultReceiver;

    public BottomPlayerAdminFragment() {
        // Required empty public constructor
    }

    public static BottomPlayerAdminFragment newInstance() {
        BottomPlayerAdminFragment fragment = new BottomPlayerAdminFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpService();
    }

    /**
     * This function sets this fragment as a new Receiver for the PlayerService
     */
    private void setUpService() {
        mPlayerResultReceiver = new PlayerResultReceiver(new Handler());
        mPlayerResultReceiver.setReceiver(this);
        // This INIT call effectively creates the service by spawning a new worker thread
        PlayerService.enqueueWork(getContext(), mPlayerResultReceiver, ACTION_INIT, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bottom_player_admin, container, false);
        ButterKnife.bind(this, view);

        mTrackProgressBar = new TrackProgressBar(mSeekBar);

        mLocationManager = new LocationManager(getContext());
        if(mLocationManager.hasNecessaryPermissions() && Party.getLocationEnabled()) {
            registerLocationUpdater();
        } else {
            mLocationManager.requestPermissions();
        }


        initFonts();

        mCollapsed = new ConstraintSet();
        mCollapsed.clone(getContext(), R.layout.fragment_bottom_player_admin_collapsed);
        mExpanded = new ConstraintSet();
        mExpanded.clone(getContext(), R.layout.fragment_bottom_player_admin);
        mSeekbarThumbDrawable = mSeekBar.getThumb();
        setExpanded(false);

        return view;
    }

    /*@Override
    public void onResume() {
        super.onResume();
        mSpotifyPlayer.checkSpotifyInstalled();
    }*/


    /**
     * Updates the play/pause button to the correct drawable and sets the play/pause state for
     * the TrackProgressBar based on the input
     * @param isPaused
     */
    private void setPaused(boolean isPaused) {
        if (isPaused) {
            mPlayPauseButton.setImageResource(R.drawable.btn_play);
            mTrackProgressBar.pause();
        } else {
            mPlayPauseButton.setImageResource(R.drawable.btn_pause);
            mTrackProgressBar.unpause();
        }
    }

    /**
     * Updates the track and artist name in the view
     * @param trackName name of currently playing track
     * @param artistName name of current track's artist
     */
    private void setTrackDetails(String trackName, String artistName) {
        mTrackName = trackName;
        mArtistName = artistName;
        updateText();
    }


    @OnClick(R.id.skip_prev_button)
    public void onRestartSong() {
        Bundle bundle = new Bundle();
        bundle.putLong(PLAYBACK_POS_KEY, 0);
        PlayerService.enqueueWork(getContext(), mPlayerResultReceiver, PlayerService.ACTION_UPDATE, bundle);
    }

    @OnClick(R.id.play_pause_button)
    public void onPlayPause() {
        PlayerService.enqueueWork(getContext(), mPlayerResultReceiver, PlayerService.ACTION_PLAY_PAUSE, null);
    }

    @OnClick(R.id.skip_next_button)
    public void onSkipNext() {
        PlayerService.enqueueWork(getContext(), mPlayerResultReceiver, PlayerService.ACTION_SKIP, null);
    }

    @OnClick(R.id.ibShare)
    public void onClickShare() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        // TODO Cache the Song in bottom player fragment.
        intent.putExtra(Intent.EXTRA_TEXT, "https://open.spotify.com/track/" + Party.getCurrentParty().getCurrentSong().getSpotifyId());
        startActivity(Intent.createChooser(intent, "Share this song!"));
    }

    private void setExpanded(boolean isExpanded) {
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
        }
        else {
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
            // TODO: Change font here
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

    private float dpToPx(float dip) {
        Resources r = getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }

    private void setVisibility(int visibility) {
        ivAlbum.setVisibility(visibility);
        ibShare.setVisibility(visibility);
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
        }
        else {
            // Hide the thumb
            mSeekBar.setThumb(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.transparent)));
            mSeekBar.setPadding(0, 0, 0, 0);
        }
    }

    private void setButtonVisibility(int visibility) {
        mSkipNextButton.setVisibility(visibility);
        mSkipPrevButton.setVisibility(visibility);
    }

    @OnClick(R.id.clCurrPlaying)
    public void onClickClCurrPlaying() {
        setExpanded(!ibExpandCollapse.isSelected());
    }

    @OnClick(R.id.ibExpandCollapse)
    public void onClickExpandCollapse() {
        setExpanded(!ibExpandCollapse.isSelected());
    }

    // TODO - Should this be in here??
    public void registerLocationUpdater() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Party.getCurrentParty().updatePartyLocation(LocationManager.createGeoPointFromLocation(locationResult.getLastLocation()), e -> {
                    if (e == null) {
                        Log.d("MainActivity", "Party location updated!");
                    }
                    else {
                        Log.e("MainActivity", "yike couldnt update party location!", e);
                    }
                });
            }
        };

        // Update location when user moves
        mLocationManager.registerLocationUpdateCallback(mLocationCallback);

        // Force update location at least once
        mLocationManager.getCurrentLocation(location -> {
            if(location != null) {
                Party.getCurrentParty().updatePartyLocation(LocationManager.createGeoPointFromLocation(location), e -> {
                    if (e == null) {
                        Log.d("MainActivity", "Party location updated!");
                    }
                    else {
                        Log.e("MainActivity", "yike couldnt update party location!", e);
                    }
                });
            }
        });
    }

    public void deregisterLocationUpdater() {
        if(mLocationCallback == null) return;
        mLocationManager.deregisterLocationUpdateCallback(mLocationCallback);
        Party.clearLocation(e -> {
            if(e == null) {
                mLocationCallback = null;
            } else {
                registerLocationUpdater();
                Log.e("MainActivity", "Couldn't clear location!", e);
            }
        }); // TODO - fix this synchronization error (The methods in LocationCallback are never called so we can't clear this in the callback?)
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == LocationManager.PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && Party.getLocationEnabled()) {
                // Location permission has been granted, register location updater
                registerLocationUpdater();
            }
            else {
                Log.i("AdminManager", "Location permission was not granted.");
            }
        }
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
        if (resultCode == RESULT_PLAY_PAUSE) {
            if (resultData != null) {
                boolean isPaused = resultData.getBoolean(PAUSED_KEY);
                setPaused(isPaused);

            }
        } else if (resultCode == RESULT_NEW_SONG) {
            if (resultData != null) {
                //String newSongId = resultData.getString(SONG_ID_KEY);
                long newDuration = resultData.getLong(DURATION_KEY);
                String newTitle = resultData.getString(TITLE_KEY);
                String newArtist = resultData.getString(ARTIST_KEY);
                long playbackPos = resultData.getLong(PLAYBACK_POS_KEY);
                boolean isPaused = resultData.getBoolean(PAUSED_KEY);
                //TODO - update song information
                mTrackProgressBar.setDuration(newDuration);
                mTrackProgressBar.update(playbackPos);
                setTrackDetails(newTitle, newArtist);
                setPaused(isPaused);
            }
        } else if (resultCode == RESULT_ALBUM_ART) {
            if (resultData != null) {
                // Decode byte array into bitmap
                byte[] byteArray = resultData.getByteArray(IMAGE_KEY);
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                ivAlbum.setImageBitmap(bitmap);
            }
        } else if (resultCode == RESULT_SEEK) {
            if (resultData != null) {
                long seekTo = resultData.getLong(PLAYBACK_POS_KEY);
                boolean isPaused = resultData.getBoolean(PAUSED_KEY);
                mTrackProgressBar.update(seekTo);
                setPaused(isPaused);
            }
        }
    }

    public class TrackProgressBar {
        private static final int NEXT_SONG_DELAY = 1000;
        private static final int LOOP_DURATION = 500;
        private SeekBar mSeekBar;
        private Handler mHandler;
        private boolean mIsBeingTouched;

        public TrackProgressBar(SeekBar seekBar) {
            mSeekBar = seekBar;
            mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
            mHandler = new Handler();
            mIsBeingTouched = false;
        }

        private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener
                = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                /*int timeRemaining = mSeekBar.getMax() - progress;
                if (timeRemaining < NEXT_SONG_DELAY && !mIsBeingTouched) {
                    mHandler.removeCallbacks(mSeekRunnable);
                    playNextSong();
                }*/
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsBeingTouched = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long timeRemaining = mSeekBar.getMax() - seekBar.getProgress();
                long progress = timeRemaining > NEXT_SONG_DELAY ? seekBar.getProgress() : mSeekBar.getMax() - NEXT_SONG_DELAY;

                // Alert service to update playback position
                Bundle bundle = new Bundle();
                bundle.putLong(PLAYBACK_POS_KEY, progress);
                PlayerService.enqueueWork(getContext(), mPlayerResultReceiver, ACTION_UPDATE, bundle);
                mIsBeingTouched = false; // TODO
            }
        };

        private final Runnable mSeekRunnable = new Runnable() {
            @Override
            public void run() {
                int progress = mSeekBar.getProgress();
                mSeekBar.setProgress(progress + LOOP_DURATION);
                mHandler.postDelayed(mSeekRunnable, LOOP_DURATION);
            }
        };

        public void setDuration(long duration) {
            mSeekBar.setMax((int) duration);
        }

        public void update(long progress) {
            mSeekBar.setProgress((int) progress);
            mHandler.removeCallbacks(mSeekRunnable);
            mHandler.postDelayed(mSeekRunnable, LOOP_DURATION);
        }

        public void pause() {
            mHandler.removeCallbacks(mSeekRunnable);
        }

        public void unpause() {
            mHandler.removeCallbacks(mSeekRunnable);
            mHandler.postDelayed(mSeekRunnable, LOOP_DURATION);
        }

        public void setEnabled(boolean isEnabled) {
            mSeekBar.setEnabled(isEnabled);
        }
    }

}
