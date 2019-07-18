package com.example.curate.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.curate.R;
import com.example.curate.models.Party;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseUser;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class JoinActivity extends AppCompatActivity {
    @BindView(R.id.buttonContainer) ConstraintLayout mButtonContainer;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        ButterKnife.bind(this);

        ensureCurrentUserExists();
    }

    private void ensureCurrentUserExists() {
        if(ParseUser.getCurrentUser() == null) {
            ParseAnonymousUtils.logIn((user, e) -> {
                if (e != null) {
                    // TODO: disable the buttons or something?
                    Log.e("JoinActivity", "Anonymous login failed!", e);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    tryJoinExistingParty();
                }
            });
        } else {
            tryJoinExistingParty();
        }
    }

    private void tryJoinExistingParty() {
        Party.getExistingParty(e -> {
            if(e == null && Party.getCurrentParty() != null) {
                switchToMainActivity();
            } else {
                mProgressBar.setVisibility(View.GONE);
                mButtonContainer.setVisibility(View.VISIBLE);
            }
        });
    }
    //TODO - Fix bug: user can be part of a party that has been deleted

    private void switchToMainActivity() {
        Intent i = new Intent(this, MainActivity.class);


        boolean isAdmin = ParseUser.getCurrentUser() == Party.getCurrentParty().get("admin");
        i.putExtra("isAdmin", isAdmin);
        startActivity(i);
        finish();
    }

    @OnClick(R.id.btnCreateParty)
    public void onCreateParty(View view) {
        Party.createParty(e -> {
            if(e == null) {
                switchToMainActivity();
            } else {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @OnClick(R.id.btnJoinParty)
    public void onJoinParty(View view) {
        Party.joinParty("oFyv4EUtAz", e -> {
            if (e == null) {
                switchToMainActivity();
            } else {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
