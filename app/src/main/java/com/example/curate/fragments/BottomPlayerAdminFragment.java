package com.example.curate.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import androidx.fragment.app.Fragment;

import com.example.curate.R;
import com.example.curate.models.Party;
import com.example.curate.utils.LocationManager;
import com.example.curate.utils.SpotifyPlayer;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerContext;
import com.spotify.protocol.types.PlayerState;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.graphics.Typeface.BOLD;
import static android.graphics.Typeface.NORMAL;

public class BottomPlayerAdminFragment extends Fragment {
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

    private SpotifyPlayer mSpotifyPlayer;
    private LocationManager mLocationManager;

    private String mTrackName = "--";
    private String mArtistName = "--";
    private boolean isExpanded;
    private Drawable mThumbDrawable;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bottom_player_admin, container, false);
        ButterKnife.bind(this, view);

        mSpotifyPlayer = new SpotifyPlayer(getContext(),
                mPlayerStateEventCallback,
                mPlayerContextEventCallback,
                getContext().getString(R.string.clientId),
                mSeekBar);

        mLocationManager = new LocationManager(getContext());
        if(mLocationManager.hasNecessaryPermissions() && Party.getLocationEnabled()) {
            registerLocationUpdater();
        } else {
            mLocationManager.requestPermissions();
        }
        mCollapsed = new ConstraintSet();
        mCollapsed.clone(getContext(), R.layout.fragment_bottom_player_admin_collapsed);
        mExpanded = new ConstraintSet();
        mExpanded.clone(getContext(), R.layout.fragment_bottom_player_admin);
        mThumbDrawable = mSeekBar.getThumb();
        setExpanded(false);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSpotifyPlayer.checkSpotifyInstalled();
    }

    //Methods for SpotifyPlayer remote player communication
    /**
     * Admin's SpotifyPlayer Player Context event callback
     * Unlocks track progress bar when new track begins
     */
    private final Subscription.EventCallback<PlayerContext> mPlayerContextEventCallback = playerContext -> {
        Log.d("SpotifyPlayer.java", playerContext.toString());
    };

    /**
     * Admin's SpotifyPlayer Player State event callback
     * Updates current song views whenever player state changes, e.g. on pause, play, new track
     */
    private final Subscription.EventCallback<PlayerState> mPlayerStateEventCallback = playerState -> {
        if (playerState.track == null) {
            mPlayPauseButton.setImageResource(R.drawable.btn_play);
            mArtistName = "--";
            mTrackName = "--";
            updateText();
        } else {
            // Set play / pause button
            if (playerState.isPaused) {
                mPlayPauseButton.setImageResource(R.drawable.btn_play);
            } else {
                mPlayPauseButton.setImageResource(R.drawable.btn_pause);
            }

            mTrackName = playerState.track.name;
            mArtistName = playerState.track.artist.name;
            updateText();
            // Get image from track
            mSpotifyPlayer.setAlbumArt(playerState, ivAlbum);
        }
    };

    @OnClick(R.id.skip_prev_button)
    public void onRestartSong() {
        mSpotifyPlayer.restartSong();
    }

    @OnClick(R.id.play_pause_button)
    public void onPlayPause() {
        mSpotifyPlayer.playPause();
    }

    @OnClick(R.id.skip_next_button)
    public void onSkipNext() {
        mSpotifyPlayer.playNextSong();
    }

    @OnClick(R.id.ibShare)
    public void onClickShare() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
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
            mSeekBar.setThumb(mThumbDrawable);
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
    public void onClickClCurrPlaying(View v) {
        setExpanded(!ibExpandCollapse.isSelected());
    }

    @OnClick(R.id.ibExpandCollapse)
    public void onClickExpandCollapse(View v) {
        setExpanded(!ibExpandCollapse.isSelected());
    }

    public void registerLocationUpdater() {
        // Update location when user moves
        mLocationManager.registerLocationUpdateCallback(new LocationCallback() {
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
        });

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
        mLocationManager.deregisterLocationUpdateCallback(new LocationCallback() {
            /*// TODO - Override methods?

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
                Log.d("MainActivity", "Location availability: " + locationAvailability.isLocationAvailable());
            }*/
        });
        Party.clearLocation(e -> {
            if (e != null) {
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
            } else {
                Log.i("AdminManager", "Location permission was not granted.");
            }
        }
    }
}
