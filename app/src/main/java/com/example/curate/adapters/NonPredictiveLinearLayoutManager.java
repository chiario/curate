package com.example.curate.adapters;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;

/**
 * A linear layout manager that always support animations even when using notifyDataSetChanged()
 */
public class NonPredictiveLinearLayoutManager extends LinearLayoutManager {
    public NonPredictiveLinearLayoutManager(Context context) {
        super(context);
    }

    /**
     * Always support predictive animations
     */
    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }
}
