package com.example.curate.activities;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.fragments.InfoDialogFragment;
import com.example.curate.fragments.QueueFragment;
import com.example.curate.fragments.SearchFragment;
import com.example.curate.models.Party;
import com.example.curate.models.Song;
import com.example.curate.utils.LocationManager;
import com.example.curate.utils.Spotify;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.material.appbar.AppBarLayout;
import com.parse.ParseException;
import com.parse.SaveCallback;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerContext;
import com.spotify.protocol.types.PlayerState;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements InfoDialogFragment.InfoDialogListener {

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

    private MenuItem miSearch;
    private MenuItem miInfo;
    private SearchView mSearchView;
    private FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment;
    private Spotify.TrackProgressBar mTrackProgressBar;
    private QueueFragment queueFragment;
    private SearchFragment searchFragment;
    private Party party;
    private SaveCallback mCurrentSongUpdatedCallback;

    private boolean isAdmin = false;

    private Spotify mSpotifyRemote;
    private LocationManager mLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        party = Party.getCurrentParty();

        initializeFragments(savedInstanceState);

        // Checks if user owns the current party and adjusts view
        isAdmin = Party.getCurrentParty().isCurrentUserAdmin();
        Log.d(TAG, "Current user is admin: " + isAdmin);

        int visibility = isAdmin ? View.VISIBLE : View.GONE;
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

        if(isAdmin) {
            mLocationManager = new LocationManager(this);
            if(mLocationManager.hasNecessaryPermissions()) {
                registerLocationUpdater();
            } else {
                mLocationManager.requestPermissions();
            }
        }
    }

    private void initializeFragments(Bundle savedInstanceState) {
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == LocationManager.PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission has been granted, register location updater
                registerLocationUpdater();
            } else {
                Log.i(TAG, "Location permission was not granted.");
            }
        }
    }

    private void registerLocationUpdater() {
        // Update location when user moves
        mLocationManager.registerLocationUpdateCallback(new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                party.updatePartyLocation(LocationManager.createGeoPointFromLocation(locationResult.getLastLocation()), e -> {
                    if (e == null) {
                        Log.d("MainActivity", "Party location updated!");
                    } else {
                        Log.e("MainActivity", "yike couldnt update party location!", e);
                    }
                });
            }
        });

        // Force update location at least once
        mLocationManager.getCurrentLocation(location -> {
            party.updatePartyLocation(LocationManager.createGeoPointFromLocation(location), e -> {
                if (e == null) {
                    Log.d("MainActivity", "Party location updated!");
                } else {
                    Log.e("MainActivity", "yike couldnt update party location!", e);
                }
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        miSearch = menu.findItem(R.id.miSearch);
        miInfo = menu.findItem(R.id.miInfo);
        MenuItem miText = menu.findItem(R.id.miText);
        Drawable d = getDrawable(R.drawable.ic_search);
        if(d != null) {
            d.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
            miSearch.setIcon(d);
        }
        if(miSearch != null) {
            mSearchView = (SearchView) miSearch.getActionView();
            if (mSearchView != null) {
                mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
                mSearchView.setIconifiedByDefault(false);
                mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        search(mSearchView, query);
                        mSearchView.setQuery("", false);
                        miSearch.collapseActionView();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return false;
                    }
                });

                mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean hasFocus) {
                        if(hasFocus) {
                            display(searchFragment);
                            searchFragment.newSearch();
                            miSearch.expandActionView();
                            showKeyboard(view);
                            //TODO show keyboard?
                        }
                    }
                });
                miSearch.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem menuItem) {
                        miText.setVisible(false);
                        new Handler().post(() -> {
                            mSearchView.requestFocus();
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) { // it's never null. I've added this line just to make the compiler happy
                                imm.showSoftInput(mSearchView.findFocus(), 0);
                            }
                        });
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                        miText.setVisible(true);
                        return true;
                    }
                });

            }
        }
        miInfo.setOnMenuItemClickListener(menuItem -> {
            // Retrieve the current party's name
            String name = party.getName();
            String joinCode = party.getJoinCode();
            // Open a new instance of the InfoDialogFragment, passing in the current party's name and code
            InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance(name, joinCode);
            infoDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_FullScreen);
            infoDialogFragment.show(fm, "fragment_party_info");
            return true;
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.miSearch:
                if(!miSearch.isActionViewExpanded()) {
                    miSearch.expandActionView();
                    mSearchView.requestFocus();
                }
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

    @Override
    public void onFinishInfoDialog() {
        onDeleteQueue();
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
                .setPositiveButton("Leave", (dialogInterface, i) -> {
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