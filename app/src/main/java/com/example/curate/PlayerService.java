package com.example.curate;

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

import com.example.curate.models.Party;
import com.example.curate.models.PlaylistEntry;
import com.parse.SaveCallback;
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

import java.util.List;

import static com.example.curate.ServiceUtils.ACTION_INIT;
import static com.example.curate.ServiceUtils.ACTION_PLAY;
import static com.example.curate.ServiceUtils.ACTION_PLAY_PAUSE;
import static com.example.curate.ServiceUtils.ACTION_SKIP;
import static com.example.curate.ServiceUtils.ACTION_UPDATE;
import static com.example.curate.ServiceUtils.PAUSED_KEY;
import static com.example.curate.ServiceUtils.PLAYBACK_POS_KEY;
import static com.example.curate.ServiceUtils.RECEIVER_KEY;
import static com.example.curate.ServiceUtils.RESULT_ALBUM_ART;
import static com.example.curate.ServiceUtils.RESULT_NEW_SONG;
import static com.example.curate.ServiceUtils.RESULT_PLAYBACK;
import static com.example.curate.ServiceUtils.RESULT_PLAY_PAUSE;
import static com.example.curate.ServiceUtils.SONG_ID_KEY;
import static com.example.curate.ServiceUtils.bundleBitmap;
import static com.example.curate.ServiceUtils.bundlePlayback;
import static com.example.curate.ServiceUtils.bundleTrack;

public class PlayerService extends JobIntentService {
    private static final String TAG = "PlayerService";
    private static final String REDIRECT_URI = "http://com.example.curate/callback";
    private static final int NEXT_SONG_DELAY = 2000;


    private static String CLIENT_ID;
    private static SpotifyAppRemote mSpotifyAppRemote;
    private static Subscription<PlayerState> mPlayerStateSubscription;
    private static PlayerApi mPlayerApi;
    private Context mContext;
    private static ResultReceiver mResultReceiver;
    private boolean mIsSpotifyInstalled;
    private static boolean mIsSpotifyConnected;

    private long mLastPlaybackPosition;
    private long mCurrDuration;
    private long mTimeRemaining;
    private String mCurrSongUri;

    private Party mCurrentParty;
    private SaveCallback mPlaylistUpdatedCallback;
    private List<PlaylistEntry> mPlaylist;



    // Default constructor
    public PlayerService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        CLIENT_ID = getString(R.string.clientId);
        connectSpotifyRemote(); //TODO check installation first
    }

    private void initializePlaylistUpdateCallback() {
        mCurrentParty = Party.getCurrentParty();
        mPlaylist = mCurrentParty.getPlaylist();
        mPlaylistUpdatedCallback = e -> mPlaylist = mCurrentParty.getPlaylist();
        mCurrentParty.registerPlaylistUpdateCallback(mPlaylistUpdatedCallback);
    }

    /**
     *  This method is called whenever the service is triggered or work is enqueued
     */
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "onHandleWork() called with: intent = [" + intent + "]");
        if (intent.getAction() != null) {
            mResultReceiver = intent.getParcelableExtra(RECEIVER_KEY);
            switch (intent.getAction()) {
                case ACTION_INIT:
                    initializePlaylistUpdateCallback();
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
        mCurrDuration = currDuration;
        mLastPlaybackPosition = currPosition;
        mTimeRemaining = mCurrDuration - mLastPlaybackPosition;
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
        Log.d(TAG, "Posting runnable with delay " + mTimeRemaining/1000);
        runnableHandler.removeCallbacks(songRunnable);
        if (mTimeRemaining > NEXT_SONG_DELAY) {
            runnableHandler.postDelayed(songRunnable, mTimeRemaining - NEXT_SONG_DELAY);
        } else {
            Log.d(TAG, "Play runnable called with not enough time remaining"); // This line should never be reached
        }
    }


    // Spotify PlayerApi methods

    /**
     * Connects context to Spotify Android Remote Player SDK and subscribes to PlayerState
     */
    private void connectSpotifyRemote() {
        if (!mIsSpotifyConnected) {
            SpotifyAppRemote.connect(mContext, new ConnectionParams.Builder(CLIENT_ID)
                    .setRedirectUri(REDIRECT_URI)
                    .showAuthView(true)
                    .build(), new Connector.ConnectionListener() {
                @Override
                public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                    Log.d(TAG, "Spotify remote app connected!");
                    mIsSpotifyConnected = true;
                    mSpotifyAppRemote = spotifyAppRemote;
                    mPlayerApi = mSpotifyAppRemote.getPlayerApi();
                    onSubscribeToPlayerState();
                }

                @Override
                public void onFailure(Throwable error) {
                    if (error instanceof NotLoggedInException || error instanceof UserNotAuthorizedException) {
                        mIsSpotifyConnected = false;
                        // Show login button and trigger the login flow from auth library when clicked TODO
                        Log.d(TAG, "User is not logged in to SpotifyPlayer", error);
                    } else if (error instanceof CouldNotFindSpotifyApp) {
                        Log.e(TAG, "User does not have SpotifyPlayer app installed on device", error);
                        // Show button to download SpotifyPlayer TODO
                    }
                }
            });
        } else {
            assert mSpotifyAppRemote.isConnected();
        }
    }

    private void onSubscribeToPlayerState() {
        // If there is already a subscription, cancel it and start a new one
        if (mPlayerStateSubscription != null && !mPlayerStateSubscription.isCanceled()) {
            mPlayerStateSubscription.cancel();
            mPlayerStateSubscription = null;
        }

        mPlayerStateSubscription = (Subscription<PlayerState>) mPlayerApi.subscribeToPlayerState()
                .setEventCallback(mPlayerStateEventCallback::onEvent)
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
     * Callback for Spotify remote app player state subscription. onEvent is triggered any time
     * the remote app starts a new track, pauses, plays, or seeks to a new playback position.
     * If a new song begins playing, passes the information to the service receiver.
     */
    private final Subscription.EventCallback<PlayerState> mPlayerStateEventCallback = playerState -> {
        if (playerState.track != null) {
            // Only update the receiver with track details if the remote player has begun a new track
            if (!playerState.track.uri.equals(mCurrSongUri) && playerState.track.name != null) {
                Log.d(TAG, "Event with new song " + playerState.track.name + " at plackback position " + playerState.playbackPosition / 1000 + " seconds");
                setCurrentlyPlaying(playerState.track.uri);
                mResultReceiver.send(RESULT_NEW_SONG, bundleTrack(playerState));
                getAlbumArt();
            } else { // If the track hasn't changed, update the receiver with the current playback position
                Log.d(TAG, "Event with playback position " + playerState.playbackPosition / 1000 + " seconds");
                mResultReceiver.send(RESULT_PLAYBACK, bundlePlayback(playerState));
            }
            updateRunnable(playerState.playbackPosition, playerState.track.duration, playerState.isPaused);
        }
    };



    private void setCurrentlyPlaying(String uri) {
        mCurrSongUri = uri;
        mCurrentParty.setCurrentlyPlaying(uri.replace("spotify:track:", ""), e -> {
            if (e == null) {
                Log.d(TAG, "Set currently playing success!");
            } else {
                Log.e(TAG, "Error setting currently playing song", e);
            }
        });
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

    /**
     * This function retrieves the top song in the locally cached playlist and deletes it.
     * If the playlist is empty, it logs an error.
     *
     * @return the spotify ID of the song to be played
     */
    private String retrieveNextSong() {
        if(mPlaylist == null || mPlaylist.isEmpty()) {
            Log.e(TAG, "Playlist is empty!");
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), "Your queue is empty!", Toast.LENGTH_LONG).show());
            return null;
        } else {
            String spotifyId = mPlaylist.get(0).getSong().getSpotifyId();
            mPlaylist.remove(0);
            return spotifyId;
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





    //TODO - implement this
    /**
     * Checks if SpotifyPlayer app is installed on device by searching for the package name
     */
    /*public void checkSpotifyInstalled() {
        PackageManager pm = mContext.getPackageManager();
        try {
            pm.getPackageInfo("com.spotify.music", 0);
            mIsSpotifyInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            mIsSpotifyInstalled = false;
            Log.e(TAG, "SpotifyPlayer app is not installed");
            // TODO prompt installation
        }
    }*/
}
