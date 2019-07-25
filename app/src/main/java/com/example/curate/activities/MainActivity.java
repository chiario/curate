package com.example.curate.activities;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.curate.R;
import com.example.curate.fragments.BottomPlayerAdminFragment;
import com.example.curate.fragments.BottomPlayerClientFragment;
import com.example.curate.fragments.InfoDialogAdminFragment;
import com.example.curate.fragments.InfoDialogClientFragment;
import com.example.curate.fragments.OnFragmentInteractionListener;
import com.example.curate.fragments.QueueFragment;
import com.example.curate.fragments.SearchFragment;
import com.example.curate.fragments.SettingsDialogFragment;
import com.example.curate.models.Party;
import com.example.curate.utils.ReverseInterpolator;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements OnFragmentInteractionListener {

    private static final String KEY_QUEUE_FRAGMENT = "queue";
    private static final String KEY_SEARCH_FRAGMENT = "search";
    private static final String KEY_ACTIVE = "active";
    private static final String TAG = "MainActivity";
    private static final int BARCODE_READER_REQUEST_CODE = 100;
    private static final String LEAVE_TAG = "LeaveQueue";
    private static final String DELETE_TAG = "DeleteQueue";
    private static final String SAVE_TAG = "SaveInfo";

    @BindView(R.id.flPlaceholder) FrameLayout flPlaceholder;
    @BindView(R.id.ablMain) AppBarLayout ablMain;
    @BindView(R.id.tbMain) Toolbar tbMain;
    @BindView(R.id.flBottomPlayer) FrameLayout flBottomPlayer;
    @BindView(R.id.miSearch) SearchView mSearchView;
    @BindView(R.id.ivSearchBackground) ImageView ivSearchBackground;

    private FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment;
    private QueueFragment queueFragment;
    private SearchFragment searchFragment;
    private Fragment mBottomPlayerFragment;
    private Party party;
    private ValueAnimator mSearchbarAnimator;
    private boolean mIsSearchbarExpanded = false;
    private boolean isAdmin = false;

    private BottomSheetBehavior bottomSheetBehavior;

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

        if (isAdmin) {
            mBottomPlayerFragment = BottomPlayerAdminFragment.newInstance();

        } else {
            mBottomPlayerFragment = BottomPlayerClientFragment.newInstance();
        }
        fm.beginTransaction().replace(R.id.flBottomPlayer, mBottomPlayerFragment).commit();

        setSupportActionBar(tbMain);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        initSearchBarAnimations();
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
        mBottomPlayerFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem miInfo = menu.findItem(R.id.miInfo);
        MenuItem miSettings = menu.findItem(R.id.miSettings);

        if (mSearchView != null) {
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            mSearchView.setIconifiedByDefault(false);
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    search(mSearchView, query);
                    mSearchView.clearFocus();
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
                        showKeyboard(view);
                        animateSearchbar(true);
                        //TODO show keyboard?
                    }
                }
            });

        }

        miInfo.setOnMenuItemClickListener(menuItem -> {
            // Retrieve the current party's name and join code
            String name = party.getName();
            String joinCode = party.getJoinCode();
            if (isAdmin) {
                // Open a new instance of the InfoDialogAdminFragment, passing in the current party's name and code
                InfoDialogAdminFragment infoDialogAdminFragment = InfoDialogAdminFragment.newInstance(name, joinCode);
                infoDialogAdminFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_Rounded);
                infoDialogAdminFragment.show(fm, "fragment_party_info");
            } else {
                // Open a new instance of the InfoDialogClientFragment, passing in the current party's name and code
                InfoDialogClientFragment infoDialogClientFragment = InfoDialogClientFragment.newInstance(name, joinCode);
                infoDialogClientFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_Rounded);
                infoDialogClientFragment.show(fm, "fragment_party_info");
            }
            return true;
        });
        miSettings.setOnMenuItemClickListener(menuItem -> {
            // Retrieve the current party's name and location preferences
            String name = party.getName();
            boolean locationEnabled = Party.getLocationEnabled();
            SettingsDialogFragment settingsDialogFragment = SettingsDialogFragment.newInstance(name, locationEnabled);
            settingsDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_Fullscreen);
            settingsDialogFragment.show(fm, "fragment_admin_settings");
            return true;
        });

        miSettings.setVisible(isAdmin);
        miSettings.setEnabled(isAdmin);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAdmin) {
//            mAdminManager.checkSpotifyInstalled();
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
                mSearchView.setQuery("", false);
                animateSearchbar(false);
                break;
        }
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

        }
        ft.commit();

        activeFragment = fragment;
    }

    private void initSearchBarAnimations() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        float maxHeight = dpToPx(56f);
        float maxWidth = displayMetrics.widthPixels;
        float maxDelta = dpToPx(16f);
        float maxRadius = dpToPx(24f);

        GradientDrawable searchbarBackground = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.bg_searchbar);
        searchbarBackground.setCornerRadius(maxRadius);
        ViewGroup.LayoutParams params = ivSearchBackground.getLayoutParams();
        params.height = (int) (maxHeight - maxDelta / 1.5f);
        params.width = (int) (maxWidth - maxDelta);
        ivSearchBackground.setLayoutParams(params);
        ivSearchBackground.setBackground(searchbarBackground);

        mSearchbarAnimator = ValueAnimator.ofFloat(1f, 0f);
        mSearchbarAnimator.setDuration(200);
        mSearchbarAnimator.addUpdateListener(anim -> {
            float currentDelta = maxDelta * (Float) anim.getAnimatedValue();
            float currentRadius = maxRadius * (Float) anim.getAnimatedValue();
            ViewGroup.LayoutParams layout = ivSearchBackground.getLayoutParams();
            layout.height = (int) (maxHeight - currentDelta / 1.5f);
            layout.width = (int) (maxWidth - currentDelta);
            ivSearchBackground.setLayoutParams(layout);
            searchbarBackground.setCornerRadius(currentRadius);
        });
    }

    private float dpToPx(float dip) {
        Resources r = getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }

    private void animateSearchbar(boolean isExpanding) {
        if(mIsSearchbarExpanded == isExpanding)
            return;
        mIsSearchbarExpanded = isExpanding;
        mSearchbarAnimator.setInterpolator(isExpanding
                ? new AccelerateDecelerateInterpolator()
                : new ReverseInterpolator(new AccelerateDecelerateInterpolator()));
        mSearchbarAnimator.start();
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


    private void onLeaveQueue() {
        Party.leaveParty(e -> {
            Intent intent = new Intent(MainActivity.this, JoinActivity.class);
            startActivity(intent);
            finish();
        });

    }

    private void onDeleteQueue() {
        Party.deleteParty(e -> {
            Intent intent = new Intent(MainActivity.this, JoinActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void onSaveInfo(@Nullable String newName, @Nullable Boolean locationEnabled) {
        Log.d(TAG, "Saving changes to party");
        if (newName != null) {
            Party.setPartyName(newName, e -> {
                if (e != null) {
                    Log.e(TAG, "Could not save party name!", e);
                }
            });
        }
        if (locationEnabled != null) {
            Party.setLocationEnabled(locationEnabled, e -> {
                if (e != null) {
                    Log.e(TAG, "Could not save location preferences!", e);
                } else {
                    if (locationEnabled) {
                        ((BottomPlayerAdminFragment) mBottomPlayerFragment).registerLocationUpdater();
                    } else {
                        ((BottomPlayerAdminFragment) mBottomPlayerFragment).deregisterLocationUpdater();
                    }
                }
            });
        }
    }

    @Override
    public void onFragmentMessage(String TAG, String newName, Boolean locationEnabled) {
        Log.d(TAG, "Received fragment message with tag" + TAG);
        switch (TAG) {
            case DELETE_TAG:
                if (isAdmin) {
                    onDeleteQueue();
                }
                break;
            case LEAVE_TAG:
                if (!isAdmin) {
                    onLeaveQueue();
                }
                break;
            case SAVE_TAG:
                if (isAdmin) {
                    onSaveInfo(newName, locationEnabled);
                }
                break;
        }
    }
}