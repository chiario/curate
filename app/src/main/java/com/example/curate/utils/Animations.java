package com.example.curate.utils;

import android.view.View;

public class Animations {
	public static class PressRunnable implements Runnable {
		View mView;
		float mX;
		float mY;

		public PressRunnable(View v, float x, float y) {
			mView = v;
			mX = x;
			mY = y;
		}

		public void run() {
			mView.getBackground().setHotspot(mX, mY);
			mView.setPressed(true);
			mView.postOnAnimationDelayed(new UnpressRunnable(mView), 100);
		}
	}

	public static class UnpressRunnable implements Runnable {
		View mView;
		public UnpressRunnable(View v) {
			mView = v;
		}

		public void run() {
			mView.setPressed(false);
		}

	}
}
