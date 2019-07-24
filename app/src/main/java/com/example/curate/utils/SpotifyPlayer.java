package com.example.curate.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.example.curate.models.Party;
import com.example.curate.models.PlaylistEntry;
import com.example.curate.models.Song;
import com.parse.ParseException;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp;
import com.spotify.android.appremote.api.error.NotLoggedInException;
import com.spotify.android.appremote.api.error.UserNotAuthorizedException;
import com.spotify.protocol.client.ErrorCallback;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.PlayerContext;
import com.spotify.protocol.types.PlayerState;

import java.util.List;

public class SpotifyPlayer {
    private static final String TAG = "SpotifyPlayer.java";
    private static final String REDIRECT_URI = "http://com.example.curate/callback";
    private static final ErrorCallback mErrorCallback = throwable -> Log.e(TAG, throwable + "Boom!");

    private static Subscription.EventCallback<PlayerState> mPlayerStateEventCallback;
    private static Subscription.EventCallback<PlayerContext> mPlayerContextEventCallback;
    private static String CLIENT_ID;
    private SpotifyAppRemote mSpotifyAppRemote;
    private Subscription<PlayerState> mPlayerStateSubscription;
    private Subscription<PlayerContext> mPlayerContextSubscription;
    private PlayerApi mPlayerApi;
    private Context mContext;
    private TrackProgressBar mTrackProgressBar;
    private boolean isSpotifyInstalled;


    private Connector.ConnectionListener mConnectionListener = new Connector.ConnectionListener() {
        @Override
        public void onConnected(SpotifyAppRemote spotifyAppRemote) {
            mSpotifyAppRemote = spotifyAppRemote;
            Log.d(TAG, "Connected!");

            mPlayerApi = mSpotifyAppRemote.getPlayerApi();
            onSubscribeToPlayerState();
            onSubscribeToPlayerContext();

            playCurrentSong();
        }

        @Override
        public void onFailure(Throwable error) {
            if (error instanceof NotLoggedInException || error instanceof UserNotAuthorizedException) {
                Log.e(TAG, error.toString());
                // Show login button and trigger the login flow from auth library when clicked TODO
                Log.d(TAG, "User is not logged in to SpotifyPlayer", error);
            } else if (error instanceof CouldNotFindSpotifyApp) {
                Log.e(TAG, "User does not have SpotifyPlayer app installed on device", error);
                // Show button to download SpotifyPlayer TODO
            }
        }
    };

    public SpotifyPlayer(Context context,
                         Subscription.EventCallback<PlayerState> eventCallback,
                         Subscription.EventCallback<PlayerContext> contextEventCallback,
                         String clientId,
                         SeekBar seekBar) {
        mContext = context;
        mPlayerStateEventCallback = eventCallback;
        mPlayerContextEventCallback = contextEventCallback;
        CLIENT_ID = clientId;

        checkSpotifyInstalled();

        if (isSpotifyInstalled) {
            SpotifyAppRemote.connect(context, new ConnectionParams.Builder(CLIENT_ID)
                    .setRedirectUri(REDIRECT_URI) // TODO try deleting this
                    .showAuthView(true)
                    .build(), mConnectionListener);
        }
        mTrackProgressBar = new TrackProgressBar(seekBar);
    }

    /**
     * Subscribes remote player to the SpotifyPlayer app's player state
     *
     */
    private void onSubscribeToPlayerState() {
        // If there is already a subscription, cancel it and start a new one
        if (mPlayerStateSubscription != null && !mPlayerStateSubscription.isCanceled()) {
            mPlayerStateSubscription.cancel();
            mPlayerStateSubscription = null;
        }

        mPlayerStateSubscription = (Subscription<PlayerState>) mPlayerApi.subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    if (playerState.track == null) {
                        mTrackProgressBar.pause();
                        mTrackProgressBar.setEnabled(false);
                    } else {
                        // Update progressbar
                        if (playerState.playbackSpeed > 0) {
                            mTrackProgressBar.unpause();
                        } else { // Playback is paused or buffering
                            mTrackProgressBar.pause();
                        }
                        mTrackProgressBar.setDuration(playerState.track.duration);
                        mTrackProgressBar.update(playerState.playbackPosition);
                        mTrackProgressBar.setEnabled(true);
                    }

                    mPlayerStateEventCallback.onEvent(playerState);
                })
                .setLifecycleCallback(new Subscription.LifecycleCallback() {
                    @Override
                    public void onStart() {
                        Log.d(TAG, "Event: start");
                    }

                    @Override
                    public void onStop() {
                        Log.d(TAG, "Event: end");
                    }
                })
                .setErrorCallback(throwable -> Log.e(TAG,throwable + "Subscribe to PlayerState failed!"));
    }

    /**
     * Subscribes remote player to the SpotifyPlayer app's player context
     *
     */
    private void onSubscribeToPlayerContext() {
        // If there is already a subscription, cancel it and start a new one
        if (mPlayerContextSubscription != null && !mPlayerContextSubscription.isCanceled()) {
            mPlayerContextSubscription.cancel();
            mPlayerContextSubscription = null;
        }

        mPlayerContextSubscription = (Subscription<PlayerContext>) mPlayerApi.subscribeToPlayerContext()
                .setEventCallback(playerContext -> {
                    mPlayerContextEventCallback.onEvent(playerContext);
                })
                .setLifecycleCallback(new Subscription.LifecycleCallback() {
                    @Override
                    public void onStart() {
                        Log.d(TAG, "Context subscription start");
                    }

                    @Override
                    public void onStop() {
                        Log.d(TAG, "Context subscription end");
                    }
                })
                .setErrorCallback(mErrorCallback);
    }

    /**
     * Plays the current party's current song
     *
     */
    public void playCurrentSong() {
        try {
            Song currSong = Party.getCurrentParty().getCurrentSong().fetchIfNeeded();
            String spotifyId = currSong.getSpotifyId();
            Log.d(TAG, "Playing track " + spotifyId);
            mPlayerApi.play("spotify:track:" + spotifyId);
        } catch (ParseException e) {
            Log.e(TAG, "Could not get next song!");
            pause();
        }
    }

    public void playNextSong() {
        List<PlaylistEntry> playlist = Party.getCurrentParty().getPlaylist();
        if(playlist.isEmpty()) {
            Log.e(TAG, "Playlist is empty!");
            return;
        }
        String spotifyId = playlist.get(0).getSong().getSpotifyId();
        Log.d(TAG, "Playing track " + spotifyId);
        mPlayerApi.play("spotify:track:" + spotifyId);
        Party.getCurrentParty().setCurrentlyPlaying(spotifyId, e -> {
            if (e == null) {
            } else {
                Log.e(TAG, "Error setting next song", e);
            }
        });
    }

    /**
     * Checks if SpotifyPlayer app is installed on device by searching for the package name
     */
    public void checkSpotifyInstalled() {
        PackageManager pm = mContext.getPackageManager();
        try {
            pm.getPackageInfo("com.spotify.music", 0);
            isSpotifyInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            isSpotifyInstalled = false;
            Log.e(TAG, "SpotifyPlayer app is not installed");
            // TODO prompt installation
        }
    }

    /**
     * Seeks to a new position in the current song playing on SpotifyPlayer
     *
     * @param progress New position to seek to
     */
    public void seekTo(int progress) {
        mPlayerApi.seekTo(progress)
                .setResultCallback(empty -> Log.d(TAG, "Seek success!"))
                .setErrorCallback(error -> Log.e(TAG, "Cannot seek unless you have premium!", error));
    }

    public void playPause() {
        mPlayerApi.getPlayerState().setResultCallback(playerState -> {
            if (playerState.isPaused) {
                mPlayerApi.resume()
                        .setResultCallback(empty -> Log.d(TAG, "Play current track successful"))
                        .setErrorCallback(mErrorCallback);
            } else {
                pause();
            }
        });
    }

    public void restartSong() {
        mPlayerApi.skipPrevious()
                .setResultCallback(empty -> Log.d(TAG, "Skip previous successful"))
                .setErrorCallback(mErrorCallback);
    }

    public void setAlbumArt(PlayerState playerState, ImageView ivAlbum) {
        mSpotifyAppRemote.getImagesApi()
                .getImage(playerState.track.imageUri, Image.Dimension.LARGE)
                .setResultCallback(bitmap -> {
                    Log.d(TAG, "Retrieved image bitmap");
                    ivAlbum.setImageBitmap(bitmap);
                });
    }

    public void pause() {
        mPlayerApi.pause()
                .setResultCallback(empty -> Log.d(TAG, "Pause successful"))
                .setErrorCallback(mErrorCallback);
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
                int timeRemaining = mSeekBar.getMax() - progress;
                if (timeRemaining < NEXT_SONG_DELAY && !mIsBeingTouched) {
                    mHandler.removeCallbacks(mSeekRunnable);
                    playNextSong();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsBeingTouched = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int timeRemaining = mSeekBar.getMax() - seekBar.getProgress();
                int progress = timeRemaining > NEXT_SONG_DELAY ? seekBar.getProgress() : mSeekBar.getMax() - NEXT_SONG_DELAY;
                SpotifyPlayer.this.seekTo(progress);
                mIsBeingTouched = false;
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

