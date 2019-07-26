package com.example.curate;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;

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

import java.io.ByteArrayOutputStream;
import java.util.List;

public class PlayerService extends JobIntentService {
    private static final String TAG = "PlayerService";
    private static final String REDIRECT_URI = "http://com.example.curate/callback";
    // Unique job ID for this service
    private static final int PLAYER_JOB_ID = 1000;
    private static final int NEXT_SONG_DELAY = 2000;

    public static final String RECEIVER_KEY = "receiver";
    public static final String SONG_ID_KEY = "spotifyId";
    public static final String PLAYBACK_POS_KEY = "playbackPosition";
    public static final String DURATION_KEY = "duration";
    public static final String PAUSED_KEY = "isPaused";
    public static final String TITLE_KEY = "title";
    public static final String ARTIST_KEY = "artist";
    public static final String IMAGE_KEY = "image";
    public static final String ACTION_PLAY = "action.PLAY";
    public static final String ACTION_UPDATE = "action.UPDATE";
    public static final String ACTION_INIT = "action.CONNECT";
    public static final String ACTION_SKIP = "action.SKIP";
    public static final String ACTION_PLAY_PAUSE = "action.PLAY_PAUSE";
    public static final int RESULT_NEW_SONG = 123;
    public static final int RESULT_PLAY_PAUSE = 456;
    public static final int RESULT_ALBUM_ART = 789;
    public static final int RESULT_SEEK = 1000;

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
    private String mCurrSongId;



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


    /**
     *  This method is called whenever the service is triggered or work is enqueued
     */
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "onHandleWork() called with: intent = [" + intent + "]");
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_INIT:
                    mResultReceiver = intent.getParcelableExtra(RECEIVER_KEY);
                    break;
                case ACTION_PLAY: // TODO - this action is called when the admin taps a song in the rv adapter
                    mResultReceiver = intent.getParcelableExtra(RECEIVER_KEY);
                    String newSongId = intent.getStringExtra(SONG_ID_KEY);
                    playNewSong(newSongId);
                    break;
                case ACTION_SKIP:
                    mResultReceiver = intent.getParcelableExtra(RECEIVER_KEY);
                    String nextSongId = retrieveNextSong();
                    playNewSong(nextSongId);
                    break;
                case ACTION_PLAY_PAUSE:
                    mResultReceiver = intent.getParcelableExtra(RECEIVER_KEY);
                    playPause();
                    break;
                case ACTION_UPDATE:
                    mResultReceiver = intent.getParcelableExtra(RECEIVER_KEY);
                    long seekPosition = intent.getLongExtra(PLAYBACK_POS_KEY, 0);
                    seekTo(seekPosition);
                    break;
            }
        }
    }

    /**
     * Convenience method for enqueuing work into this service.
     */
    public static void enqueueWork(Context context, PlayerResultReceiver playerResultReceiver, String ACTION, @Nullable Bundle data) {
        Intent intent = new Intent(context, PlayerService.class);
        intent.putExtra(RECEIVER_KEY, playerResultReceiver);

        switch (ACTION) {
            case ACTION_UPDATE:
                long seekTo = data.getLong(PLAYBACK_POS_KEY);
                intent.putExtra(PLAYBACK_POS_KEY, seekTo);
                break;
            case ACTION_PLAY:
                String newSongId = data.getString(SONG_ID_KEY);
                intent.putExtra(SONG_ID_KEY, newSongId); // TODO - more efficient way to do this??
                break;
        }

        intent.setAction(ACTION);
        enqueueWork(context, PlayerService.class, PLAYER_JOB_ID, intent);
    }


    // Auto-play methods

    final Handler runnableHandler = new Handler();

    final Runnable songRunnable = new Runnable() {
//        private String lastSongId;
        @Override
        public void run() {
            String nextSongId = retrieveNextSong();
            playNewSong(nextSongId);
            //TODO - add some check to ensure new song should start playing??
        }
    };

    private void pauseRunnable() {
        runnableHandler.removeCallbacks(songRunnable);
    }

    private void setRunnable() {
        Log.d(TAG, "Posting runnable with delay " + mTimeRemaining/1000);
        runnableHandler.removeCallbacks(songRunnable);

        if (mTimeRemaining > NEXT_SONG_DELAY) {
            runnableHandler.postDelayed(songRunnable, mTimeRemaining - NEXT_SONG_DELAY);
        }
    }


    /**
     * Connects context to spotify remote player
     * Subscribes to player state on connection
     */
    private void connectSpotifyRemote() {
//        if (mSpotifyAppRemote == null || !mSpotifyAppRemote.isConnected()) {
        if (!mIsSpotifyConnected) {
            SpotifyAppRemote.connect(mContext, new ConnectionParams.Builder(CLIENT_ID)
                    .setRedirectUri(REDIRECT_URI)
                    .showAuthView(true)
                    .build(), new Connector.ConnectionListener() {
                @Override
                public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                    mIsSpotifyConnected = true;
                    mSpotifyAppRemote = spotifyAppRemote;
                    Log.d(TAG, "Spotify remote app connected!");
                    mPlayerApi = mSpotifyAppRemote.getPlayerApi();
                    onSubscribeToPlayerState();
                }

                @Override
                public void onFailure(Throwable error) {
                    if (error instanceof NotLoggedInException || error instanceof UserNotAuthorizedException) {
                        mIsSpotifyConnected = false;
                        Log.e(TAG, error.toString());
                        // Show login button and trigger the login flow from auth library when clicked TODO
                        Log.d(TAG, "User is not logged in to SpotifyPlayer", error);
                    } else if (error instanceof CouldNotFindSpotifyApp) {
                        Log.e(TAG, "User does not have SpotifyPlayer app installed on device", error);
                        // Show button to download SpotifyPlayer TODO
                    }
                }
            });
        }
    }

    /**
     * Subscribes to Spotify remote application player state.
     * First deletes any existing subscription.
     */
    private void onSubscribeToPlayerState() {
        // If there is already a subscription, cancel it and start a new one
        if (mPlayerStateSubscription != null && !mPlayerStateSubscription.isCanceled()) {
            mPlayerStateSubscription.cancel();
            mPlayerStateSubscription = null;
        }

        mPlayerStateSubscription = (Subscription<PlayerState>) mPlayerApi.subscribeToPlayerState()
                .setEventCallback(playerState -> {
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
     * Callback for Spotify remote app player state subscription. onEvent is triggered any time
     * the remote app starts a new track, pauses, plays, or seeks to a new playback position.
     * If a new song begins playing, passes the information to the service receiver.
     */
    private final Subscription.EventCallback<PlayerState> mPlayerStateEventCallback = playerState -> {
        if (playerState.track != null) {
            // Only update the party's currently playing song if it has changed
            if (!playerState.track.uri.equals(mCurrSongId)) {
                Log.d(TAG, "Event with new song " + playerState.track.name + " at plackback position " + playerState.playbackPosition / 1000 + " seconds");
                mCurrSongId = playerState.track.uri;
                Bundle bundle = new Bundle();
                bundle.putString(SONG_ID_KEY, playerState.track.uri);
                bundle.putString(TITLE_KEY, playerState.track.name);
                bundle.putString(ARTIST_KEY, playerState.track.artist.name);
                bundle.putLong(DURATION_KEY, playerState.track.duration);
                bundle.putBoolean(PAUSED_KEY, playerState.isPaused);
                bundle.putLong(PLAYBACK_POS_KEY, playerState.playbackPosition);
                mResultReceiver.send(RESULT_NEW_SONG, bundle);
                getAlbumArt();
                /*Party.getCurrentParty().setCurrentlyPlaying(playerState.track.uri, e -> {
                    if (e == null) {
                        Log.e(TAG, "Success setting currently playing song");
                    } else {
                        Log.e(TAG, "Error setting currently playing song", e);
                    }
                });*///TODO
            } else {
                Log.d(TAG, "Event with playback position " + playerState.playbackPosition / 1000 + " seconds");
                Bundle bundle = new Bundle();
                bundle.putLong(PLAYBACK_POS_KEY, playerState.playbackPosition);
                mResultReceiver.send(RESULT_SEEK, bundle);
            }

            mCurrDuration = playerState.track.duration;
            mLastPlaybackPosition = playerState.playbackPosition;
            mTimeRemaining = mCurrDuration - mLastPlaybackPosition;
            if (playerState.isPaused || mTimeRemaining == 0) {
                pauseRunnable();
            } else {
                setRunnable();
            }
        }
    };


    /**
     * Pauses or resumes remote player based on current pause state.
     * Sends new pause state to receiver.
     */
    private void playPause() {
        if (mPlayerApi != null) {
            mPlayerApi.getPlayerState().setResultCallback(playerState -> {
                if (playerState.isPaused) {
                    mPlayerApi.resume()
                            .setResultCallback(empty -> {
                                Bundle bundle = new Bundle();
                                bundle.putBoolean(PAUSED_KEY, false);
                                mResultReceiver.send(RESULT_PLAY_PAUSE, bundle);
                                Log.d(TAG, "Resume successful");
                            })
                            .setErrorCallback(throwable -> Log.e(TAG, "Error resuming play!", throwable));
                } else {
                    pause();
                }
            });
        }
    }

    private void pause() {
        if (mPlayerApi != null) {
            mPlayerApi.pause()
                    .setResultCallback(empty -> {
                        Bundle bundle = new Bundle();
                        bundle.putBoolean(PAUSED_KEY, true);
                        mResultReceiver.send(RESULT_PLAY_PAUSE, bundle);
                        Log.d(TAG, "Pause successful");
                    })
                    .setErrorCallback(throwable -> Log.e(TAG, "Error pausing play!", throwable));
        }
    }



    private void playNewSong(String spotifyId) {
        if (mPlayerApi != null) {
            mPlayerApi.play("spotify:track:" + spotifyId)
                    .setResultCallback(empty -> {
                mPlayerApi.getPlayerState()
                        .setResultCallback(playerState -> Log.d(TAG, "Play success"))
                        .setErrorCallback(throwable -> Log.e(TAG, "Play error", throwable));
            });
        }
    }



    /**
     * This function retrieves the top song in the locally cached playlist for the current party
     * and deletes it.
     * If the playlist is empty, it logs an error.
     *
     * @return the spotify ID of the song to be played
     */
    private String retrieveNextSong() {
        List<PlaylistEntry> playlist = Party.getCurrentParty().getPlaylist();
        if(playlist.isEmpty()) {
            Log.e(TAG, "Playlist is empty!");
            //TODO - Toast
            return null;
        }
        String spotifyId = playlist.get(0).getSong().getSpotifyId();
        playlist.remove(0); //TODO ???
        return spotifyId;
    }

    /**
     * Seeks to a new position in the current song playing remotely
     *
     * @param progress New position to seek to
     */
    public void seekTo(long progress) {
        if (mPlayerApi != null) {
            mPlayerApi.seekTo(progress)
                    .setResultCallback(empty -> {
                        mLastPlaybackPosition = progress;
                        mTimeRemaining = mCurrDuration - mLastPlaybackPosition;
//                    setRunnable();
                        Log.d(TAG, "Seek success!");
                    })
                    .setErrorCallback(error -> Log.e(TAG, "Cannot seek unless you have premium!", error));
        }
    }


    /**
     * Retrieves the bitmap for the album art of the curently playing song.
     * Converts the bitmap to a byte array and sends to receiver for loading into views.
     */
    private void getAlbumArt() {
        mPlayerApi.getPlayerState().setResultCallback(playerState -> {
            mSpotifyAppRemote.getImagesApi()
                    .getImage(playerState.track.imageUri, Image.Dimension.LARGE)
                    .setResultCallback(bitmap -> {
                        Bundle bundle = new Bundle();

                        // convert bitmap to Byte array
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] byteArray = stream.toByteArray();

                        bundle.putByteArray(IMAGE_KEY,byteArray); //TODO string
                        mResultReceiver.send(RESULT_ALBUM_ART, bundle);
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
