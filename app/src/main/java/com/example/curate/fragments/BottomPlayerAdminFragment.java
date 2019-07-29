package com.example.curate.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
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
import com.example.curate.R;
import com.example.curate.TrackProgressBar;
import com.example.curate.models.Party;
import com.example.curate.utils.LocationManager;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.graphics.Typeface.BOLD;
import static android.graphics.Typeface.NORMAL;
import static com.example.curate.ServiceUtils.ACTION_INIT;
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

public class BottomPlayerAdminFragment extends Fragment implements PlayerResultReceiver.Receiver {
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

    private LocationManager mLocationManager;
    private TrackProgressBar mTrackProgressBar;

    private String mTrackName = "--";
    private String mArtistName = "--";
    private boolean isExpanded;
    private Drawable mSeekbarThumbDrawable;
    private Typeface mBoldFont;
    private Typeface mNormalFont;

    private LocationCallback mLocationCallback = null;
    public static PlayerResultReceiver mPlayerResultReceiver;

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
        enqueuePlayer(getContext(), mPlayerResultReceiver, ACTION_INIT, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bottom_player_admin, container, false);
        ButterKnife.bind(this, view);

        mTrackProgressBar = new TrackProgressBar(this, mSeekBar);

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

    @Override
    public void onResume() {
        super.onResume();
        setUpService();
    }


    /**
     * Updates the play/pause button to the correct drawable and sets the play/pause state for
     * the TrackProgressBar based on the input
     * @param isPaused
     */
    private void setPaused(boolean isPaused) {
        if (isPaused) {
            mPlayPauseButton.setImageResource(R.drawable.ic_play);
            mTrackProgressBar.pause();
        } else {
            mPlayPauseButton.setImageResource(R.drawable.ic_pause);
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


    @OnClick(R.id.ivPrev)
    public void onRestartSong() {
        onSeekTo(0);
    }

    public void onSeekTo(long pos) {
        Bundle bundle = new Bundle();
        bundle.putLong(PLAYBACK_POS_KEY, pos);
        enqueuePlayer(getContext(), mPlayerResultReceiver, ACTION_UPDATE, bundle);
    }

    @OnClick(R.id.ivPlayPause)
    public void onPlayPause() {
        enqueuePlayer(getContext(), mPlayerResultReceiver, ACTION_PLAY_PAUSE, null);
    }

    @OnClick(R.id.ivNext)
    public void onSkipNext() {
        enqueuePlayer(getContext(), mPlayerResultReceiver, ACTION_SKIP, null);
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
        } else {
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
        } else {
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
                    } else {
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
                    } else {
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
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == LocationManager.PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && Party.getLocationEnabled()) {
                // Location permission has been granted, register location updater
                registerLocationUpdater();
            } else {
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
        if (resultData != null) {
            if (resultCode == RESULT_PLAY_PAUSE) {
                setPaused(resultData.getBoolean(PAUSED_KEY));
            } else if (resultCode == RESULT_NEW_SONG) {
                String newSongId = resultData.getString(SONG_ID_KEY);
                // Update song information
                mTrackProgressBar.setDuration(resultData.getLong(DURATION_KEY));
                mTrackProgressBar.update(resultData.getLong(PLAYBACK_POS_KEY));
                setTrackDetails(resultData.getString(TITLE_KEY), resultData.getString(ARTIST_KEY));
                setPaused(resultData.getBoolean(PAUSED_KEY));
            } else if (resultCode == RESULT_ALBUM_ART) {
                // Decode byte array into bitmap
                byte[] byteArray = resultData.getByteArray(IMAGE_KEY);
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                ivAlbum.setImageBitmap(bitmap);
            } else if (resultCode == RESULT_PLAYBACK) {
                mTrackProgressBar.update(resultData.getLong(PLAYBACK_POS_KEY));
                setPaused(resultData.getBoolean(PAUSED_KEY));
            }
        }
    }
}
