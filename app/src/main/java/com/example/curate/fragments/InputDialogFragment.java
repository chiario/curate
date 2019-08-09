package com.example.curate.fragments;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.curate.R;

public class InputDialogFragment extends DialogFragment {

	private SubmitListener mSubmit;
	private String mHint;
	private String mTitle;
	private boolean mIsSubmitted = false;

	public InputDialogFragment() {
		// Required empty public constructor
	}

	public interface SubmitListener {
		void submit(String input);
	}

	public static InputDialogFragment newInstance(SubmitListener submitCallback, String hint, String title) {
		InputDialogFragment inputDialogFragment = new InputDialogFragment();
		inputDialogFragment.mTitle = title;
		inputDialogFragment.mHint = hint;
		inputDialogFragment.mSubmit = submitCallback;
		return inputDialogFragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_input, container);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);

		EditText etInput = getView().findViewById(R.id.etInput);
		etInput.setHint(mHint);
		TextView tvTitle = getView().findViewById(R.id.tvTitle);
		tvTitle.setText(mTitle);
		ImageButton ibSubmit = getView().findViewById(R.id.ibSubmit);
		ibSubmit.setOnClickListener(view1 -> {
			mSubmit.submit(etInput.getText().toString());
			mIsSubmitted = true;
			dismiss();
		});

		etInput.setOnEditorActionListener((textView, i, keyEvent) -> {
			mSubmit.submit(etInput.getText().toString());
			mIsSubmitted = true;
			dismiss();
			return true;
		});

		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		if(!mIsSubmitted) mSubmit.submit("");
	}

	@Override
	public void onStart() {
		super.onStart();
		Dialog dialog = getDialog();
		if(dialog != null) {
			DisplayMetrics dm = new DisplayMetrics();
			dialog.getWindow().getWindowManager().getDefaultDisplay().getMetrics(dm);
			int width = dm.widthPixels - 2 * (int) getResources().getDimension(R.dimen.dialog_input_margin);
			int height = ViewGroup.LayoutParams.WRAP_CONTENT;
			dialog.getWindow().setLayout(width, height);
		}
	}
}
