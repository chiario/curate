package com.example.curate.utils;

import android.view.animation.Interpolator;

public class ReverseInterpolator implements Interpolator {
    private Interpolator mInterpolator;

    public ReverseInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    @Override
    public float getInterpolation(float paramFloat) {
        return Math.abs(mInterpolator.getInterpolation(paramFloat) - 1f);
    }
}
