package com.example.curate.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.example.curate.R;
import com.example.curate.models.Party;
import com.example.curate.models.PlaylistEntry;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp;
import com.spotify.android.appremote.api.error.NotLoggedInException;
import com.spotify.android.appremote.api.error.UserNotAuthorizedException;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import java.util.List;

import static com.example.curate.service.ServiceUtils.ACTION_CONNECT;
import static com.example.curate.service.ServiceUtils.ACTION_PLAY;
import static com.example.curate.service.ServiceUtils.ACTION_PLAY_PAUSE;
import static com.example.curate.service.ServiceUtils.ACTION_SKIP;
import static com.example.curate.service.ServiceUtils.ACTION_UPDATE;
import static com.example.curate.service.ServiceUtils.PAUSED_KEY;
import static com.example.curate.service.ServiceUtils.PLAYBACK_POS_KEY;
import static com.example.curate.service.ServiceUtils.RECEIVER_KEY;
import static com.example.curate.service.ServiceUtils.RESULT_ALBUM_ART;
import static com.example.curate.service.ServiceUtils.RESULT_CONNECTED;
import static com.example.curate.service.ServiceUtils.RESULT_DISCONNECTED;
import static com.example.curate.service.ServiceUtils.RESULT_INSTALL_SPOTIFY;
import static com.example.curate.service.ServiceUtils.RESULT_NEW_SONG;
import static com.example.curate.service.ServiceUtils.RESULT_OPEN_SPOTIFY;
import static com.example.curate.service.ServiceUtils.RESULT_PLAYBACK;
import static com.example.curate.service.ServiceUtils.RESULT_PLAY_PAUSE;
import static com.example.curate.service.ServiceUtils.SONG_ID_KEY;
import static com.example.curate.service.ServiceUtils.bundleBitmap;
import static com.example.curate.service.ServiceUtils.bundlePlayback;
import static com.example.curate.service.ServiceUtils.bundleTrack;

public class PlayerService extends JobIntentService {
    private static final String TAG = "PlayerService";
    private static final String REDIRECT_URI = "http://com.example.curate/callback";
    private static final int NEXT_SONG_PADDING = 2000;

    private static SpotifyAppRemote mSpotifyAppRemote;
    private static Subscription<PlayerState> mPlayerStateSubscription;
    private static PlayerApi mPlayerApi;
    private static ResultReceiver mResultReceiver;
    private static boolean mIsSpotifyConnected;

    private long mTimeRemaining;
    private String mCurrSongUri;

    private Party mCurrentParty;


    // Default constructor
    public PlayerService() {
        super();
    }


    // Called each time the service is started in a new thread
    @Override
    public void onCreate() {
        super.onCreate();
        mCurrentParty = Party.getCurrentParty();
        // Connect to Spotify remote player if needed
        if (!mIsSpotifyConnected) {
            connectSpotifyRemote(getApplicationContext(), getString(R.string.clientId));
        }
    }

    /**
     * This method is called whenever the service is triggered or work is enqueued
     */
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "onHandleWork() called with: intent = [" + intent + "]");
        if (intent.getAction() != null) {
            mResultReceiver = intent.getParcelableExtra(RECEIVER_KEY);
            switch (intent.getAction()) {
                case ACTION_CONNECT:
                    checkConnection();
                    break;
                case ACTION_PLAY:
                    String newSongId = intent.getStringExtra(SONG_ID_KEY);
                    playNewSong(newSongId);
                    break;
                case ACTION_SKIP:
                    pausePlayer();
                    String nextSongId = retrieveNextSong();
                    if (nextSongId != null) {
                        playNewSong(nextSongId);
                    } else {
                        pausePlayer();
                    }
                    break;
                case ACTION_PLAY_PAUSE:
                    playPause();
                    break;
                case ACTION_UPDATE:
                    long seekPosition = intent.getLongExtra(PLAYBACK_POS_KEY, 0);
                    seekTo(seekPosition);
                    break;

            }
        }
    }

    // Auto-play fields and methods

    final Handler runnableHandler = new Handler();
    final Runnable songRunnable = () -> {
        String nextSongId = retrieveNextSong();
        if (nextSongId != null) {
            playNewSong(nextSongId);
        } else {
            pausePlayer();
        }
        //TODO - add some check to ensure new song should start playing??
    };

    private void updateRunnable(long currPosition, long currDuration, boolean isPaused) {
        mTimeRemaining = currDuration - currPosition;
        // Set the runnable according to the new time remaining
        if (isPaused || mTimeRemaining <= 0) {
            pauseRunnable();
        } else {
            playRunnable();
        }
    }

    private void pauseRunnable() {
        runnableHandler.removeCallbacks(songRunnable);
    }

    private void playRunnable() {
        runnableHandler.removeCallbacks(songRunnable);
        if (mTimeRemaining > NEXT_SONG_PADDING) {
            runnableHandler.postDelayed(songRunnable, mTimeRemaining - NEXT_SONG_PADDING);
        } else {
            Log.d(TAG, "Play runnable called with not enough time remaining"); // This line should never be reached
        }
    }


    // Spotify PlayerApi methods

    /**
     * Connection listener for the Spotify Remote Player connection.
     * On result, notifies the service's result receiver if the remote player is connected.
     * If connected, subscribes to remote player state.
     */
    Connector.ConnectionListener mConnectionListener = new Connector.ConnectionListener() {
        // Called when connection to the Spotify app has been established
        @Override
        public void onConnected(SpotifyAppRemote spotifyAppRemote) {
            Log.d(TAG, "Spotify remote app connected!");
            mIsSpotifyConnected = true;
            mSpotifyAppRemote = spotifyAppRemote;
            mPlayerApi = mSpotifyAppRemote.getPlayerApi();

            // Notify the receiver that the Spotify remote player is connected
            mResultReceiver.send(RESULT_CONNECTED, null);
            onSubscribeToPlayerState();
        }

        // Called when connection to the Spotify app fails or is lost
        @Override
        public void onFailure(Throwable error) {
            mIsSpotifyConnected = false;
            // Notify the receiver that the Spotify remote player is disconnected
            mResultReceiver.send(RESULT_DISCONNECTED, null);

            if (error instanceof NotLoggedInException) {
                Log.e(TAG, "User is not logged in to Spotify.");
                mResultReceiver.send(RESULT_OPEN_SPOTIFY, null);
            } else if (error instanceof UserNotAuthorizedException) {
                Log.d(TAG, "User is not authorized.", error);
            } else if (error instanceof CouldNotFindSpotifyApp) {
                Log.e(TAG, "User does not have Spotify app installed on device.");
                mResultReceiver.send(RESULT_INSTALL_SPOTIFY, null);
            }
        }
    };

    /**
     * Connects context to Spotify Android Remote Player SDK and subscribes to PlayerState.
     * Only connects if the Spotify remote player isn't already connected.
     *
     * @param context the context to connect the Spotify remote player to.
     * @param clientId the application Spotify Client ID for the connection
     */
    private void connectSpotifyRemote(Context context, String clientId) {
        SpotifyAppRemote.connect(context, new ConnectionParams.Builder(clientId)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build(), mConnectionListener);
    }

    private void onSubscribeToPlayerState() {
        // If there is already a subscription, cancel it and start a new one
        if (mPlayerStateSubscription != null && !mPlayerStateSubscription.isCanceled()) {
            mPlayerStateSubscription.cancel();
            mPlayerStateSubscription = null;
        }

        mPlayerStateSubscription = (Subscription<PlayerState>) mPlayerApi.subscribeToPlayerState()
                .setEventCallback(mPlayerStateEventCallback)
                .setLifecycleCallback(new Subscription.LifecycleCallback() {
                    @Override
                    public void onStart() {
                        Log.d(TAG, "PlayerState subscription start");
                    }

                    @Override
                    public void onStop() {
                        Log.d(TAG, "PlayerState subscription end");
                    }
                })
                .setErrorCallback(throwable -> Log.e(TAG, throwable + "Subscribe to PlayerState failed!"));
    }

    /**
     * Callback for Spotify remote app player state subscription. onEvent is triggered any time
     * the remote app starts a new track, pauses, plays, or seeks to a new playback position.
     * If a new song begins playing, passes the information to the service receiver.
     */
    private final Subscription.EventCallback<PlayerState> mPlayerStateEventCallback = playerState -> {
        if (playerState.track != null) {
            if (!playerState.track.uri.equals(mCurrSongUri) && playerState.track.name != null) {
                // If the remote player has begun a new track update the receiver with track details
                mCurrSongUri = playerState.track.uri;
                Log.d(TAG, "Event with new song " + playerState.track.name
                        + " at plackback position " + playerState.playbackPosition / 1000 + " seconds");
                setCurrentlyPlaying(playerState.track);
                mResultReceiver.send(RESULT_NEW_SONG, bundleTrack(playerState));
                getAlbumArt();
            } else {
                // If the track hasn't changed, update the receiver with the current playback position
                mResultReceiver.send(RESULT_PLAYBACK, bundlePlayback(playerState));
            }
            updateRunnable(playerState.playbackPosition, playerState.track.duration, playerState.isPaused);
        }
    };

    /**
     * Sets the currently playing song in the Parse server.
     * If the song is in the current playlist, the object ID for the PlaylistEntry is passed as an
     * argument so it can be deleted from the playlist. Otherwise, the Spotify ID is provided.
     *
     * @param track the Spotify track of the currently playing song
     */
    private void setCurrentlyPlaying(Track track) {
        String spotifyId = track.uri.replace("spotify:track:", "");;
        PlaylistEntry entry = getEntryBySpotifyId(spotifyId);
        if (entry == null) {
            mCurrentParty.setCurrentlyPlayingSong(spotifyId, e-> {
                if (e != null) {
                    Log.e(TAG, "Error setting currently playing song", e);
                }
            });
        } else {
            mCurrentParty.setCurrentlyPlayingEntry(entry.getObjectId(), e -> { // TODO - this isn't working
                if (e != null) {
                    Log.e(TAG, "Error setting currently playing entry", e);
                }
            });
        }
    }

    /**
     * Iterates through the current party's playlist entries. If the given Spotify ID corresponds
     * to a playlist entry, returns that entry.
     *
     * @param spotifyId the Spotify ID of the song to search for in the playlist
     * @return the playlist entry corresponding to the given Spotify ID, or null
     */
    private PlaylistEntry getEntryBySpotifyId(String spotifyId) {
        List<PlaylistEntry> playlistEntries = mCurrentParty.getPlaylist().getEntries();
        for (PlaylistEntry entry : playlistEntries) {
            if (spotifyId.equals(entry.getSong().getSpotifyId())) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Notifies the receiver of the connection state
     */
    private void checkConnection() {
        if (mIsSpotifyConnected) {
            mResultReceiver.send(RESULT_CONNECTED, null);
            getCurrentPlayback();
        } else {
            mResultReceiver.send(RESULT_DISCONNECTED, null);
        }
    }


    // Playback methods

    private void playPause() {
        if (mPlayerApi != null && mSpotifyAppRemote.isConnected()) {
            mPlayerApi.getPlayerState().setResultCallback(playerState -> {
                if (playerState.isPaused) {
                    resumePlayer();
                } else {
                    pausePlayer();
                }
            });
        }
    }

    private void resumePlayer() {
        mPlayerApi.resume()
                .setResultCallback(empty -> {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(PAUSED_KEY, false);
                    mResultReceiver.send(RESULT_PLAY_PAUSE, bundle);
                    Log.d(TAG, "Resume successful");
                })
                .setErrorCallback(throwable -> Log.e(TAG, "Error resuming play!", throwable));
    }

    private void pausePlayer() {
        mPlayerApi.pause()
                .setResultCallback(empty -> {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(PAUSED_KEY, true);
                    mResultReceiver.send(RESULT_PLAY_PAUSE, bundle);
                    Log.d(TAG, "Pause successful");
                })
                .setErrorCallback(throwable -> Log.e(TAG, "Error pausing play!", throwable));
    }

    public void seekTo(long progress) {
        if (mPlayerApi != null && mSpotifyAppRemote.isConnected()) {
            mPlayerApi.seekTo(progress)
                    .setResultCallback(empty -> Log.d(TAG, "Seek success!"))
                    .setErrorCallback(error -> Log.e(TAG, "Cannot seek unless you have premium!", error));
        }
    }

    private void playNewSong(String spotifyId) {
        if (mPlayerApi != null && mSpotifyAppRemote.isConnected()) {
            mPlayerApi.play("spotify:track:" + spotifyId)
                    .setResultCallback(empty -> Log.d(TAG, "Success playing new song " + spotifyId))
                    .setErrorCallback(throwable -> Log.e(TAG, "Error playing new song " + spotifyId, throwable));
        }
    }

    private void getCurrentPlayback() {
        mPlayerApi.getPlayerState().setResultCallback(playerState -> {
            mResultReceiver.send(RESULT_NEW_SONG, bundleTrack(playerState));
        });
        getAlbumArt();
    }

    /**
     * This function retrieves the top song in the current party's cached playlist and deletes it.
     * If the playlist is empty, it logs an error.
     *
     * @return the spotify ID of the song to be played
     */
    private String retrieveNextSong() {
        if (mCurrentParty.getPlaylist().getEntries() == null || mCurrentParty.getPlaylist().getEntries().isEmpty()) {
            Log.e(TAG, "Playlist is empty!");
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), "Your queue is empty!", Toast.LENGTH_LONG).show());
            return null;
        } else {
            PlaylistEntry entry = mCurrentParty.getPlaylist().getEntries().get(0);
            return entry.getSong().getSpotifyId();
        }
    }

    /**
     * Retrieves the album art bitmap of the currently playing song, converts to a byte array
     * and sends to receiver for loading into views.
     */
    private void getAlbumArt() {
        mPlayerApi.getPlayerState().setResultCallback(playerState -> {
            mSpotifyAppRemote.getImagesApi()
                    .getImage(playerState.track.imageUri, Image.Dimension.LARGE)
                    .setResultCallback(bitmap -> {
                        mResultReceiver.send(RESULT_ALBUM_ART, bundleBitmap(bitmap));
                    });
        });
    }
}
