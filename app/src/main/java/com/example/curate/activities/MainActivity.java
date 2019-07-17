package com.example.curate.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.palette.graphics.Palette;

import com.example.curate.R;
import com.example.curate.adapters.QueueAdapter;
import com.example.curate.adapters.SearchAdapter;
import com.example.curate.fragments.QueueFragment;
import com.example.curate.fragments.SearchFragment;
import com.example.curate.models.Party;
import com.example.curate.models.Song;
import com.parse.ParseException;
import com.parse.SaveCallback;
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
    private ConnectionParams mConnectionParams;
    private static final String TAG = "MainActivity";

    private String testSongId = "7GhIk7Il098yCjg4BQjzvb";
    private String testSongId2 = "37ZJ0p5Jm13JPevGcx4SkF";
    private String testSongId3 = "43eBgYRTmu5BJnCJDBU5Hb";
    private final ErrorCallback mErrorCallback = throwable -> Log.e(TAG, throwable + "Boom!");

    private FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment;

    Subscription<PlayerState> mPlayerStateSubscription;

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
    @BindView(R.id.clCurPlaying) ConstraintLayout mPlayerBackground;
    @BindView(R.id.ibDeleteQueue) ImageButton ibDeleteQueue;


    Connector.ConnectionListener mConnectionListener = new Connector.ConnectionListener() {
        @Override
        public void onConnected(SpotifyAppRemote spotifyAppRemote) {
            mSpotifyAppRemote = spotifyAppRemote;
            Log.d(TAG, "Connected!");
            // Subscribe to PlayerState
            onSubscribeToPlayerState();
            playNext(); //TODO - decide what to do on app creation, i.e. start playing immediately or wait for prompt from admin
        }

        @Override
        public void onFailure(Throwable error) {
            if (error instanceof NotLoggedInException || error instanceof UserNotAuthorizedException) {
                Log.e(TAG, error.toString());
                // Show login button and trigger the login flow from auth library when clicked TODO
                Toast.makeText(MainActivity.this, "Please log in to Spotify to proceed", Toast.LENGTH_LONG).show();
            } else if (error instanceof CouldNotFindSpotifyApp) {
                Log.e(TAG, error.toString());
                // Show button to download Spotify TODO
                Toast.makeText(MainActivity.this, "Please install the Spotify app to proceed", Toast.LENGTH_LONG).show();
            }
        }
    };


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
        mConnectionParams = new ConnectionParams.Builder(CLIENT_ID)
                    .setRedirectUri(REDIRECT_URI)
                    .showAuthView(true)
                    .build();

        SpotifyAppRemote.connect(this, mConnectionParams, mConnectionListener);

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

        if(activeFragment != null) {
            display(activeFragment);
        } else {
            display(queueFragment);
        }

        ibSearch.setOnClickListener(this::search);

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(v);
                return true;
            }
            return false;
        });

        mSeekBar.setEnabled(false);
//        mSeekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
//        mSeekBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        mTrackProgressBar = new TrackProgressBar(mSeekBar);

    }

    private void search(View v) {
        display(searchFragment);
        searchFragment.setSearchText(etSearch.getText().toString());
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if Spotify app is installed on device
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.spotify.music", 0);
            isSpotifyInstalled = true;
//            SpotifyAppRemote.connect(this, mConnectionParams, mConnectionListener);
        } catch (PackageManager.NameNotFoundException e) {
            isSpotifyInstalled = false;
            Toast.makeText(this, "Please install the Spotify app to proceed", Toast.LENGTH_LONG).show();
            // TODO prompt installation
        }
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

    private void setPlayerColor(Bitmap bitmap) {
        Palette.from(bitmap).generate(p -> {
            // Load default colors
            int backgroundColor = ContextCompat.getColor(MainActivity.this, R.color.asphalt);
            int textColor = ContextCompat.getColor(MainActivity.this, R.color.white);

            Palette.Swatch swatch = p.getDarkMutedSwatch();
            if(swatch != null) {
                backgroundColor = swatch.getRgb();
            }

            mPlayerBackground.setBackgroundColor(backgroundColor);
            tvTitle.setTextColor(textColor);
            tvArtist.setTextColor(textColor);
        });
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
//                        setPlayerColor(bitmap);
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
            mPlayerStateSubscription.cancel(); // TODO - do we really want to cancel and remake the subscription every time?
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

    @OnClick(R.id.ibDeleteQueue)
    public void onDeleteQueue(View v) {
        Party.deleteParty(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                Intent intent = new Intent(MainActivity.this, JoinActivity.class);
                startActivity(intent);
                finish();
            }
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
            playNext();
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
                int timeRemaining = mSeekBar.getMax() - progress;
//                Log.d(TAG, "Time Remaining: " + timeRemaining);
                if (timeRemaining < 1500) {
                    playNext();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSpotifyAppRemote.getPlayerApi().seekTo(seekBar.getProgress())
                        .setErrorCallback(error -> {
                            mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(
                                    playerState -> mTrackProgressBar.update(playerState.playbackPosition)
                            );
                            Toast.makeText(MainActivity.this, "Can't seek unless you have premium!", Toast.LENGTH_SHORT).show();
                        });
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

    private void playNext() {
        Log.d(TAG, "Get ready to play next song");
//        mSpotifyAppRemote.getPlayerApi().play("spotify:track:" + testSongId3);

        String songId = queueFragment.getNextSong();
        mSpotifyAppRemote.getPlayerApi().play("spotify:track:" + songId);
    }
}