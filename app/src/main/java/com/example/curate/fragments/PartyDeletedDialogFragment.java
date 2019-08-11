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

public class PartyDeletedDialogFragment extends BlurDialogFragment {

    @BindView(R.id.btnExit) Button btnExit;

    View.OnClickListener mOnSubmit;

    public PartyDeletedDialogFragment() {
        // Required empty public constructor
    }

    public static PartyDeletedDialogFragment newInstance(View.OnClickListener onSumbit) {
        PartyDeletedDialogFragment exitDialogFragment = new PartyDeletedDialogFragment();
        exitDialogFragment.mOnSubmit = onSumbit;
        return exitDialogFragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_party_deleted_dialog, container, false);
        super.onShow(view);
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.bind(this, view);

        btnExit.setOnClickListener(mOnSubmit);
    }
}
