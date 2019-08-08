package com.example.curate.activities;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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
import com.example.curate.utils.ToastHelper;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.material.appbar.AppBarLayout;
import com.parse.ParseUser;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_QUEUE_FRAGMENT = "queue";
    private static final String KEY_SEARCH_FRAGMENT = "search";
    private static final String KEY_ACTIVE = "active";
    private static final String TAG = "MainActivity";

    protected static final String KEY_PARTY_DELETED = "partyDeleted";

    @BindView(R.id.flPlaceholder)
    FrameLayout flPlaceholder;
    @BindView(R.id.ablMain)
    AppBarLayout ablMain;
    @BindView(R.id.tbMain)
    Toolbar tbMain;
    @BindView(R.id.flBottomPlayer)
    FrameLayout flBottomPlayer;
    @BindView(R.id.miSearch)
    SearchView miSearchView;
    @BindView(R.id.ivSearchBackground)
    ImageView ivSearchBackground;
    @BindView(R.id.ibBack)
    ImageButton ibBack;
    @BindView(R.id.ibOverflow)
    ImageButton ibOverflow;
    @BindView(R.id.clSearchbar)
    ConstraintLayout clSearchbar;

    private Party mCurrentParty;
    private FragmentManager mFragmentManager;
    private Fragment mActiveFragment;
    private QueueFragment mQueueFragment;
    private SearchFragment mSearchFragment;
    private PlayerFragment mBottomPlayerFragment;

    private boolean mIsSearchbarExpanded = false;

    private boolean mIsAdmin = false;
    private LocationManager mLocationManager;
    private LocationCallback mLocationCallback = null;

    private boolean mIsLeavingQueue = false;

    private User.PartyDeletedListener mPartyDeleteListener;

    public AdminPlayerFragment getBottomPlayerFragment() {
        if (!mIsAdmin) return null;
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

        ToastHelper.setBottomToaster((context, text) -> {
            View layout = getLayoutInflater().inflate(R.layout.toast_main, findViewById(R.id.clToast));
            ((TextView) layout.findViewById(R.id.tvMessage)).setText(text);
            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.BOTTOM, 0, (int) context.getResources().getDimension(R.dimen.toast_margin)
                    + (int) mBottomPlayerFragment.getHeight());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
        });

        if (mIsAdmin) {
            mBottomPlayerFragment = AdminPlayerFragment.newInstance();
            // Set up the location manager
            mLocationManager = new LocationManager(this);
            if (mLocationManager.hasNecessaryPermissions() && mCurrentParty.getLocationEnabled()) {
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

        if (User.getCurrentScreenName() == null) {
            setUserName();
        }
    }

    private void setUserName() {
        User user = (User) ParseUser.getCurrentUser();
        View inputView = getLayoutInflater().inflate(R.layout.fragment_input, null);
        EditText etInput = inputView.findViewById(R.id.etInput);
        etInput.setHint("Name");
        TextView tvTitle = inputView.findViewById(R.id.tvTitle);
        tvTitle.setText("Set your name...");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(inputView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        etInput.setOnEditorActionListener((textView, i, keyEvent) -> {
            ToastHelper.makeText(MainActivity.this, "Time to rock out!", true);
            dialog.dismiss();
            user.setScreenName(etInput.getText().toString());
            return true;
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void setPartyDeleteListener() {
        if (mPartyDeleteListener != null) {
            ((User) ParseUser.getCurrentUser()).deregisterPartyDeletedListener(mPartyDeleteListener);
            mPartyDeleteListener = null;
        }

        mPartyDeleteListener = mainActivity -> runOnUiThread(() -> {
            if (!mIsLeavingQueue) {
                removePartyDeleteListener();
                Intent intent = new Intent(mainActivity, JoinActivity.class);
                intent.putExtra(KEY_PARTY_DELETED, true);
                startActivity(intent);
                Party.partyDeleted();
                finish();
            }
        });

        ((User) ParseUser.getCurrentUser()).registerPartyDeletedListener(mPartyDeleteListener, this);
    }

    private void removePartyDeleteListener() {
        ((User) ParseUser.getCurrentUser()).deregisterPartyDeletedListener(mPartyDeleteListener);
        mPartyDeleteListener = null;
    }

    private void initializeFragments(Bundle savedInstanceState) {
        mFragmentManager = getSupportFragmentManager();
        if (savedInstanceState != null) {
            mQueueFragment = (QueueFragment) mFragmentManager.getFragment(savedInstanceState, KEY_QUEUE_FRAGMENT);
            mSearchFragment = (SearchFragment) mFragmentManager.getFragment(savedInstanceState, KEY_SEARCH_FRAGMENT);
            mActiveFragment = savedInstanceState.getString(KEY_ACTIVE).equals(KEY_QUEUE_FRAGMENT)
                    ? mQueueFragment : mSearchFragment;
        }

        if (mQueueFragment == null) {
            mQueueFragment = QueueFragment.newInstance();
            mFragmentManager.beginTransaction().add(R.id.flPlaceholder, mQueueFragment, KEY_QUEUE_FRAGMENT).hide(mQueueFragment).commit();
        }
        if (mSearchFragment == null) {
            mSearchFragment = SearchFragment.newInstance();
            mFragmentManager.beginTransaction().add(R.id.flPlaceholder, mSearchFragment, KEY_SEARCH_FRAGMENT).hide(mSearchFragment).commit();
        }

        if (mActiveFragment != null) {
            display(mActiveFragment);
        } else {
            display(mQueueFragment);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        if (miSearchView != null) {
            initializeSearch();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @OnClick(R.id.ibOverflow)
    public void onClickOverflow() {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, ibOverflow, Gravity.END, 0, R.style.Curate_MenuPopup);

        popupMenu.inflate(R.menu.menu_overflow);
        MenuItem miSettings = popupMenu.getMenu().findItem(R.id.miSettings);
        MenuItem miLeave = popupMenu.getMenu().findItem(R.id.miLeave);
        MenuItem miDelete = popupMenu.getMenu().findItem(R.id.miDelete);

        // Only show settings for admin
        miSettings.setVisible(mIsAdmin);
        miSettings.setEnabled(mIsAdmin);

        // Show delete for admin
        miDelete.setVisible(mIsAdmin);
        miDelete.setEnabled(mIsAdmin);

        // Show leave for clients
        miLeave.setVisible(!mIsAdmin);
        miLeave.setEnabled(!mIsAdmin);

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.miDelete:
                    deleteQueue();
                    return true;
                case R.id.miLeave:
                    leaveQueue();
                    return true;
                case R.id.miInfo:
                    String name = Party.getCurrentParty().getName();
                    String joinCode = Party.getCurrentParty().getJoinCode();
                    InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance(name, joinCode, mIsAdmin);
                    infoDialogFragment.show(mFragmentManager, "fragment_party_info");
                    return true;
                case R.id.miSettings:
                    SettingsDialogFragment settings = SettingsDialogFragment.newInstance();
                    settings.setStyle(DialogFragment.STYLE_NORMAL, R.style.Curate_AlertDialog_Fullscreen);
                    settings.show(mFragmentManager, "fragment_admin_settings");
                    return true;
            }
            return false;
        });
        popupMenu.show();
    }

    /**
     * Initializes the searchbar onclicks
     */
    private void initializeSearch() {
        // Set up back button onClick
        ibBack.setOnClickListener(view -> {
            onBackPressed();
            if (miSearchView.hasFocus()) {
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
            if (hasFocus) {
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
        switch (mActiveFragment.getTag()) {
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
        if (fragment == null || fragment.equals(mActiveFragment)) return;
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        if (mActiveFragment != null)
            ft.hide(mActiveFragment);
        ft.show(fragment);
        if (fragment.equals(mSearchFragment)) {
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
    }

    private void animateSearchbar(boolean isExpanding) {
        if (mIsSearchbarExpanded == isExpanding)
            return;
        ibBack.setVisibility(isExpanding ? View.VISIBLE : View.GONE);
        mIsSearchbarExpanded = isExpanding;

        // Start transition
        TransitionManager.beginDelayedTransition(clSearchbar);

        // Get the display metrics for window width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        // Get the maximum values
        float maxHeight = getResources().getDimension(R.dimen.max_searchbar_height);
        float maxWidth = displayMetrics.widthPixels;
        float maxDelta = getResources().getDimension(R.dimen.max_searchbar_delta);
        float maxRadius = getResources().getDimension(R.dimen.max_searchbar_radius);

        // Apply changes to layout
        ((GradientDrawable) ivSearchBackground.getBackground()).setCornerRadius(isExpanding ? maxRadius - maxDelta : maxRadius);
        ViewGroup.LayoutParams layout = ivSearchBackground.getLayoutParams();
        layout.height = (int) (isExpanding ? maxHeight : maxHeight - maxDelta);
        layout.width = (int) (isExpanding ? maxWidth : maxWidth - maxDelta);
        ivSearchBackground.setLayoutParams(layout);

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

    private void leaveQueue() {
        showExitDialog("Leave this party?", "You can rejoin with the code " + Party.getCurrentParty().getJoinCode(),
                "Leave", view -> {
                    mIsLeavingQueue = true;
                    mCurrentParty.leaveParty(e -> {
                        if (e != null) {
                            mIsLeavingQueue = false;
                        } else {
                            NotificationHelper.removeNotifications(MainActivity.this);
                            removePartyDeleteListener();
                            ((User) ParseUser.getCurrentUser()).setScreenName(null);
                            Intent intent = new Intent(MainActivity.this, JoinActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                });
    }

    private void deleteQueue() {
        showExitDialog("Delete this party?", "You won't be able to undo this action!", "Delete", view -> {
            AdminPlayerFragment.disconnectService(MainActivity.this);
            mCurrentParty.deleteParty(e -> {
                ((User) ParseUser.getCurrentUser()).setScreenName(null);
                Intent intent = new Intent(MainActivity.this, JoinActivity.class);
                startActivity(intent);
                finish();
            });
        });
    }

    private void showExitDialog(String title, String message, String exitText, View.OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View layoutView = getLayoutInflater().inflate(R.layout.fragment_confirm_exit, null);
        builder.setView(layoutView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        ((TextView) layoutView.findViewById(R.id.tvTitle)).setText(title);
        ((TextView) layoutView.findViewById(R.id.tvMessage)).setText(message);
        Button btnExit = layoutView.findViewById(R.id.btnExit);
        btnExit.setText(exitText);
        btnExit.setOnClickListener(view -> {
            onClickListener.onClick(view);
            dialog.dismiss();
        });
        layoutView.findViewById(R.id.btnCancel).setOnClickListener(view -> dialog.dismiss());
        dialog.show();
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
            if (location != null) {
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
        if (mLocationCallback == null) return;
        mLocationManager.deregisterLocationUpdateCallback(mLocationCallback);
        mCurrentParty.clearLocation(e -> {
            if (e == null) {
                mLocationCallback = null;
            } else {
                registerLocationUpdater();
                Log.e(TAG, "Couldn't clear location!", e);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LocationManager.PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && mCurrentParty.getLocationEnabled()) {
                // Location permission has been granted, register location updater
                registerLocationUpdater();
            } else {
                Log.i(TAG, "Location permission was not granted.");
            }
        }
    }
}