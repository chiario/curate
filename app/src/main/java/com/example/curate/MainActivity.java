package com.example.curate;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_QUEUE_FRAGMENT = "queue";
    private static final String KEY_SEARCH_FRAGMENT = "search";

    private FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment;

    QueueFragment queueFragment;
    SearchFragment searchFragment;

    @BindView(R.id.etSearch) EditText etSearch;
    @BindView(R.id.ibSearch) ImageButton ibSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        if(savedInstanceState != null) {
            queueFragment = (QueueFragment) fm.findFragmentByTag(KEY_QUEUE_FRAGMENT);
            searchFragment = (SearchFragment) fm.findFragmentByTag(KEY_SEARCH_FRAGMENT);
        }

        if(queueFragment == null) {
            queueFragment = QueueFragment.newInstance();
            fm.beginTransaction().add(R.id.flPlaceholder, queueFragment, KEY_QUEUE_FRAGMENT).hide(queueFragment).commit();
        }
        if(searchFragment == null) {
            searchFragment = SearchFragment.newInstance();
            fm.beginTransaction().add(R.id.flPlaceholder, searchFragment, KEY_SEARCH_FRAGMENT).hide(searchFragment).commit();
        }

        display(queueFragment);

        ibSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                display(searchFragment);
                searchFragment.setSearchText(etSearch.getText().toString());
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        etSearch.setText("");
    }

    private void display(Fragment fragment) {
        FragmentTransaction ft = fm.beginTransaction();
        if(activeFragment != null)
            ft.hide(activeFragment);
        ft.show(fragment);
        if(fragment.equals(searchFragment))
            ft.addToBackStack(fragment.getTag());
        ft.commit();

        activeFragment = fragment;
    }
}
