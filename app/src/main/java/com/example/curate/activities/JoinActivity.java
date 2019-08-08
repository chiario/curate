package com.example.curate.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.example.curate.fragments.JoinFragment;
import com.example.curate.fragments.SelectFragment;
import com.example.curate.R;
import com.example.curate.models.User;
import com.example.curate.utils.ToastHelper;
import com.parse.ParseUser;

public class JoinActivity extends AppCompatActivity implements SelectFragment.OnOptionSelected {
    SelectFragment mSelectFragment;
    JoinFragment mJoinFragment;
    FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        boolean partyDeleted = getIntent().getBooleanExtra(MainActivity.KEY_PARTY_DELETED, false);
        if(partyDeleted) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            AlertDialog dialog = builder.setView(R.layout.fragment_confirm_exit).setCancelable(false).show();
            Button button = dialog.findViewById(R.id.btnExit);
            button.setOnClickListener(view -> {
                dialog.dismiss();
            });
            button.setText("Return to menu");
            dialog.findViewById(R.id.btnCancel).setVisibility(View.GONE);
            ((TextView) dialog.findViewById(R.id.tvTitle)).setText("Party Deleted");
            ((TextView) dialog.findViewById(R.id.tvMessage)).setText("This party has been deleted by the admin.");
        }

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
