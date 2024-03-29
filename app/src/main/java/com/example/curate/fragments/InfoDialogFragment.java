package com.example.curate.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.curate.R;
import com.example.curate.models.Party;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import butterknife.BindView;
import butterknife.ButterKnife;

public class InfoDialogFragment extends BlurDialogFragment {
    private static final String PARTY_NAME_KEY = "partyName";
    private static final String JOIN_CODE_KEY = "joinCode";

    @BindView(R.id.tvName) TextView tvPartyName;
    @BindView(R.id.tvJoinCode) TextView tvJoinCode;
    @BindView(R.id.ivQR) ImageView ivQR;
    @BindView(R.id.tvUserCountText) TextView tvUserCount;

    private static boolean mIsAdmin;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info_dialog, container, false);
        super.onShow(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.bind(this, view);
        // Fetch arguments from bundle
        String partyName = getArguments().getString(PARTY_NAME_KEY);
        String joinCode = getArguments().getString(JOIN_CODE_KEY);

        // Get QR code
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            // TODO: Fix hardcoded size?
            int side = (int) getResources().getDimension(R.dimen.QR_size);
            int remove = 10;
            Bitmap bitmap = barcodeEncoder.encodeBitmap(joinCode.toLowerCase(), BarcodeFormat.QR_CODE, side, side);
            Bitmap resized = Bitmap.createBitmap(bitmap, remove, remove, side - 2 * remove, side - 2 * remove);
            Glide.with(getContext()).load(resized).transform(new RoundedCorners(50)).into(ivQR);
        }
        catch (WriterException e) {
            e.printStackTrace();
        }

        // Populate views with party information
        if (partyName != null) {
            tvPartyName.setText(partyName);
        }
        tvJoinCode.setText("Join Code: " + joinCode);
        int count = Party.getCurrentParty().getPartyUserCount().intValue();
        tvUserCount.setText(count == 1
                ? count + " person partying"
                : count + " people partying");
    }
}
