package com.example.curate.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.curate.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ExitDialogFragment extends BlurDialogFragment {

    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvMessage) TextView tvMessage;
    @BindView(R.id.btnExit) Button btnExit;
    @BindView(R.id.btnCancel) Button btnCancel;

    String mTitle;
    String mMessage;
    String mExitText;
    View.OnClickListener mOnSubmit;
    View.OnClickListener mOnCancel;
    public ExitDialogFragment() {
        // Required empty public constructor
    }

    public static ExitDialogFragment newInstance(String title, String message, String exitText, View.OnClickListener onSumbit, View.OnClickListener onCancel) {
        ExitDialogFragment exitDialogFragment = new ExitDialogFragment();
        exitDialogFragment.mTitle = title;
        exitDialogFragment.mMessage = message;
        exitDialogFragment.mExitText = exitText;
        exitDialogFragment.mOnSubmit = onSumbit;
        exitDialogFragment.mOnCancel = onCancel;
        return exitDialogFragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_confirm_exit_dialog, container, false);
        super.onShow(view);
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.bind(this, view);

        tvTitle.setText(mTitle);
        tvMessage.setText(mMessage);
        btnExit.setText(mExitText);
        btnExit.setOnClickListener(mOnSubmit);
        btnCancel.setOnClickListener(mOnCancel);
    }
}
