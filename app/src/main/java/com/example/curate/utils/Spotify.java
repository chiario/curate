package com.example.curate.utils;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.example.curate.models.Party;
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
import com.spotify.protocol.types.PlayerState;

public class Spotify {
    private static final String TAG = "Spotify.java";
    private static final String REDIRECT_URI = "http://com.example.curate/callback";
    private static final ErrorCallback mErrorCallback = throwable -> Log.e(TAG, throwable + "Boom!");
    private static Subscription.EventCallback<PlayerState> mEventCallback;
    private static String CLIENT_ID;
    private SpotifyAppRemote mSpotifyAppRemote;
    private Subscription<PlayerState> mPlayerStateSubscription;
    private PlayerApi mPlayerApi;
    private TrackProgressBar mTrackProgressBar;
    private Context mContext;


    private Connector.ConnectionListener mConnectionListener = new Connector.ConnectionListener() {
        @Override
        public void onConnected(SpotifyAppRemote spotifyAppRemote) {
            mSpotifyAppRemote = spotifyAppRemote;
            Log.d(TAG, "Connected!");
            mPlayerApi = mSpotifyAppRemote.getPlayerApi();
            onSubscribeToPlayerState();
        }

        @Override
        public void onFailure(Throwable error) {
            if (error instanceof NotLoggedInException || error instanceof UserNotAuthorizedException) {
                Log.e(TAG, error.toString());
                // Show login button and trigger the login flow from auth library when clicked TODO
                Log.d(TAG, "User is not logged in to Spotify", error);
            } else if (error instanceof CouldNotFindSpotifyApp) {
                Log.e(TAG, "User does not have Spotify app installed on device", error);
                // Show button to download Spotify TODO
            }
        }
    };

    public Spotify(Context context, Subscription.EventCallback<PlayerState> eventCallback, String clientId) {
        mContext = context;
        mEventCallback = eventCallback;
        CLIENT_ID = clientId;
        SpotifyAppRemote.connect(context, new ConnectionParams.Builder(clientId)
                .setRedirectUri(REDIRECT_URI) // TODO try deleting this
                .showAuthView(true)
                .build(), mConnectionListener);
    }

    /**
     * Connects context to the Spotify app remotely
     *
     * @param context Context to connect to Spotify for remote playing
     * @param eventCallback Callback for changes in player state
     * @param clientId App's clientId for the Spotify API
     */
    /*public static void connectRemote(Context context, Subscription.EventCallback<PlayerState> eventCallback, String clientId) {
        mEventCallback = eventCallback;
        SpotifyAppRemote.connect(context, new ConnectionParams.Builder(clientId)
                .setRedirectUri(REDIRECT_URI) // TODO try deleting this
                .showAuthView(true)
                .build(), mConnectionListener);
    }*/

    /**
     * Subscribes remote player to the Spotify app's player state
     * If there is already a subscription, cancels it and starts a new one
     */
    private void onSubscribeToPlayerState() {
        if (mPlayerStateSubscription != null && !mPlayerStateSubscription.isCanceled()) {
            mPlayerStateSubscription.cancel();
            mPlayerStateSubscription = null;
        }

        mPlayerStateSubscription = (Subscription<PlayerState>) mPlayerApi
                .subscribeToPlayerState()
                .setEventCallback(mEventCallback)
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
                .setErrorCallback(throwable -> {
                    Log.e(TAG,throwable + "Subscribed to PlayerState failed!");
                });
    }

    /**
     * Plays the given song id
     * If passed a null id, pauses the Spotify player
     *
     * @param songId Spotify id of song to be played
     */
    public void playNextSong(String songId) {
        Log.d(TAG, "Playing track " + songId);
        mPlayerApi.play("spotify:track:" + songId);
    }

    /**
     * Seeks to a new position in the current song playing on Spotify and sets the progress bar to that position
     *
     * @param progress New position to seek to
     */
    public void seekTo(int progress) {
        mSpotifyAppRemote.getPlayerApi()
                .seekTo(progress)
                .setResultCallback(empty -> {
                    mSpotifyAppRemote.getPlayerApi()
                            .getPlayerState()
                            .setResultCallback(playerState -> {
//                                mTrackProgressBar.update(playerState.playbackPosition);
                            }
                    );
                })
                .setErrorCallback(error -> {
                    Log.e(TAG, "Cannot seek unless you have premium!", error);
                });
    }

    public void playPause() {
        mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
            if (playerState.isPaused) {
                mSpotifyAppRemote.getPlayerApi()
                        .resume()
                        .setResultCallback(empty -> Log.d(TAG, "Play current track successful"))
                        .setErrorCallback(mErrorCallback);
            } else {
                pause();
            }
        });
    }

    public void restartSong() {
        mSpotifyAppRemote.getPlayerApi()
                .skipPrevious()
                .setResultCallback(empty -> Log.d(TAG, "Skip previous successful"))
                .setErrorCallback(mErrorCallback);
    }

    public void setAlbumArt(PlayerState playerState, final ImageView ivAlbum) {
        mSpotifyAppRemote.getImagesApi()
                .getImage(playerState.track.imageUri, Image.Dimension.LARGE)
                .setResultCallback(bitmap -> {
                    Log.d(TAG, "Got image bitmap");
                    ivAlbum.setImageBitmap(bitmap);
                });
    }

    public void pause() {
        mSpotifyAppRemote.getPlayerApi()
                .pause()
                .setResultCallback(empty -> Log.d(TAG, "Pause successful"))
                .setErrorCallback(mErrorCallback);
    }

    public void setTrackProgressBar(TrackProgressBar trackProgressBar) {
        mTrackProgressBar = trackProgressBar;
    }


    public static class TrackProgressBar {
        private static final int LOOP_DURATION = 500;
        private final SeekBar mSeekBar;
        private final Handler mHandler;
        private Spotify mSpotifyPlayer;

        public TrackProgressBar(Spotify spotifyPlayer, SeekBar seekBar) {
            mSeekBar = seekBar;
            mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
            mHandler = new Handler();
            mSpotifyPlayer = spotifyPlayer;
        }

        private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int timeRemaining = mSeekBar.getMax() - progress;
                if (timeRemaining < 2000) {

                    Party.getCurrentParty().getNextSong(e -> {
                        try {
                            mSpotifyPlayer.playNextSong(Party.getCurrentParty().getCurrentSong().getSpotifyId());
                        } catch (NullPointerException e1) {
                            mSpotifyPlayer.pause();
//                            Toast.makeText(, "Add another song!", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSpotifyPlayer.seekTo(seekBar.getProgress());
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
        }

        public void pause() {
            mHandler.removeCallbacks(mSeekRunnable);
        }

        public void unpause() {
            mHandler.removeCallbacks(mSeekRunnable);
            mHandler.postDelayed(mSeekRunnable, LOOP_DURATION);
        }
    }

}

