package com.example.curate.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.curate.R;

import org.w3c.dom.Text;

import butterknife.BindView;
import butterknife.ButterKnife;

public class InputDialogFragment extends BlurDialogFragment {

	@BindView(R.id.etInput) EditText etInput;
	@BindView(R.id.tvTitle) TextView tvTitle;
	@BindView(R.id.ibSubmit) ImageButton ibSubmit;
	private SubmitListener mSubmit;
	private String mHint;
	private String mTitle;
	private boolean mIsNumberInput;
	private boolean mSubmitted = false;

	public InputDialogFragment() {
		// Required empty public constructor
	}

	public interface SubmitListener {
		void submit(String input);
	}

	@Override
	public void onHide(Runnable onComplete) {
		super.onHide(onComplete);
		if(!mSubmitted) mSubmit.submit(etInput.getText().toString());
	}

	public static InputDialogFragment newInstance(SubmitListener submitCallback, String hint, String title, boolean isNumberInput) {
		InputDialogFragment inputDialogFragment = new InputDialogFragment();
		inputDialogFragment.mTitle = title;
		inputDialogFragment.mHint = hint;
		inputDialogFragment.mSubmit = submitCallback;
		inputDialogFragment.mIsNumberInput = isNumberInput;
		return inputDialogFragment;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_input_dialog, container, false);
		super.onShow(view);
		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		ButterKnife.bind(this, view);
		etInput.setHint(mHint);
		if(mIsNumberInput) etInput.setInputType(InputType.TYPE_CLASS_NUMBER);
		else etInput.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

		tvTitle.setText(mTitle);

		ibSubmit.setOnClickListener(view1 -> {
			mSubmit.submit(etInput.getText().toString());
			mSubmitted = true;
		});

		etInput.setOnEditorActionListener((textView, i, keyEvent) -> {
			mSubmit.submit(etInput.getText().toString());
			mSubmitted = true;
			return false;
		});

		super.onViewCreated(view, savedInstanceState);
	}
}
