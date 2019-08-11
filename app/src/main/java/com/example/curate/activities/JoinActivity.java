package com.example.curate.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.example.curate.fragments.BlurDialogFragment;
import com.example.curate.fragments.ExitDialogFragment;
import com.example.curate.fragments.JoinFragment;
import com.example.curate.fragments.PartyDeletedDialogFragment;
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
    BlurDialogFragment mDialogFragment;
    private boolean mIsHiding = false;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        svContainer.setEnabled(false);
        View content = getWindow().findViewById(Window.ID_ANDROID_CONTENT);
        int height = content.getHeight();
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) flContainer.getLayoutParams();
        params.height = height;
        flContainer.setLayoutParams(params);
        svContainer.invalidate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        ButterKnife.bind(this);

        initDialogOverlay();

        mFragmentManager = getSupportFragmentManager();

        boolean partyDeleted = getIntent().getBooleanExtra(MainActivity.KEY_PARTY_DELETED, false);
        if(partyDeleted) {
            PartyDeletedDialogFragment deletedDialog = PartyDeletedDialogFragment.newInstance(v -> hideDialog());
            showDialog(deletedDialog);
        }

        mSelectFragment = SelectFragment.newInstance();
        mJoinFragment = JoinFragment.newInstance();

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
        if(mDialogFragment != null) {
            return;
        }
        mDialogFragment = dialog;
        flOverlay.setVisibility(View.VISIBLE);
        flOverlay.setAlpha(0f);
        flOverlay.animate().alpha(1f).setDuration(500).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {

            }
        });
        mFragmentManager.beginTransaction().replace(R.id.flOverlay, mDialogFragment, null).commit();
    }

    @Override
    public void onBackPressed() {
        if(mDialogFragment != null) {
            hideDialog();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void hideDialog() {
        if(mDialogFragment == null || mIsHiding) {
            return;
        }
        mIsHiding = true;

        flOverlay.setAlpha(1f);
        flOverlay.animate().alpha(0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                flOverlay.setVisibility(View.GONE);
            }
        });
        mDialogFragment.onHide(() -> {
            mDialogFragment = null;
            mIsHiding = false;
        });
    }
}
