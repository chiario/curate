package com.example.curate.utils;

import android.content.Context;

public class ToastHelper {
	static Toaster mToaster;
	public interface Toaster {
		void makeText(Context context, String text);
	}

	public static void setToaster(Toaster toaster) {
		mToaster =  toaster;
	}

	public static boolean makeText(Context context, String text) {
		if(mToaster == null) return false;
		mToaster.makeText(context, text);
		return true;
	}
}
