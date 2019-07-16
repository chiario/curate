package com.example.curate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.curate.models.Song;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.ErrorCallback;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.PlayerState;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements SearchAdapter.OnSongAddedListener, QueueAdapter.OnSongLikedListener {

    private static final String KEY_QUEUE_FRAGMENT = "queue";
    private static final String KEY_SEARCH_FRAGMENT = "search";
    private static final String KEY_ACTIVE = "active";
    private String CLIENT_ID;
    private static final String REDIRECT_URI = "http://com.example.curate/callback"; //TODO - change this
    private SpotifyAppRemote mSpotifyAppRemote;
    private static final String TAG = "MainActivity";

    private String testSongId = "37ZJ0p5Jm13JPevGcx4SkF";
    private final ErrorCallback mErrorCallback = throwable -> Log.e(TAG, throwable + "Boom!");

    private FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment;

    Subscription<PlayerState> mPlayerStateSubscription;
//    Subscription<PlayerContext> mPlayerContextSubscription;

    boolean isSpotifyInstalled = false;

    TrackProgressBar mTrackProgressBar;

    QueueFragment queueFragment;
    SearchFragment searchFragment;

    @BindView(R.id.etSearch) EditText etSearch;
    @BindView(R.id.ibSearch) ImageButton ibSearch;
    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvArtist) TextView tvArtist;
    @BindView(R.id.ivAlbum) ImageView ivAlbum;
    @BindView(R.id.seek_to) SeekBar mSeekBar;
    @BindView(R.id.play_pause_button) ImageView mPlayPauseButton;

    // Search Fragment Listener:

    /***
     * Called when user adds a song from the SearchFragment. Adds the song to the (local for now)
     * queue in the QueueFragment
     * @param song Song to be added
     */
	@Override
	public void onSongAdded(Song song) {
		queueFragment.addSong(song);
	}

    /***
     * Called when user likes a song in the QueueFragment
     * @param song
     */
    @Override
    public void onSongLiked(Song song) {
        // TODO
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        CLIENT_ID = getString(R.string.clientId);

        // Check if Spotify app is installed on device
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.spotify.music", 0);
            isSpotifyInstalled = true;
            connectRemotePlayer();
        } catch (PackageManager.NameNotFoundException e) {
            isSpotifyInstalled = false;
            Toast.makeText(this, "Please install the Spotify app to proceed", Toast.LENGTH_LONG).show();
            // TODO prompt installation
        }

        if(savedInstanceState != null) {
            queueFragment = (QueueFragment) fm.findFragmentByTag(KEY_QUEUE_FRAGMENT);
            searchFragment = (SearchFragment) fm.findFragmentByTag(KEY_SEARCH_FRAGMENT);
            activeFragment = fm.findFragmentByTag(savedInstanceState.getString(KEY_ACTIVE));
        }

        if(queueFragment == null) {
            queueFragment = QueueFragment.newInstance();
            fm.beginTransaction().add(R.id.flPlaceholder, queueFragment, KEY_QUEUE_FRAGMENT).hide(queueFragment).commit();
        }
        if(searchFragment == null) {
            searchFragment = SearchFragment.newInstance();
            fm.beginTransaction().add(R.id.flPlaceholder, searchFragment, KEY_SEARCH_FRAGMENT).hide(searchFragment).commit();
        }

        if(activeFragment != null)
            display(activeFragment);
        else
            display(queueFragment);

        ibSearch.setOnClickListener(view -> {
            display(searchFragment);
            searchFragment.setSearchText(etSearch.getText().toString());
            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            //Find the currently focused view, so we can grab the correct window token from it.
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        });


        mSeekBar.setEnabled(false);
        mSeekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        mSeekBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);

        mTrackProgressBar = new TrackProgressBar(mSeekBar);
    }

    /***
     * Saves currently active fragment
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ACTIVE, activeFragment.getTag());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        switch(activeFragment.getTag()) {
            case KEY_QUEUE_FRAGMENT:
                // TODO decide what to do here: could dismiss the fragments and go back to login?
                break;
            case KEY_SEARCH_FRAGMENT:
                activeFragment = queueFragment;
                break;
        }
        etSearch.setText("");
    }


    /***
     * Displays a new fragment and hides previously active fragment
     * @param fragment Fragment to display
     */
    private void display(Fragment fragment) {
        FragmentTransaction ft = fm.beginTransaction();
        if(activeFragment != null)
            ft.hide(activeFragment);
        ft.show(fragment);
        if(fragment.equals(searchFragment) && activeFragment != fragment)
            ft.addToBackStack(fragment.getTag());
        ft.commit();

        activeFragment = fragment;
    }

    private void connectRemotePlayer() {
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.connect(this, connectionParams,
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d(TAG, "Connected! Yay!");
                        playSong(testSongId);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e(TAG, throwable.getMessage(), throwable);
                    }
                });
    }

    public void playSong(String spotifySongId) {
        mSpotifyAppRemote.getPlayerApi().play("spotify:track:" + spotifySongId);
        // Subscribe to PlayerState
        onSubscribeToPlayerState();
    }

    /*@SuppressLint("SetTextI18n")
    private final Subscription.EventCallback<PlayerContext> mPlayerContextEventCallback = new Subscription.EventCallback<PlayerContext>() {
        @Override
        public void onEvent(PlayerContext playerContext) {
            mPlayerContextButton.setText(String.format(Locale.US, "%s\n%s", playerContext.title, playerContext.subtitle));
            mPlayerContextButton.setTag(playerContext);
        }
    };*/

    @SuppressLint("SetTextI18n")
    private final Subscription.EventCallback<PlayerState> mPlayerStateEventCallback = new Subscription.EventCallback<PlayerState>() {
        @Override
        public void onEvent(PlayerState playerState) {
            // Update progressbar
            if (playerState.playbackSpeed > 0) {
                mTrackProgressBar.unpause();
            } else {
                mTrackProgressBar.pause();
            }

            // Invalidate play / pause
            if (playerState.isPaused) {
                mPlayPauseButton.setImageResource(R.drawable.btn_play);
            } else {
                mPlayPauseButton.setImageResource(R.drawable.btn_pause);
            }

            tvTitle.setText(playerState.track.name);
            tvArtist.setText(playerState.track.artist.name);
            // Get image from track
            mSpotifyAppRemote.getImagesApi()
                    .getImage(playerState.track.imageUri, Image.Dimension.LARGE)
                    .setResultCallback(bitmap -> {
                        ivAlbum.setImageBitmap(bitmap);
                    });

            // Invalidate seekbar length and position
            mSeekBar.setMax((int) playerState.track.duration);
            mTrackProgressBar.setDuration(playerState.track.duration);
            mTrackProgressBar.update(playerState.playbackPosition);

            mSeekBar.setEnabled(true);
        }
    };

    public void onSubscribeToPlayerState() {
        if (mPlayerStateSubscription != null && !mPlayerStateSubscription.isCanceled()) {
            mPlayerStateSubscription.cancel();
            mPlayerStateSubscription = null;
        }

        mPlayerStateSubscription = (Subscription<PlayerState>) mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(mPlayerStateEventCallback)
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
                    Log.e(TAG,throwable + "Subscribed to PlayerContext failed!");
                });
    }

    @OnClick(R.id.skip_prev_button)
    public void onSkipPrevious() {
        if (isSpotifyInstalled) {
            mSpotifyAppRemote.getPlayerApi()
                    .skipPrevious()
                    .setResultCallback(empty -> Log.d(TAG, "Skip previous successful"))
                    .setErrorCallback(mErrorCallback);
        } else {
            Toast.makeText(this, "Please install the Spotify app to proceed", Toast.LENGTH_LONG).show();
        }
    }

    @OnClick(R.id.play_pause_button)
    public void onPlayPause() {
        if (isSpotifyInstalled) {
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
        } else {
            Toast.makeText(this, "Please install the Spotify app to proceed", Toast.LENGTH_LONG).show();
        }
    }

    @OnClick(R.id.skip_next_button)
    public void onSkipNext() {
        if (isSpotifyInstalled) {
            mSpotifyAppRemote.getPlayerApi()
                    .skipNext()
                    .setResultCallback(data -> {
                        Log.d(TAG, "Skip next successful");
                    })
                    .setErrorCallback(mErrorCallback);
        } else {
            Toast.makeText(this, "Please install the Spotify app to proceed", Toast.LENGTH_LONG).show();
        }
    }

    private class TrackProgressBar {

        private static final int LOOP_DURATION = 500;
        private final SeekBar mSeekBar;
        private final Handler mHandler;


        private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSpotifyAppRemote.getPlayerApi().seekTo(seekBar.getProgress())
                        .setErrorCallback(mErrorCallback);
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

        private TrackProgressBar(SeekBar seekBar) {
            mSeekBar = seekBar;
            mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
            mHandler = new Handler();
        }

        private void setDuration(long duration) {
            mSeekBar.setMax((int) duration);
        }

        private void update(long progress) {
            mSeekBar.setProgress((int) progress);
        }

        private void pause() {
            mHandler.removeCallbacks(mSeekRunnable);
        }

        private void unpause() {
            mHandler.removeCallbacks(mSeekRunnable);
            mHandler.postDelayed(mSeekRunnable, LOOP_DURATION);
        }
    }
}
