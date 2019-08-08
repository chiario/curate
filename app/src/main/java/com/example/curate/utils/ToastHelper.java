package com.example.curate.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.curate.R;

public class ToastHelper {
	public interface Toaster {
		void makeText(Context context, String text);
	}

	public static boolean makeText(Context context, String text) {
		return makeText(context, text, getToaster());
	}

	public static boolean makeText(Context context, String text, Toaster toaster) {
		toaster.makeText(context, text);
		return true;
	}

	private static Toaster getToaster() {
		return (context, text) -> {
			View layout = LayoutInflater.from(context).inflate(R.layout.toast_main, null);
			((TextView) layout.findViewById(R.id.tvMessage)).setText(text);
			Toast toast = new Toast(context);
			toast.setDuration(Toast.LENGTH_SHORT);
			toast.setView(layout);
			toast.show();
		};
	}
}
