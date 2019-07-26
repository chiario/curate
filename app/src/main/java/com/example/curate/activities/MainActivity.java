package com.example.curate.activities;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
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
import com.example.curate.fragments.InfoDialogFragment;
import com.example.curate.fragments.QueueFragment;
import com.example.curate.fragments.SearchFragment;
import com.example.curate.fragments.SettingsDialogFragment;
import com.example.curate.models.Party;
import com.example.curate.models.User;
import com.example.curate.utils.ReverseInterpolator;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.parse.ParseUser;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements InfoDialogFragment.OnDeleteListener {

    private static final String KEY_QUEUE_FRAGMENT = "queue";
    private static final String KEY_SEARCH_FRAGMENT = "search";
    private static final String KEY_ACTIVE = "active";
    private static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "CurateChannel";

    // Timings subject to change
    private static final int NOTIFICATION_THRESHOLD = 10 * 60 * 1000; // 10 minutes in millis
    private static final int NOTIFICATION_CHECK_TIME = 60 * 1000; // 1 minute in millis
    private static final int NOTIFICATION_ID = 70; // Only ever show 1 notification

    @BindView(R.id.flPlaceholder) FrameLayout flPlaceholder;
    @BindView(R.id.ablMain) AppBarLayout ablMain;
    @BindView(R.id.tbMain) Toolbar tbMain;
    @BindView(R.id.flBottomPlayer) FrameLayout flBottomPlayer;
    @BindView(R.id.miSearch) SearchView miSearchView;
    @BindView(R.id.ivSearchBackground) ImageView ivSearchBackground;

    private FragmentManager mFragmentManager = getSupportFragmentManager();
    private Fragment mActiveFragment;
    private QueueFragment mQueueFragment;
    private SearchFragment mSearchFragment;
    private Fragment mBottomPlayerFragment;
    private ValueAnimator mSearchbarAnimator;
    private boolean mIsSearchbarExpanded = false;
    private boolean mIsAdmin = false;

    private long lastInteractionTime;
    private Handler notificationHandler;

    public BottomPlayerAdminFragment getBottomPlayerFragment() {
        if(!mIsAdmin) return null;
        return (BottomPlayerAdminFragment) mBottomPlayerFragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mIsAdmin = Party.getCurrentParty().isCurrentUserAdmin();

        initializeFragments(savedInstanceState);

        if (mIsAdmin) {
            mBottomPlayerFragment = BottomPlayerAdminFragment.newInstance();
        } else {
            mBottomPlayerFragment = BottomPlayerClientFragment.newInstance();
        }
        mFragmentManager.beginTransaction().replace(R.id.flBottomPlayer, mBottomPlayerFragment).commit();

        setSupportActionBar(tbMain);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if(!mIsAdmin) {
            lastInteractionTime = SystemClock.elapsedRealtime();
            notificationHandler = new Handler();
            notificationHandler.post(addSongsNotification);
        }

        initSearchBarAnimations();

        ((User) ParseUser.getCurrentUser()).registerPartyDeletedListener(mainActivity -> runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            builder
                    .setTitle("Party Deleted")
                    .setMessage("This party has been deleted by the admin.")
                    .setPositiveButton("Return to menu", (dialogInterface, i) -> {
                        Intent intent = new Intent(mainActivity, JoinActivity.class);
                        startActivity(intent);
                        Party.partyDeleted();
                    });
            builder.show();
        }), this);
    }

    private void initializeFragments(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            mQueueFragment = (QueueFragment) mFragmentManager.findFragmentByTag(KEY_QUEUE_FRAGMENT);
            mSearchFragment = (SearchFragment) mFragmentManager.findFragmentByTag(KEY_SEARCH_FRAGMENT);
            mActiveFragment = mFragmentManager.findFragmentByTag(savedInstanceState.getString(KEY_ACTIVE));
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

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem miInfo = menu.findItem(R.id.miInfo);
        MenuItem miSettings = menu.findItem(R.id.miSettings);

        if (miSearchView != null) {
            miSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            miSearchView.setIconifiedByDefault(false);
            miSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    search(miSearchView, query);
                    miSearchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });

            miSearchView.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
                if(hasFocus) {
                    display(mSearchFragment);
                    mSearchFragment.newSearch();
                    showKeyboard(view);
                    animateSearchbar(true);
                }
            });

        }

        miInfo.setOnMenuItemClickListener(menuItem -> {
            // Retrieve the current mParty's name and join code
            String name = Party.getCurrentParty().getName();
            String joinCode = Party.getCurrentParty().getJoinCode();

            InfoDialogFragment infoDialogFragment = InfoDialogFragment.newInstance(name, joinCode, mIsAdmin);
            infoDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_Rounded);
            infoDialogFragment.show(mFragmentManager, "fragment_party_info");
            return true;
        });
        miSettings.setOnMenuItemClickListener(menuItem -> {
            // Retrieve the current mParty's name and location preferences
            String name = Party.getCurrentParty().getName();
            boolean locationEnabled = Party.getLocationEnabled();
            SettingsDialogFragment settingsDialogFragment = SettingsDialogFragment.newInstance(name, locationEnabled);
            settingsDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.Dialog_Fullscreen);
            settingsDialogFragment.show(mFragmentManager, "fragment_admin_settings");
            return true;
        });

        miSettings.setVisible(mIsAdmin);
        miSettings.setEnabled(mIsAdmin);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIsAdmin) {
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
        outState.putString(KEY_ACTIVE, mActiveFragment.getTag());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        switch(mActiveFragment.getTag()) {
            case KEY_QUEUE_FRAGMENT:
                break;
            case KEY_SEARCH_FRAGMENT:
                mActiveFragment = mQueueFragment;
                miSearchView.setQuery("", false);
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
        if(fragment == null || fragment.equals(mActiveFragment)) return;
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        if(mActiveFragment != null)
            ft.hide(mActiveFragment);
        ft.show(fragment);
        if(fragment.equals(mSearchFragment))
            ft.addToBackStack(fragment.getTag());
        else if(fragment.equals(mQueueFragment)) {

        }
        ft.commit();

        mActiveFragment = fragment;
    }

    private void initSearchBarAnimations() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        float maxHeight = dpToPx(56f);
        float maxWidth = displayMetrics.widthPixels;
        float maxDelta = dpToPx(16f);
        float maxRadius = dpToPx(24f);

        GradientDrawable searchbarBackground = (GradientDrawable) ContextCompat.getDrawable(
                this, R.drawable.bg_searchbar);
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
        display(mSearchFragment);
        mSearchFragment.executeSearch(query);
        hideKeyboard(v);
    }


    @Override
    public void onLeaveQueue() {
        if (!mIsAdmin) {
            Party.leaveParty(e -> {
                removeNotifications();
                Intent intent = new Intent(MainActivity.this, JoinActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    public void onDeleteQueue() {
        if (mIsAdmin) {
            Party.deleteParty(e -> {
                Intent intent = new Intent(MainActivity.this, JoinActivity.class);
                startActivity(intent);
                finish();
            });
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
                .setContentTitle("Add a song to party!")
                .setContentText("It's been a while, don't miss out on the fun")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        updateInteractionTime();
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

    private void removeNotifications() {
        if(!mIsAdmin) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.deleteNotificationChannel(CHANNEL_ID);
            }
            notificationHandler.removeCallbacks(addSongsNotification);
        }
    }

    public void updateInteractionTime() {
        lastInteractionTime = SystemClock.elapsedRealtime();
    }
}