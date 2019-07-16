package com.example.curate;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.curate.models.Song;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Track;

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

    private FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment;

    QueueFragment queueFragment;
    SearchFragment searchFragment;

    @BindView(R.id.etSearch) EditText etSearch;
    @BindView(R.id.ibSearch) ImageButton ibSearch;

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
        PackageManager pm = getPackageManager();
        boolean isSpotifyInstalled;
        try {
            pm.getPackageInfo("com.spotify.music", 0);
            isSpotifyInstalled = true;
            connectRemotePlayer();
        } catch (PackageManager.NameNotFoundException e) {
            isSpotifyInstalled = false;
            Toast.makeText(this, "Please install the Spotify app to proceed", Toast.LENGTH_LONG).show(); // TODO prompt installation
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
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    final Track track = playerState.track;
                    if (track != null) {
                        Log.d(TAG, track.name + " by " + track.artist.name);
                    }
                });
    }

    @OnClick(R.id.ibPlayPause)
    public void playPause() {
	    playSong(testSongId);
    }
}
