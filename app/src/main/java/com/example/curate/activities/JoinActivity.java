package com.example.curate.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.example.curate.JoinFragment;
import com.example.curate.SelectFrament;
import com.example.curate.R;

public class JoinActivity extends AppCompatActivity implements SelectFrament.OnOptionSelected {
    SelectFrament mSelectFragment;
    JoinFragment mJoinFragment;
    FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        mSelectFragment = SelectFrament.newInstance();
        mJoinFragment = JoinFragment.newInstance();
        mFragmentManager = getSupportFragmentManager();

        displaySelectFragment();
    }

    public void displaySelectFragment() {
        mFragmentManager.beginTransaction()
                .replace(R.id.fl_container, mSelectFragment)
                .commit();
    }

    public void displayJoinFragment() {
        mFragmentManager.beginTransaction()
                .replace(R.id.fl_container, mJoinFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onPartyObtained() {
        switchToMainActivity();
    }

    @Override
    public void onJoinPartySelected() {
        displayJoinFragment();
    }

    private void switchToMainActivity() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }
}
