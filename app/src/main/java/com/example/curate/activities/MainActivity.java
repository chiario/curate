package com.example.curate.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import com.example.curate.utils.Spotify;
import com.parse.ParseUser;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;

public class MainActivity extends AppCompatActivity implements SearchAdapter.OnSongAddedListener, QueueAdapter.OnSongLikedListener {

    private static final String KEY_QUEUE_FRAGMENT = "queue";
    private static final String KEY_SEARCH_FRAGMENT = "search";
    private static final String KEY_ACTIVE = "active";
    private static final String TAG = "MainActivity";
    @BindView(R.id.etSearch) EditText etSearch;
    @BindView(R.id.ibSearch) ImageButton ibSearch;
    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvArtist) TextView tvArtist;
    @BindView(R.id.ivAlbum) ImageView ivAlbum;
    @BindView(R.id.seek_to) SeekBar mSeekBar;
    @BindView(R.id.play_pause_button) ImageView mPlayPauseButton;
    @BindView(R.id.clCurPlaying) ConstraintLayout mPlayerBackground;
    @BindView(R.id.ibDeleteQueue) ImageButton ibDeleteQueue;
    @BindView(R.id.ibLeaveQueue) ImageButton ibLeaveQueue;
    @BindView(R.id.skip_prev_button) ImageView mSkipPrevButton;
    @BindView(R.id.skip_next_button) ImageView mSkipNextButton;
    private FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment;
    private TrackProgressBar mTrackProgressBar;
    private QueueFragment queueFragment;
    private SearchFragment searchFragment;
    private boolean isSpotifyInstalled = false;
    private boolean isAdmin = false;

    //TODO - delete these
    private String testSongId = "7GhIk7Il098yCjg4BQjzvb";
    private String testSongId2 = "37ZJ0p5Jm13JPevGcx4SkF";
    private String testSongId3 = "43eBgYRTmu5BJnCJDBU5Hb";

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

        // Checks if user owns the current party and adjusts view
        isAdmin = ParseUser.getCurrentUser().getObjectId().equals(Party.getCurrentParty().getAdmin().getObjectId()); // TODO - fix this code
        Log.d(TAG, "Current user is admin: " + isAdmin);

        int visibility = isAdmin ? View.VISIBLE : View.GONE;
        ibDeleteQueue.setVisibility(visibility);
        ibLeaveQueue.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        mPlayPauseButton.setVisibility(visibility);
        mSkipNextButton.setVisibility(visibility);
        mSkipPrevButton.setVisibility(visibility);
        mSeekBar.setVisibility(visibility);
        if (isAdmin) {
            Spotify.connectRemote(this, mPlayerStateEventCallback, getString(R.string.clientId));
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

        if(activeFragment != null) {
            display(activeFragment);
        } else {
            display(queueFragment);
        }

        mSeekBar.setEnabled(false);
        mTrackProgressBar = new TrackProgressBar(mSeekBar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAdmin) {
            // Check if Spotify app is installed on device
            PackageManager pm = getPackageManager();
            try {
                pm.getPackageInfo("com.spotify.music", 0);
                isSpotifyInstalled = true;
            } catch (PackageManager.NameNotFoundException e) {
                isSpotifyInstalled = false;
                Toast.makeText(this, "Please install the Spotify app to proceed", Toast.LENGTH_LONG).show();
                // TODO prompt installation
            }
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
                etSearch.clearFocus();
                break;
        }
        etSearch.setText("");
    }

    public boolean isAdmin() {
        return isAdmin;
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
        if(fragment == null || fragment.equals(activeFragment)) return;
        FragmentTransaction ft = fm.beginTransaction();
        if(activeFragment != null)
            ft.hide(activeFragment);
        ft.show(fragment);
        if(fragment.equals(searchFragment))
            ft.addToBackStack(fragment.getTag());
        else if(fragment.equals(queueFragment))
            etSearch.clearFocus();
        ft.commit();

        activeFragment = fragment;
    }

   /* TODO Decide if we want this?
    @OnTextChanged(R.id.etSearch)
    public void onSearchTextChange() {
        searchFragment.setSearchText(etSearch.getText().toString());
    }*/

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    private void search(View v) {
        display(searchFragment);
        searchFragment.executeSearch(etSearch.getText().toString());
        etSearch.clearFocus();
        hideKeyboard(v);
    }

    public void focusSearch() {
        etSearch.requestFocus();
        showKeyboard(etSearch);
        display(searchFragment);
        searchFragment.newSearch();
    }

   // ButterKnife Listeners

//   @OnTextChanged(R.id.etSearch)
//   public void onSearchTextChange(CharSequence text) {
//   	    searchFragment.setSearchText(text.toString());
//   }



    @OnFocusChange(R.id.etSearch)
    public void onSearchFocusChange(boolean hasFocus) {
        if(hasFocus) {
           focusSearch();
        }
    }

    @OnClick(R.id.clSearch)
    public void onSearchbarClick() {
        focusSearch();
    }

    @OnClick(R.id.ibSearch)
    public void onSearchClick(View v) {
        if(!etSearch.hasFocus())
            focusSearch();
        else
   	        search(v);
    }

    @OnEditorAction(R.id.etSearch)
    public void onSearchEditorAction(TextView tv, int actionId) {
       if (actionId == EditorInfo.IME_ACTION_SEARCH)
           search((View) tv);
    }

    @OnClick(R.id.ibDeleteQueue)
    public void onDeleteQueue(View v) {
        Party.deleteParty(e -> {
            Intent intent = new Intent(MainActivity.this, JoinActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @OnClick(R.id.ibLeaveQueue)
    public void onLeaveQueue(View v) {
        Party.leaveParty(e -> {
            Intent intent = new Intent(MainActivity.this, JoinActivity.class);
            startActivity(intent);
            finish();
        });
    }

    //Methods for Spotify remote player communication

    /**
     * Spotify Player event callback
     * Updates current song views when new song begins playing
     */
//    @SuppressLint("SetTextI18n")
    public final Subscription.EventCallback<PlayerState> mPlayerStateEventCallback = new Subscription.EventCallback<PlayerState>() {
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
            Spotify.setAlbumArt(playerState, ivAlbum);

            // Invalidate seekbar length and position
            mSeekBar.setMax((int) playerState.track.duration);
            mTrackProgressBar.setDuration(playerState.track.duration);
            mTrackProgressBar.update(playerState.playbackPosition);

            mSeekBar.setEnabled(true);
        }
    };

    @OnClick(R.id.skip_prev_button)
    public void onRestartSong() {
        Spotify.restartSong();
    }

    @OnClick(R.id.play_pause_button)
    public void onPlayPause() {
        Spotify.playPause();
    }

    @OnClick(R.id.skip_next_button)
    public void onSkipNext() {
        String songId = queueFragment.getNextSong();
        Spotify.playNextSong(songId);
    }

    public class TrackProgressBar {
        private static final int LOOP_DURATION = 500;
        private final SeekBar mSeekBar;
        private final Handler mHandler;

        private TrackProgressBar(SeekBar seekBar) {
            mSeekBar = seekBar;
            mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
            mHandler = new Handler();
        }

        private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int timeRemaining = mSeekBar.getMax() - progress;
                if (timeRemaining < 1500 && isAdmin) {
                    Spotify.playNextSong(queueFragment.getNextSong());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Spotify.seekTo(seekBar.getProgress(), mTrackProgressBar);
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


        private void setDuration(long duration) {
            mSeekBar.setMax((int) duration);
        }

        public void update(long progress) {
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