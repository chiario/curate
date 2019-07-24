package com.example.curate.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.service.autofill.SaveCallback;
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
import androidx.fragment.app.DialogFragment;

import com.example.curate.R;
import com.example.curate.models.Party;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.parse.FunctionCallback;
import com.parse.ParseException;

import org.w3c.dom.Text;

import butterknife.BindView;
import butterknife.ButterKnife;

public class InfoDialogAdminFragment extends DialogFragment {
    private static final String PARTY_NAME_KEY = "partyName";
    private static final String JOIN_CODE_KEY = "joinCode";
    private static final String DELETE_TAG = "DeleteQueue";


    @BindView(R.id.tvName) TextView tvPartyName;
    @BindView(R.id.tvCode) TextView tvJoinCode;
    @BindView(R.id.btnDelete) Button btnDelete;
    @BindView(R.id.ivQR) ImageView ivQR;
    @BindView(R.id.tvUserCount) TextView tvUserCount;
    @BindView(R.id.tvUserCountText) TextView tvUserCountText;

    private OnFragmentInteractionListener mListener;

    public InfoDialogAdminFragment() {
        // Required empty public constructor
    }

    public static InfoDialogAdminFragment newInstance(String partyName, String joinCode) {
        InfoDialogAdminFragment infoDialogAdminFragment = new InfoDialogAdminFragment();
        Bundle args = new Bundle();
        args.putString(PARTY_NAME_KEY, partyName);
        args.putString(JOIN_CODE_KEY, joinCode);
        infoDialogAdminFragment.setArguments(args);
        return infoDialogAdminFragment;
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
        View view = inflater.inflate(R.layout.fragment_info_dialog_admin, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.bind(this, view);

        // Store the listener (activity)
        mListener = (OnFragmentInteractionListener) getContext();
        // Set on click listener for delete button
        btnDelete.setOnClickListener(view1 -> onDeleteQueue());

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
        int count = Party.getPartyUserCount();
        tvUserCountText.setText(
                count == 1
                        ? getResources().getString(R.string.user_count_singular)
                        : getResources().getString(R.string.user_count_normal));
        tvUserCount.setText(String.valueOf(count));
    }


    private void onDeleteQueue() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete this party?")
                .setMessage("You won't be able to undo this action!")
                .setPositiveButton("Delete", (dialogInterface, i) -> {
                    mListener.onFragmentMessage(DELETE_TAG, null, null);
                    dismiss();
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> {});

        builder.show();
    }
}
