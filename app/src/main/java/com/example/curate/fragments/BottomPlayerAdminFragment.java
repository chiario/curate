package com.example.curate.fragments;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

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

public class BottomPlayerAdminFragment extends Fragment {
    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvArtist) TextView tvArtist;
    @BindView(R.id.ivAlbum) ImageView ivAlbum;
    @BindView(R.id.seek_to) SeekBar mSeekBar;
    @BindView(R.id.play_pause_button) ImageView mPlayPauseButton;
    @BindView(R.id.clCurrPlaying) ConstraintLayout mPlayerBackground;
    @BindView(R.id.skip_prev_button) ImageView mSkipPrevButton;
    @BindView(R.id.skip_next_button) ImageView mSkipNextButton;

    private SpotifyPlayer mSpotifyPlayer;
    private LocationManager mLocationManager;

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
        if(mLocationManager.hasNecessaryPermissions()) {
            registerLocationUpdater();
        } else {
            mLocationManager.requestPermissions();
        }

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
    public final Subscription.EventCallback<PlayerContext> mPlayerContextEventCallback = playerContext -> {
        Log.d("SpotifyPlayer.java", playerContext.toString());
    };

    /**
     * Admin's SpotifyPlayer Player State event callback
     * Updates current song views whenever player state changes, e.g. on pause, play, new track
     */
    public final Subscription.EventCallback<PlayerState> mPlayerStateEventCallback = playerState -> {
        if (playerState.track == null) {
            mPlayPauseButton.setImageResource(R.drawable.btn_play);
            tvTitle.setText("--");
            tvArtist.setText("--");
        } else {
            // Set play / pause button
            if (playerState.isPaused) {
                mPlayPauseButton.setImageResource(R.drawable.btn_play);
            } else {
                mPlayPauseButton.setImageResource(R.drawable.btn_pause);
            }

            tvTitle.setText(playerState.track.name);
            tvArtist.setText(playerState.track.artist.name);
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
        Party.getCurrentParty().getNextSong(e -> {
            if (e == null) {
                mSpotifyPlayer.playCurrentSong();
            } else {
                Log.e("AdminManager", "Error getting next song", e);
            }
        });
    }

    private void registerLocationUpdater() {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == LocationManager.PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission has been granted, register location updater
                registerLocationUpdater();
            } else {
                Log.i("AdminManager", "Location permission was not granted.");
            }
        }
    }
}
