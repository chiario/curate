package com.example.curate.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

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

        mRoot.setOnClickListener(view -> {});


        return mRoot;
    }

    public void onHide(final Runnable onComplete) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        if(mRoot != null) {
            mRoot.animate().alpha(0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    onComplete.run();
                }
            });
        } else {
            onComplete.run();
        }
    }
}
