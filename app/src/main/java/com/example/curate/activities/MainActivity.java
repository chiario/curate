package com.example.curate.activities;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.curate.R;
import com.example.curate.fragments.AdminPlayerFragment;
import com.example.curate.fragments.InfoDialogFragment;
import com.example.curate.fragments.PlayerFragment;
import com.example.curate.fragments.QueueFragment;
import com.example.curate.fragments.SearchFragment;
import com.example.curate.fragments.SettingsDialogFragment;
import com.example.curate.models.Party;
import com.example.curate.models.User;
import com.example.curate.utils.LocationManager;
import com.example.curate.utils.NotificationHelper;
import com.example.curate.utils.ReverseInterpolator;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.material.appbar.AppBarLayout;
import com.parse.Parse;
import com.parse.ParseUser;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements InfoDialogFragment.OnDeleteListener {

    private static final String KEY_QUEUE_FRAGMENT = "queue";
    private static final String KEY_SEARCH_FRAGMENT = "search";
    private static final String KEY_ACTIVE = "active";
    private static final String TAG = "MainActivity";

    @BindView(R.id.flPlaceholder) FrameLayout flPlaceholder;
    @BindView(R.id.ablMain) AppBarLayout ablMain;
    @BindView(R.id.tbMain) Toolbar tbMain;
    @BindView(R.id.flBottomPlayer) FrameLayout flBottomPlayer;
    @BindView(R.id.miSearch) SearchView miSearchView;
    @BindView(R.id.ivSearchBackground) ImageView ivSearchBackground;
    @BindView(R.id.ibBack) ImageButton ibBack;

    private Party mCurrentParty;
    private FragmentManager mFragmentManager;
    private Fragment mActiveFragment;
    private QueueFragment mQueueFragment;
    private SearchFragment mSearchFragment;
    private PlayerFragment mBottomPlayerFragment;
    private ValueAnimator mSearchbarAnimator;
    private boolean mIsSearchbarExpanded = false;
    private boolean mIsAdmin = false;
    private LocationManager mLocationManager;
    private LocationCallback mLocationCallback = null;

    private User.PartyDeletedListener mPartyDeleteListener;

    public AdminPlayerFragment getBottomPlayerFragment() {
        if(!mIsAdmin) return null;
        return (AdminPlayerFragment) mBottomPlayerFragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mCurrentParty = Party.getCurrentParty();
        mIsAdmin = mCurrentParty.isCurrentUserAdmin();

        initializeFragments(savedInstanceState);

        if (mIsAdmin) {
            mBottomPlayerFragment = AdminPlayerFragment.newInstance();
            // Set up the location manager
            mLocationManager = new LocationManager(this);
            if(mLocationManager.hasNecessaryPermissions() && mCurrentParty.getLocationEnabled()) {
                registerLocationUpdater();
            } else {
                mLocationManager.requestPermissions();
            }
        } else {
            mBottomPlayerFragment = PlayerFragment.newInstance();
            // Set up push notifications
            NotificationHelper.initializeNotifications(this);

            setPartyDeleteListener();
        }
        mFragmentManager.beginTransaction().replace(R.id.flBottomPlayer, mBottomPlayerFragment).commit();

        setSupportActionBar(tbMain);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        initSearchBarAnimations();

//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        AnimationDrawable backgroundAnimation = (AnimationDrawable) findViewById(R.id.rootView).getBackground();
        backgroundAnimation.setEnterFadeDuration(10);
        backgroundAnimation.setExitFadeDuration(getResources().getInteger(R.integer.anim_gradient_transition_time));
        backgroundAnimation.start();
    }

    private void setPartyDeleteListener() {
        if(mPartyDeleteListener != null) {
            ((User) ParseUser.getCurrentUser()).deregisterPartyDeletedListener(mPartyDeleteListener);
            mPartyDeleteListener = null;
        }

        mPartyDeleteListener = mainActivity -> runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            builder
                    .setTitle("Party Deleted")
                    .setMessage("This party has been deleted by the admin.")
                    .setPositiveButton("Return to menu", (dialogInterface, i) -> {
                        removePartyDeleteListener();
                        Intent intent = new Intent(mainActivity, JoinActivity.class);
                        startActivity(intent);
                        Party.partyDeleted();
                        finish();
                    });
            builder.setCancelable(false);
            builder.show();
        });

        ((User) ParseUser.getCurrentUser()).registerPartyDeletedListener(mPartyDeleteListener, this);
    }

    private void removePartyDeleteListener() {
        ((User) ParseUser.getCurrentUser()).deregisterPartyDeletedListener(mPartyDeleteListener);
        mPartyDeleteListener = null;
    }

    private void initializeFragments(Bundle savedInstanceState) {
        mFragmentManager = getSupportFragmentManager();
        if(savedInstanceState != null) {
            mQueueFragment = (QueueFragment) mFragmentManager.getFragment(savedInstanceState, KEY_QUEUE_FRAGMENT);
            mSearchFragment = (SearchFragment) mFragmentManager.getFragment(savedInstanceState, KEY_SEARCH_FRAGMENT);
            mActiveFragment = savedInstanceState.getString(KEY_ACTIVE).equals(KEY_QUEUE_FRAGMENT)
                    ? mQueueFragment : mSearchFragment;
        }

        if(mQueueFragment == null) {
            mQueueFragment = QueueFragment.newInstance();
            mFragmentManager.beginTransaction().add(R.id.flPlaceholder, mQueueFragment, KEY_QUEUE_FRAGMENT).hide(mQueueFragment).commit();
        }
        if(mSearchFragment == null) {
            mSearchFragment = SearchFragment.newInstance();
            mFragmentManager.beginTransaction().add(R.id.flPlaceholder, mSearchFragment, KEY_SEARCH_FRAGMENT).hide(mSearchFragment).commit();
        }

        if(mActiveFragment != null) {
            display(mActiveFragment);
        } else {
            display(mQueueFragment);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem miInfo = menu.findItem(R.id.miInfo);
        MenuItem miSettings = menu.findItem(R.id.miSettings);

        if (miSearchView != null) {
            initializeSearch();
        }

        // Set info option onClick callback
        miInfo.setOnMenuItemClickListener(menuItem -> {
            // Retrieve the current mParty's name and join code
            String name = Party.getCurrentParty().getName();
            String joinCode = Party.getCurrentParty().getJoinCode();

            InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance(name, joinCode, mIsAdmin);
            infoDialogFragment.show(mFragmentManager, "fragment_party_info");
            return true;
        });

        // Set setting option onClick callback
        miSettings.setOnMenuItemClickListener(menuItem -> {
            SettingsDialogFragment settings = SettingsDialogFragment.newInstance();
            settings.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_Fullscreen);
            settings.show(mFragmentManager, "fragment_admin_settings");
            return true;
        });

        // Only show settings for admin
        miSettings.setVisible(mIsAdmin);
        miSettings.setEnabled(mIsAdmin);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Initializes the searchbar onclicks
     */
    private void initializeSearch() {
        // Set up back button onClick
        ibBack.setOnClickListener(view -> {
            onBackPressed();
            if(miSearchView.hasFocus()) {
                miSearchView.clearFocus();
                hideKeyboard(miSearchView);
            }
        });

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        miSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        miSearchView.setIconifiedByDefault(false);

        // Set SearchView callbacks
        miSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                search(query, miSearchView);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                liveSearch(query);
                return true;
            }
        });

        // Set up fragment transition on focus change
        miSearchView.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
            if(hasFocus) {
                display(mSearchFragment);
                showKeyboard(view);
                mBottomPlayerFragment.setExpanded(false);
                animateSearchbar(true);
            }
        });
    }

    /***
     * Saves currently active fragment
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, KEY_ACTIVE, mActiveFragment);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        switch(mActiveFragment.getTag()) {
            case KEY_QUEUE_FRAGMENT:
                break;
            case KEY_SEARCH_FRAGMENT:
                mSearchFragment.clear();
                miSearchView.setQuery("", false);
                animateSearchbar(false);
                display(mQueueFragment);
                break;
        }
    }

    /***
     * Displays a new fragment and hides previously active fragment
     * @param fragment Fragment to display
     */
    private void display(Fragment fragment) {
        NotificationHelper.updateInteractionTime();
        if(fragment == null || fragment.equals(mActiveFragment)) return;
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        if(mActiveFragment != null)
            ft.hide(mActiveFragment);
        ft.show(fragment);
        if(fragment.equals(mSearchFragment)) {
            mSearchFragment.clear();
            ft.addToBackStack(fragment.getTag());
        }
        ft.commit();

        mActiveFragment = fragment;
    }

    /**
     * Initializes the search animation callbacks for animating the search bar
     */
    private void initSearchBarAnimations() {

        // Get the display metrics for window width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        // Set the maximum values
        float maxHeight = getResources().getDimension(R.dimen.max_searchbar_height);
        float maxWidth = displayMetrics.widthPixels;
        float maxDelta = getResources().getDimension(R.dimen.max_searchbar_delta);
        float maxRadius = getResources().getDimension(R.dimen.max_searchbar_radius);

        // Set searchbar corner radius to initial/maximum value
        GradientDrawable searchbarBackground = (GradientDrawable) ContextCompat.getDrawable(
                this, R.drawable.bg_searchbar);
        searchbarBackground.setCornerRadius(maxRadius);
        ivSearchBackground.setBackground(searchbarBackground);

        // Set searchbar width and height to initial/maximum values
        ViewGroup.LayoutParams params = ivSearchBackground.getLayoutParams();
        params.height = (int) (maxHeight - maxDelta / 1.5f);
        params.width = (int) (maxWidth - maxDelta);
        ivSearchBackground.setLayoutParams(params);


        // Set up animator from 1 to 0
        mSearchbarAnimator = ValueAnimator.ofFloat(1f, 0f);
        mSearchbarAnimator.setDuration(200);

        // Update listener callback called every "tick"
        mSearchbarAnimator.addUpdateListener(anim -> {
            // Set the animated delta and radius (percentage of maximum values)
            float currentDelta = maxDelta * (Float) anim.getAnimatedValue();
            float currentRadius = maxRadius * (Float) anim.getAnimatedValue();

            // Set the layout height and width for the searchbar based on current delta
            ViewGroup.LayoutParams layout = ivSearchBackground.getLayoutParams();
            layout.height = (int) (maxHeight - currentDelta / 1.5f);
            layout.width = (int) (maxWidth - currentDelta);
            ivSearchBackground.setLayoutParams(layout);

            // Set the search bar corner radius based on current radius
            searchbarBackground.setCornerRadius(currentRadius);
        });
    }

    private void animateSearchbar(boolean isExpanding) {
        if(mIsSearchbarExpanded == isExpanding)
            return;
        ibBack.setVisibility(isExpanding ? View.VISIBLE : View.GONE);
        mIsSearchbarExpanded = isExpanding;
        mSearchbarAnimator.setInterpolator(isExpanding
                ? new AccelerateDecelerateInterpolator()
                : new ReverseInterpolator(new AccelerateDecelerateInterpolator()));
        mSearchbarAnimator.start();
    }

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

    private void search(String query, SearchView v) {
        display(mSearchFragment);
        mSearchFragment.executeSearch(query);
        hideKeyboard(v);
        miSearchView.clearFocus();
    }

    private void liveSearch(String query) {
        display(mSearchFragment);
        mSearchFragment.updateLiveSearch(query);
    }


    @Override
    public void onLeaveQueue() {
        mCurrentParty.leaveParty(e -> {
            NotificationHelper.removeNotifications(MainActivity.this);
            removePartyDeleteListener();
            ((User) ParseUser.getCurrentUser()).setScreenName(null);
            Intent intent = new Intent(MainActivity.this, JoinActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onDeleteQueue() {
        mCurrentParty.deleteParty(e -> {
            ((User) ParseUser.getCurrentUser()).setScreenName(null);
            Intent intent = new Intent(MainActivity.this, JoinActivity.class);
            startActivity(intent);
            finish();
        });
    }

    public void registerLocationUpdater() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Party.getCurrentParty().updatePartyLocation(LocationManager.createGeoPointFromLocation(locationResult.getLastLocation()), e -> {
                    if (e == null) {
                        Log.d(TAG, "Party location updated!");
                    } else {
                        Log.e(TAG, "yike couldnt update party location!", e);
                    }
                });
            }
        };

        // Update location when user moves
        mLocationManager.registerLocationUpdateCallback(mLocationCallback);

        // Force update location at least once
        mLocationManager.getCurrentLocation(location -> {
            if(location != null) {
                Party.getCurrentParty().updatePartyLocation(LocationManager.createGeoPointFromLocation(location), e -> {
                    if (e == null) {
                        Log.d(TAG, "Party location updated!");
                    } else {
                        Log.e(TAG, "yike couldnt update party location!", e);
                    }
                });
            }
        });
    }

    public void deregisterLocationUpdater() {
        if(mLocationCallback == null) return;
        mLocationManager.deregisterLocationUpdateCallback(mLocationCallback);
        mCurrentParty.clearLocation(e -> {
            if(e == null) {
                mLocationCallback = null;
            } else {
                registerLocationUpdater();
                Log.e(TAG, "Couldn't clear location!", e);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == LocationManager.PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && mCurrentParty.getLocationEnabled()) {
                // Location permission has been granted, register location updater
                registerLocationUpdater();
            } else {
                Log.i(TAG, "Location permission was not granted.");
            }
        }
    }
}