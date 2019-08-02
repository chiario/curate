package com.example.curate.utils;

import android.os.Handler;
import android.widget.SeekBar;

import com.example.curate.fragments.AdminPlayerFragment;

public class TrackProgressBar {
    private static final int NEXT_SONG_DELAY = 2000;
    private static final int LOOP_DURATION = 500;
    private SeekBar mSeekBar;
    private Handler mHandler;
    private AdminPlayerFragment mFragment;

    public TrackProgressBar(AdminPlayerFragment fragment, SeekBar seekBar) {
        mSeekBar = seekBar;
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mHandler = new Handler();
        mFragment = fragment;
    }

    private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener
            = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            long timeRemaining = mSeekBar.getMax() - seekBar.getProgress();
            // Ensure the seekbar is never tracked into the last NEXT_SONG_DELAY millis of play
            if (timeRemaining <= NEXT_SONG_DELAY) {
                mFragment.onSkipNext();
            } else {
                long progress = seekBar.getProgress();
                mFragment.onSeekTo(progress);
                update(progress);
            }
        }
    };

    private final Runnable mSeekRunnable = new Runnable() {
        @Override
        public void run() {
            int progress = mSeekBar.getProgress();
            mSeekBar.setProgress(progress + LOOP_DURATION);
            mHandler.postDelayed(mSeekRunnable, LOOP_DURATION);
        }
    };

    public void setDuration(long duration) {
        mSeekBar.setMax((int) duration);
    }

    public void update(long progress) {
        mSeekBar.setProgress((int) progress);
        mHandler.removeCallbacks(mSeekRunnable);
        mHandler.postDelayed(mSeekRunnable, LOOP_DURATION);
    }

    public void pause() {
        mHandler.removeCallbacks(mSeekRunnable);
    }

    public void unpause() {
        mHandler.removeCallbacks(mSeekRunnable);
        mHandler.postDelayed(mSeekRunnable, LOOP_DURATION);
    }

    public void setEnabled(boolean isEnabled) {
        mSeekBar.setEnabled(isEnabled);
    }
}
