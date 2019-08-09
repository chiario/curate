package com.example.curate.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.example.curate.R;
import com.github.mmin18.widget.RealtimeBlurView;

public class BlurDialogFragment extends Fragment {
    View mRoot;
    RealtimeBlurView mBlurView;

    public View onShow(View root) {
        mRoot = root;
        mRoot.setAlpha(0f);

        mBlurView = mRoot.findViewById(R.id.blurLayout);
        mBlurView.setVisibility(View.GONE);

        float blurRadius = getResources().getDimension(R.dimen.blur_radius);
        ValueAnimator blurAnimator = ValueAnimator.ofFloat(0f, blurRadius);
        blurAnimator.setDuration(250);
        blurAnimator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            mBlurView.setBlurRadius(animatedValue);
        });

        mRoot.animate().alpha(1f).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mBlurView.setVisibility(View.VISIBLE);
                blurAnimator.start();
            }
        });


        return mRoot;
    }

    public void onHide(final Runnable onComplete) {
        mRoot.animate().alpha(0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onComplete.run();
            }
        });
    }
}
