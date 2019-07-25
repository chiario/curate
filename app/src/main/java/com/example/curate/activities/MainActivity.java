package com.example.curate.activities;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.curate.fragments.BottomPlayerAdminFragment;
import com.example.curate.fragments.BottomPlayerClientFragment;
import com.example.curate.fragments.InfoDialogAdminFragment;
import com.example.curate.fragments.InfoDialogClientFragment;
import com.example.curate.fragments.QueueFragment;
import com.example.curate.fragments.SearchFragment;
import com.example.curate.fragments.SettingsDialogFragment;
import com.example.curate.models.Party;
import com.example.curate.utils.ReverseInterpolator;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements InfoDialogAdminFragment.OnDeleteListener, InfoDialogClientFragment.OnLeaveListener, SettingsDialogFragment.OnSaveListener {

    private static final String KEY_QUEUE_FRAGMENT = "queue";
    private static final String KEY_SEARCH_FRAGMENT = "search";
    private static final String KEY_ACTIVE = "active";
    private static final String TAG = "MainActivity";
    private static final int BARCODE_READER_REQUEST_CODE = 100;
    private static final String LEAVE_TAG = "LeaveQueue";
    private static final String DELETE_TAG = "DeleteQueue";
    private static final String SAVE_TAG = "SaveInfo";
    private static final String CHANNEL_ID = "CurateChannel";

    // Timings subject to change
    private static final int NOTIFICATION_THRESHOLD = 10 * 60 * 1000;
    private static final int NOTIFICATION_CHECK_TIME = 60 * 1000;
    private final int NOTIFICATION_ID = Integer.parseInt(new SimpleDateFormat("ddHHmmss",  Locale.US).format(new Date()));

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

    private long lastInteractionTime;

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

        lastInteractionTime = SystemClock.elapsedRealtime();
        new Handler().post(addSongsNotification);

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
            String name = Party.getName();
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
        lastInteractionTime = SystemClock.elapsedRealtime();
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


    @Override
    public void onLeaveQueue() {
        if (!isAdmin) {
            Party.leaveParty(e -> {
                Intent intent = new Intent(MainActivity.this, JoinActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    public void onDeleteQueue() {
        if (isAdmin) {
            Party.deleteParty(e -> {
                Intent intent = new Intent(MainActivity.this, JoinActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    public void onSaveInfo(@Nullable String newName, @Nullable Boolean locationEnabled) {
        if (isAdmin) {
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
    }

    Runnable addSongsNotification = new Runnable() {
        @Override
        public void run() {
            if(SystemClock.elapsedRealtime() - lastInteractionTime > NOTIFICATION_THRESHOLD) createNotification();

            // Check interactions in NOTIFICATION_CHECK_TIME millis
            new Handler().postDelayed(addSongsNotification, NOTIFICATION_CHECK_TIME);
        }
    };

    private void createNotification() {
        createNotificationChannel();
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(getResources().getColor(R.color.colorAccent))
                .setContentTitle("Add a song to your current party!")
                .setContentText("It's been a while, don't miss out on the fun")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // Return if notification channel is already created
            if(notificationManager.getNotificationChannel(CHANNEL_ID) != null) return;

            String name = "Curate Channel";
            String description = "Notifies user to add song";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);
        }
    }
}