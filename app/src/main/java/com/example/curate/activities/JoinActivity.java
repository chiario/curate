package com.example.curate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.example.curate.fragments.JoinFragment;
import com.example.curate.fragments.SelectFragment;
import com.example.curate.R;
import com.example.curate.models.User;
import com.parse.ParseUser;

public class JoinActivity extends AppCompatActivity implements SelectFragment.OnOptionSelected {
    SelectFragment mSelectFragment;
    JoinFragment mJoinFragment;
    FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        mSelectFragment = SelectFragment.newInstance();
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
        User user = (User) ParseUser.getCurrentUser();
        if(User.getCurrentScreenName() == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            EditText etScreenName = new EditText(this);
            builder.setTitle("Set your username")
                    .setView(etScreenName)
                    .setPositiveButton("Rock out!", (dialogInterface, i) -> {
                        user.setScreenName(etScreenName.getText().toString());
                        switchToMainActivity();
                    });
            builder.setCancelable(false);
            builder.show();
        } else {
            switchToMainActivity();
        }
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
