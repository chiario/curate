package com.example.curate.fragments;


import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

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
			dismiss();
			mSubmit.submit(etInput.getText().toString());
		});

		etInput.setOnEditorActionListener((textView, i, keyEvent) -> {
			dismiss();
			mSubmit.submit(etInput.getText().toString());
			return true;
		});

		super.onViewCreated(view, savedInstanceState);
	}
}
