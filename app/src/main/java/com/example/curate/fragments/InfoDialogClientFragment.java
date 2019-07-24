package com.example.curate.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.curate.R;
import com.example.curate.models.Party;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.parse.FunctionCallback;
import com.parse.ParseException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class InfoDialogClientFragment extends DialogFragment {
    private static final String PARTY_NAME_KEY = "partyName";
    private static final String JOIN_CODE_KEY = "joinCode";
    private static final String LEAVE_TAG = "LeaveQueue";

    @BindView(R.id.tvName) TextView tvPartyName;
    @BindView(R.id.tvCode) TextView tvJoinCode;
    @BindView(R.id.btnLeave) Button btnLeave;
    @BindView(R.id.ivQR) ImageView ivQR;
    @BindView(R.id.tvUserCount) TextView tvUserCount;
    @BindView(R.id.tvUserCountText) TextView tvUserCountText;

    private OnFragmentInteractionListener mListener;

    public InfoDialogClientFragment() {
        // Required empty public constructor
    }

    public static InfoDialogClientFragment newInstance(String partyName, String joinCode) {
        InfoDialogClientFragment infoDialogClientFragment = new InfoDialogClientFragment();
        Bundle args = new Bundle();
        args.putString(PARTY_NAME_KEY, partyName);
        args.putString(JOIN_CODE_KEY, joinCode);
        infoDialogClientFragment.setArguments(args);
        return infoDialogClientFragment;
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
        View view = inflater.inflate(R.layout.fragment_info_dialog_client, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.bind(this, view);

        // Store the listener (activity)
        mListener = (OnFragmentInteractionListener) getContext();
        // Set on click listener for delete button
        btnLeave.setOnClickListener(view1 -> onLeaveQueue());

        // Fetch arguments from bundle
        String partyName = getArguments().getString(PARTY_NAME_KEY);
        String joinCode = getArguments().getString(JOIN_CODE_KEY);

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            // TODO: Fix hardcoded size
            Bitmap bitmap = barcodeEncoder.encodeBitmap(joinCode, BarcodeFormat.QR_CODE, 200, 200);
            ivQR.setImageBitmap(bitmap);
        }
        catch (WriterException e) {
            e.printStackTrace();
        }

        // Set party name and join code
        if (partyName != null) {
            tvPartyName.setText(partyName);
        } else {
            tvPartyName.setHint("Add a party name...");
        }
        tvJoinCode.setText(joinCode);
        Party.getPartyUserCount(new FunctionCallback<Integer>() {
            @Override
            public void done(Integer object, ParseException e) {
                if(e != null)
                    Log.d("InfoDiaClientFrag", e.getMessage());
                if(object == 1)
                    tvUserCountText.setText(getResources().getString(R.string.user_count_singular));
                else
                    tvUserCountText.setText(getResources().getString(R.string.user_count_normal));
                tvUserCount.setText(String.valueOf(object));
            }
        });
    }


    private void onLeaveQueue() {
        String message = "You can rejoin this party with the following code: " + tvJoinCode.getText();
        int joinCodeColor = ContextCompat.getColor(getContext(), R.color.colorAccent_text);
        SpannableStringBuilder messageSpan = new SpannableStringBuilder(message);
        messageSpan.setSpan(new ForegroundColorSpan(joinCodeColor),
                message.length() - 4,
                message.length(),
                Spanned.SPAN_INCLUSIVE_INCLUSIVE
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Leave this party?")
                .setMessage(messageSpan)
                .setPositiveButton("Leave", (dialogInterface, i) -> {
                    mListener.onFragmentMessage(LEAVE_TAG, null, null);
                    dismiss();
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> {});

        builder.show();
    }
}
