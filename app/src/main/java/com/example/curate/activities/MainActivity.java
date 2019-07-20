package com.example.curate.activities;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.adapters.QueueAdapter;
import com.example.curate.adapters.SearchAdapter;
import com.example.curate.fragments.QueueFragment;
import com.example.curate.fragments.SearchFragment;
import com.example.curate.models.Party;
import com.example.curate.models.Song;
import com.example.curate.utils.Spotify;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.parse.ParseException;
import com.parse.SaveCallback;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerContext;
import com.spotify.protocol.types.PlayerState;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements SearchAdapter.OnSongAddedListener, QueueAdapter.OnSongLikedListener {

    private static final String KEY_QUEUE_FRAGMENT = "queue";
    private static final String KEY_SEARCH_FRAGMENT = "search";
    private static final String KEY_ACTIVE = "active";
    private static final String TAG = "MainActivity";

    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvArtist) TextView tvArtist;
    @BindView(R.id.ivAlbum) ImageView ivAlbum;
    @BindView(R.id.seek_to) SeekBar mSeekBar;
    @BindView(R.id.play_pause_button) ImageView mPlayPauseButton;
    @BindView(R.id.clCurrPlaying) ConstraintLayout mPlayerBackground;
    @BindView(R.id.skip_prev_button) ImageView mSkipPrevButton;
    @BindView(R.id.skip_next_button) ImageView mSkipNextButton;
    @BindView(R.id.flPlaceholder) FrameLayout flPlaceholder;
    @BindView(R.id.ablMain) AppBarLayout ablMain;
    @BindView(R.id.tbMain) Toolbar tbMain;
    private FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment;
    private Spotify.TrackProgressBar mTrackProgressBar;
    private QueueFragment queueFragment;
    private SearchFragment searchFragment;
    private Party party;
    private SaveCallback mCurrentSongUpdatedCallback;

    private boolean isAdmin = false;

    private Spotify mSpotifyRemote;


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

        party = Party.getCurrentParty();

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

        // Checks if user owns the current party and adjusts view
        // Checks if user owns the current party and adjusts view
        isAdmin = Party.getCurrentParty().isCurrentUserAdmin();
        Log.d(TAG, "Current user is admin: " + isAdmin);

        int visibility = isAdmin ? View.VISIBLE : View.GONE;
//        ibDeleteQueue.setVisibility(visibility);
//        ibLeaveQueue.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        mPlayPauseButton.setVisibility(visibility);
        mSkipNextButton.setVisibility(visibility);
        mSkipPrevButton.setVisibility(visibility);
        mSeekBar.setVisibility(visibility);
        mSeekBar.setEnabled(false);

        if (isAdmin) {
            mSpotifyRemote = new Spotify(this, mPlayerStateEventCallback, mPlayerContextEventCallback, getString(R.string.clientId));
            mTrackProgressBar = new Spotify.TrackProgressBar(mSpotifyRemote, mSeekBar);
        } else {
            initializeSongUpdateCallback();
        }




        setSupportActionBar(tbMain);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    public int pxToDp(int px) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.miDeleteParty).setVisible(isAdmin);
        menu.findItem(R.id.miLeaveParty).setVisible(!isAdmin);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.miSearch);
        Drawable d = getDrawable(R.drawable.ic_search);
        if(d != null) {
            d.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
            searchItem.setIcon(d);
        }
        if(searchItem != null) {
            final SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        search(searchView, query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return false;
                    }
                });

                searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean hasFocus) {
                        if(hasFocus) {
                            display(searchFragment);
                            searchFragment.newSearch();
                            showKeyboard(view);
                            //TODO show keyboard?
                        }
                    }
                });

            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.miSearch:
                break;
            case R.id.miGenerate:
                break;
            case R.id.miLeaveParty:
                onLeaveQueue();
                break;
            case R.id.miDeleteParty:
                onDeleteQueue();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAdmin) {
            mSpotifyRemote.checkSpotifyInstalled();
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
                // TODO decide what to do here: could dismiss the fragments and go back to login
                //  Maybe exit the party?
                break;
            case KEY_SEARCH_FRAGMENT:
                activeFragment = queueFragment;
//                etSearch.clearFocus();
                break;
        }
//        etSearch.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        party.deregisterPlaylistUpdateCallback(mCurrentSongUpdatedCallback);
    }

    private void initializeSongUpdateCallback() {
        mCurrentSongUpdatedCallback = e -> {
            if (e == null) {
                Song currentSong = party.getCurrentSong();
                if (currentSong != null) {
                    try {
                        currentSong.fetchIfNeeded(); // TODO - work around this fetch; add ParseCloud function??
                        tvTitle.setText(currentSong.getTitle());
                        tvArtist.setText(currentSong.getArtist());
                        Glide.with(this).load(currentSong.getImageUrl()).into(ivAlbum);
                    } catch (ParseException e1) {
                        e1.printStackTrace();
                    }
                }
            } else {
                Log.d(TAG, "Error in song update callback", e);
            }
        };
        party.registerPlaylistUpdateCallback(mCurrentSongUpdatedCallback);
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
        else if(fragment.equals(queueFragment)) {
//            etSearch.clearFocus();
        }
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
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void search(SearchView v, String query) {
        display(searchFragment);
        searchFragment.executeSearch(query);
        hideKeyboard(v);
    }

   // ButterKnife Listeners

//   @OnTextChanged(R.id.etSearch)
//   public void onSearchTextChange(CharSequence text) {
//   	    searchFragment.setSearchText(text.toString());
//   }

/*//    @OnFocusChange(R.id.etSearch)
    public void onSearchFocusChange(boolean hasFocus) {
        if(hasFocus) {
           focusSearch();
        }
    }

//    @OnClick(R.id.clSearch)
    public void onSearchbarClick() {
        focusSearch();
    }*/

//    @OnClick(R.id.ibDeleteQueue)
    public void onDeleteQueue() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete this party?")
                .setMessage("You won't be able to undo this action!")
                .setPositiveButton("Delete", (dialogInterface, i) -> {
                    Party.deleteParty(e -> {
                        Intent intent = new Intent(MainActivity.this, JoinActivity.class);
                        startActivity(intent);
                        finish();
                    });
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> {});

        builder.show();
    }

//    @OnClick(R.id.ibLeaveQueue)
    public void onLeaveQueue() {
        String message = "You can rejoin this party with the following code: " + party.getJoinCode();
        int joinCodeColor = ContextCompat.getColor(this, R.color.colorAccent_text);
        SpannableStringBuilder messageSpan = new SpannableStringBuilder(message);
        messageSpan.setSpan(new ForegroundColorSpan(joinCodeColor),
                message.length() - 4,
                message.length(),
                Spanned.SPAN_INCLUSIVE_INCLUSIVE
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Leave this party?")
                .setMessage(messageSpan)
                .setPositiveButton("Delete", (dialogInterface, i) -> {
                    Party.leaveParty(e -> {
                        Intent intent = new Intent(MainActivity.this, JoinActivity.class);
                        startActivity(intent);
                        finish();
                    });
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> {});

        builder.show();
    }

    //Methods for Spotify remote player communication
    /**
     * Admin's Spotify Player Context event callback
     * Unlocks track progress bar when new track begins
     */
    public final Subscription.EventCallback<PlayerContext> mPlayerContextEventCallback = new Subscription.EventCallback<PlayerContext>() {
        @Override
        public void onEvent(PlayerContext playerContext) {
            Log.d("Spotify.java", playerContext.toString());
            mTrackProgressBar.unlock();
        }
    };

    /**
     * Admin's Spotify Player State event callback
     * Updates current song views whenever player state changes, e.g. on pause, play, new track
     */
    public final Subscription.EventCallback<PlayerState> mPlayerStateEventCallback = new Subscription.EventCallback<PlayerState>() {
        @Override
        public void onEvent(PlayerState playerState) {
            if (playerState.track == null) {
                mTrackProgressBar.pause();
                mPlayPauseButton.setImageResource(R.drawable.btn_play);
                tvTitle.setText("--");
                tvArtist.setText("--");
                mSeekBar.setEnabled(false);
            } else {
                // Update progressbar
                if (playerState.playbackSpeed > 0) {
                    mTrackProgressBar.unpause();
                } else { // Playback is paused or buffering
                    mTrackProgressBar.pause();
                }

                // Set play / pause button
                if (playerState.isPaused) {
                    mPlayPauseButton.setImageResource(R.drawable.btn_play);
                } else {
                    mPlayPauseButton.setImageResource(R.drawable.btn_pause);
                }

                tvTitle.setText(playerState.track.name);
                tvArtist.setText(playerState.track.artist.name);
                // Get image from track
                mSpotifyRemote.setAlbumArt(playerState, ivAlbum);

                // Set seekbar length and position
                mSeekBar.setMax((int) playerState.track.duration);
                mTrackProgressBar.setDuration(playerState.track.duration);
                mTrackProgressBar.update(playerState.playbackPosition);

                mSeekBar.setEnabled(true);
            }
        }
    };

    @OnClick(R.id.skip_prev_button)
    public void onRestartSong() {
        mSpotifyRemote.restartSong();
    }

    @OnClick(R.id.play_pause_button)
    public void onPlayPause() {
        mSpotifyRemote.playPause();
    }

    @OnClick(R.id.skip_next_button)
    public void onSkipNext() {
        party.getNextSong(e -> {
            if (e == null) {
                mSpotifyRemote.playCurrentSong();
            } else {
                Log.e(TAG, "Error getting next song", e);
            }
        });
    }
}