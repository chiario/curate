package com.example.curate.activities;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.example.curate.models.Party;
import com.google.android.material.appbar.AppBarLayout;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements InfoDialogFragment.SaveInfoListener {

    private static final String KEY_QUEUE_FRAGMENT = "queue";
    private static final String KEY_SEARCH_FRAGMENT = "search";
    private static final String KEY_ACTIVE = "active";
    private static final String TAG = "MainActivity";
    private static final int BARCODE_READER_REQUEST_CODE = 100;

    @BindView(R.id.flPlaceholder) FrameLayout flPlaceholder;
    @BindView(R.id.ablMain) AppBarLayout ablMain;
    @BindView(R.id.tbMain) Toolbar tbMain;
    @BindView(R.id.flBottomPlayer) FrameLayout flBottomPlayer;

    private MenuItem miSearch;
    private MenuItem miText;
    private SearchView mSearchView;
    private FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment;
    private QueueFragment queueFragment;
    private SearchFragment searchFragment;
    private Fragment mBottomPlayerFragment;
    private Party party;

    private boolean isAdmin = false;

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
            fm.beginTransaction().replace(R.id.flBottomPlayer, mBottomPlayerFragment).commit();
        } else {
            mBottomPlayerFragment = BottomPlayerClientFragment.newInstance();
            fm.beginTransaction().replace(R.id.flBottomPlayer, mBottomPlayerFragment).commit();
        }

        setSupportActionBar(tbMain);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
    }

    @Override
    public void onSaveInfo(String newName, boolean locationEnabled) {
        Party.setPartyName(newName, e -> {
            if (e == null) {
                miText.setTitle(party.getName());
            } else {
                Log.e(TAG, "Couldn't save party name!", e);
            }
        });
        Party.setLocationEnabled(locationEnabled, e -> {
            if (e != null) {
                Log.e(TAG, "Couldn't change location preferences!", e);
            }
        });
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
        miSearch = menu.findItem(R.id.miSearch);
        MenuItem miInfo = menu.findItem(R.id.miInfo);
        miText = menu.findItem(R.id.miText);
        MenuItem miLeave = menu.findItem(R.id.miLeave);

        miText.setTitle(party.getName());

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
        miLeave.setOnMenuItemClickListener(menuItem -> {
           onLeaveQueue();
           return true;
        });

        miInfo.setVisible(isAdmin);
        miInfo.setEnabled(isAdmin);
        miLeave.setVisible(!isAdmin);
        miLeave.setVisible(!isAdmin);
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
//                etSearch.clearFocus();
                break;
        }
//        etSearch.setText("");
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
                .setNegativeButton("Cancel", (dialogInterface, i) -> {
                });
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
}