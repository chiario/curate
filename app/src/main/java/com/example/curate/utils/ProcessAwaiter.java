package com.example.curate.utils;

public class ProcessAwaiter {
    public static interface OnCompleteCallback {
        public void done();
    }

    private final Object mMutex = new Object();
    private final OnCompleteCallback mCallback;
    private int mLatch;

    public ProcessAwaiter(OnCompleteCallback callback) {
        mCallback = callback;
    }

    private void incrementLatch() {
        synchronized (mMutex) {
            mLatch++;
        }
    }

    private void decrementLatch() {
        synchronized (mMutex) {
            mLatch--;
        }
    }

    public void notifyProcessStarted() {
        incrementLatch();
    }

    public void notifyProcessCompleted() {
        synchronized (mMutex) {
            decrementLatch();
            if(mLatch == 0) {
                mCallback.done();
            }
        }
    }

}
