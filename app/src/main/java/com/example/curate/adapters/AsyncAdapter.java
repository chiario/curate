package com.example.curate.adapters;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public abstract class AsyncAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected final List<T> mDataset = new ArrayList<>();
    private final ArrayDeque<List<T>> mPendingUpdates = new ArrayDeque<>();
    final Handler mHandler = new Handler(Looper.getMainLooper());

    @MainThread
    public boolean hasPendingUpdates() {
        return !mPendingUpdates.isEmpty();
    }

    @MainThread
    public List<T> peekLast() {
        return mPendingUpdates.isEmpty() ? mDataset : mPendingUpdates.peekLast();
    }

    @MainThread
    public void update(final List<T> items) {
        mPendingUpdates.add(items);
        if (mPendingUpdates.size() == 1)
            internalUpdate(items);
    }

    private void internalUpdate(final List<T> newList) {
        new Thread(() -> {

            final DiffUtil.DiffResult result = DiffUtil.calculateDiff(getDiffCallback(mDataset, newList), true);
            mHandler.post(() -> {
                mDataset.clear();
                mDataset.addAll(newList);
                result.dispatchUpdatesTo(this);
                processQueue();
            });
        }).start();
    }

    abstract DiffUtil.Callback getDiffCallback(List<T> oldList, List<T> newList);

    @MainThread
    private void processQueue() {
        mPendingUpdates.remove();
        if (!mPendingUpdates.isEmpty()) {
            if (mPendingUpdates.size() > 1) {
                List<T> lastList = mPendingUpdates.peekLast();
                mPendingUpdates.clear();
                mPendingUpdates.add(lastList);
            }
            internalUpdate(mPendingUpdates.peek());
        }
    }
}
