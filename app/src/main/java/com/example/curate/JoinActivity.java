package com.example.curate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.curate.models.Party;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;

public class JoinActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        ensureCurrentUserExists();
        tryJoinExistingParty();
    }

    private void ensureCurrentUserExists() {
        if(ParseUser.getCurrentUser() == null) {
            ParseAnonymousUtils.logIn((user, e) -> {
                if (e != null) {
                    // TODO: disable the buttons or something?
                    Log.e("JoinActivity", "Anonymous login failed!", e);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void tryJoinExistingParty() {
        Party.getExistingParty(e -> {
            if(e == null && Party.getCurrentParty() != null) {
                switchToMainActivity();
            }
        });
    }

    private void switchToMainActivity() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    public void onCreateParty(View view) {
        Party.createParty(e -> {
            if(e == null) {
                switchToMainActivity();
            } else {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
