package com.example.curate.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.curate.R;
import com.example.curate.models.Party;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import butterknife.BindView;
import butterknife.ButterKnife;

public class InfoDialogFragment extends DialogFragment {
    private static final String PARTY_NAME_KEY = "partyName";
    private static final String JOIN_CODE_KEY = "joinCode";

    @BindView(R.id.tvName) TextView tvPartyName;
    @BindView(R.id.tvCode) TextView tvJoinCode;
    @BindView(R.id.btnDelete) Button btnDelete;
    @BindView(R.id.ivQR) ImageView ivQR;
    @BindView(R.id.tvUserCount) TextView tvUserCount;
    @BindView(R.id.tvUserCountText) TextView tvUserCountText;

    private OnDeleteListener mListener;
    private static boolean mIsAdmin;

    public interface OnDeleteListener {
        void onDeleteQueue();
        void onLeaveQueue();
    }

    public InfoDialogFragment() {
        // Required empty public constructor
    }

    public static InfoDialogFragment newInstance(String partyName, String joinCode, Boolean isAdmin) {
        mIsAdmin = isAdmin;
        InfoDialogFragment infoDialogFragment = new InfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(PARTY_NAME_KEY, partyName);
        args.putString(JOIN_CODE_KEY, joinCode);
        infoDialogFragment.setArguments(args);
        return infoDialogFragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // Use this instead of onCreateDialog because the entire view is defined by our custom xml
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_info_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.bind(this, view);
        // Fetch arguments from bundle
        String partyName = getArguments().getString(PARTY_NAME_KEY);
        String joinCode = getArguments().getString(JOIN_CODE_KEY);
        // Store the listener
        mListener = (OnDeleteListener) getContext();

        btnDelete.setText(mIsAdmin?"Delete":"Leave");
        // Set on click listener for delete button
        btnDelete.setOnClickListener(view1 -> onDeleteQueue());

        // Get QR code
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            // TODO: Fix hardcoded size?
            Bitmap bitmap = barcodeEncoder.encodeBitmap(joinCode, BarcodeFormat.QR_CODE, 300, 300);
            ivQR.setImageBitmap(bitmap);
        }
        catch (WriterException e) {
            e.printStackTrace();
        }

        // Populate views with party information
        if (partyName != null) {
            tvPartyName.setText(partyName);
        }
        tvJoinCode.setText(joinCode);
        int count = Party.getPartyUserCount();
        tvUserCountText.setText(
                count == 1
                        ? getResources().getString(R.string.user_count_singular)
                        : getResources().getString(R.string.user_count_normal));
        tvUserCount.setText(String.valueOf(count));
    }


    private void onDeleteQueue() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder
            .setTitle(mIsAdmin
                ? "Delete this party?"
                : "Leave this party?")
            .setMessage(mIsAdmin
                    ? "You won't be able to undo this action!"
                    : "You can rejoin with the code " + tvJoinCode.getText().toString())
            .setPositiveButton(mIsAdmin ? "Delete" : "Leave", (dialogInterface, i) -> {
                if (mIsAdmin) {
                    mListener.onDeleteQueue();
                } else {
                    mListener.onLeaveQueue();
                }
                dismiss();
            })
            .setNegativeButton("Cancel", (dialogInterface, i) -> {});
        builder.show();
    }
}