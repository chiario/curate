package com.example.curate.utils;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import com.example.curate.activities.MainActivity;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
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
    private static SpotifyAppRemote mSpotifyAppRemote;
    private static Subscription<PlayerState> mPlayerStateSubscription;


    private static Connector.ConnectionListener mConnectionListener = new Connector.ConnectionListener() {
        @Override
        public void onConnected(SpotifyAppRemote spotifyAppRemote) {
            mSpotifyAppRemote = spotifyAppRemote;
            Log.d(TAG, "Connected!");
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

    /**
     * Connects context to the Spotify app remotely
     *
     * @param context Context to connect to Spotify for remote playing
     * @param eventCallback Callback for changes in player state
     * @param clientId App's clientId for the Spotify API
     */
    public static void connectRemote(Context context, Subscription.EventCallback<PlayerState> eventCallback, String clientId) {
        mEventCallback = eventCallback;

        SpotifyAppRemote.connect(context, new ConnectionParams.Builder(clientId)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build(), mConnectionListener);
    }

    /**
     * Subscribes remote player to the Spotify app's player state
     * If there is already a subscription, cancels it and starts a new one
     */
    private static void onSubscribeToPlayerState() {
        if (mPlayerStateSubscription != null && !mPlayerStateSubscription.isCanceled()) {
            mPlayerStateSubscription.cancel(); // TODO - do we really want to cancel and remake the subscription every time?
            mPlayerStateSubscription = null;
        }

        mPlayerStateSubscription = (Subscription<PlayerState>) mSpotifyAppRemote.getPlayerApi()
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

    public static void playNextSong(String songId) {
        Log.d(TAG, "Playing track " + songId);
        mSpotifyAppRemote.getPlayerApi().play("spotify:track:" + songId);
    }

    /**
     * Seeks to a new position in the current song playing on Spotify and sets the progress bar to that position
     *
     * @param progress New position to seek to
     * @param trackProgressBar Progress bar to update
     */
    public static void seekTo(int progress, MainActivity.TrackProgressBar trackProgressBar) {
        mSpotifyAppRemote.getPlayerApi().seekTo(progress)
                .setErrorCallback(error -> {
                    mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(
                            playerState -> trackProgressBar.update(playerState.playbackPosition)
                    );
                    Log.e(TAG, "Cannot seek unless you have premium!");
                });
    }

    public static void playPause() {
        mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(playerState -> {
            if (playerState.isPaused) {
                mSpotifyAppRemote.getPlayerApi()
                        .resume()
                        .setResultCallback(empty -> Log.d(TAG, "Play current track successful"))
                        .setErrorCallback(mErrorCallback);
            } else {
                mSpotifyAppRemote.getPlayerApi()
                        .pause()
                        .setResultCallback(empty -> Log.d(TAG, "Pause successful"))
                        .setErrorCallback(mErrorCallback);
            }
        });
    }

    public static void restartSong() {
        mSpotifyAppRemote.getPlayerApi()
                .skipPrevious()
                .setResultCallback(empty -> Log.d(TAG, "Skip previous successful"))
                .setErrorCallback(mErrorCallback);
    }

    public static void setAlbumArt(PlayerState playerState, final ImageView ivAlbum) {
        mSpotifyAppRemote.getImagesApi()
                .getImage(playerState.track.imageUri, Image.Dimension.LARGE)
                .setResultCallback(bitmap -> {
                    Log.d(TAG, "Got image bitmap");
                    ivAlbum.setImageBitmap(bitmap);
                });
    }
}
