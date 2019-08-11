package com.example.curate.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.example.curate.fragments.BlurDialogFragment;
import com.example.curate.fragments.JoinFragment;
import com.example.curate.fragments.SelectFragment;
import com.example.curate.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class JoinActivity extends AppCompatActivity implements SelectFragment.OnOptionSelected {
    @BindView(R.id.flOverlay) FrameLayout flOverlay;
    @BindView(R.id.fl_container) FrameLayout flContainer;
    @BindView(R.id.scroll) ScrollView svContainer;

    SelectFragment mSelectFragment;
    JoinFragment mJoinFragment;
    FragmentManager mFragmentManager;
    BlurDialogFragment mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        ButterKnife.bind(this);

        svContainer.setEnabled(false);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) flContainer.getLayoutParams();
        params.height = height;
        flContainer.setLayoutParams(params);
        svContainer.invalidate();


        initDialogOverlay();

        boolean partyDeleted = getIntent().getBooleanExtra(MainActivity.KEY_PARTY_DELETED, false);
        if(partyDeleted) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            AlertDialog dialog = builder.setView(R.layout.fragment_confirm_exit).setCancelable(false).show();
            Button button = dialog.findViewById(R.id.btnExit);
            button.setOnClickListener(view -> {
                dialog.dismiss();
            });
            button.setText("Got it!");
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

    private void initDialogOverlay() {
        flOverlay.setOnClickListener((view) -> {
            hideDialog();
        });
        flOverlay.setAlpha(0f);
        flOverlay.setVisibility(View.GONE);
    }

    @Override
    public void showDialog(BlurDialogFragment dialog) {
        if(mCurrentFragment != null) {
            return;
        }
        mCurrentFragment = dialog;
        flOverlay.setVisibility(View.VISIBLE);
        flOverlay.setAlpha(0f);
        flOverlay.animate().alpha(1f).setDuration(500).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {

            }
        });
        mFragmentManager.beginTransaction().replace(R.id.flOverlay, mCurrentFragment, null).commit();
    }

    @Override
    public void onBackPressed() {
        if(mCurrentFragment != null) {
            hideDialog();
        } else {
            super.onBackPressed();
        }
    }

    public void hideDialog() {
        if(mCurrentFragment == null) {
            return;
        }

        flOverlay.setAlpha(1f);
        flOverlay.animate().alpha(0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                flOverlay.setVisibility(View.GONE);
            }
        });
        mCurrentFragment.onHide(() -> mCurrentFragment = null);
    }
}
