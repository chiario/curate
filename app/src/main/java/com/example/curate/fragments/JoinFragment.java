package com.example.curate.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.curate.R;
import com.example.curate.activities.JoinActivity;
import com.example.curate.adapters.PartyAdapter;
import com.example.curate.models.Party;
import com.example.curate.utils.LocationManager;
import com.example.curate.utils.ToastHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class JoinFragment extends Fragment {
    private static final int BARCODE_READER_REQUEST_CODE = 100;

    @BindView(R.id.rvNearby) RecyclerView rvNearby;
    @BindView(R.id.etJoinCode) EditText etJoinCode;
    @BindView(R.id.tvMessage) TextView tvMessage;


    private SelectFragment.OnOptionSelected mListener;
    private View mRootView;
    private LocationManager mLocationManager;
    private PartyAdapter mAdapter;

    public JoinFragment() {
        // Required empty public constructor
    }

    public static JoinFragment newInstance() {
        JoinFragment fragment = new JoinFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_join, container, false);
        ButterKnife.bind(this, mRootView);

        tvMessage.setVisibility(View.GONE);

        mLocationManager = new LocationManager(this);
        if(mLocationManager.hasNecessaryPermissions()) {
            getNearbyParties();
        } else {
            mLocationManager.requestPermissions();
        }

        // Make join code CAPITALIZED
        InputFilter[] editFilters = etJoinCode.getFilters();
        InputFilter[] newFilters = new InputFilter[editFilters.length + 1];
        System.arraycopy(editFilters, 0, newFilters, 0, editFilters.length);
        newFilters[editFilters.length] = new InputFilter.AllCaps();
        etJoinCode.setFilters(newFilters);

        return mRootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SelectFragment.OnOptionSelected) {
            mListener = (SelectFragment.OnOptionSelected) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnOptionSelected");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == LocationManager.PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission has been granted, register location updater
                getNearbyParties();
            } else {
                Log.i("JoinFragment", "Location permission was not granted.");
            }
        }
    }

    private void getNearbyParties() {
        mLocationManager.getCurrentLocation(location -> {
            if(location == null) {
                tvMessage.setPadding(0,16, 0, 0); //TODO - style this better!
                tvMessage.setText("No nearby parties!");
                tvMessage.setVisibility(View.VISIBLE);
                return;
            }

            Log.d("LOCATION", String.format("lat: %f, long: %f", location.getLatitude(), location.getLongitude()));

            Party.getNearbyParties(LocationManager.createGeoPointFromLocation(location), (parties, e) -> {
                if(e == null) {
                    displayNearbyParties(parties);
                } else {
                    tvMessage.setText("No nearby parties!");
                    tvMessage.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void displayNearbyParties(List<Party> parties) {
        mAdapter = new PartyAdapter(getContext(), parties, mListener, (JoinActivity) getActivity());
        rvNearby.setAdapter(mAdapter);
        rvNearby.setLayoutManager(new LinearLayoutManager(getContext()));

    }

    @OnClick(R.id.ibScan)
    public void onScan() {
       IntentIntegrator.forSupportFragment(this)
               .setOrientationLocked(false)
               .setRequestCode(BARCODE_READER_REQUEST_CODE)
               .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
               .setPrompt("Scan a party barcode from the party info page")
               .setBeepEnabled(false)
               .initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == BARCODE_READER_REQUEST_CODE) {
            IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);
            if (result.getContents() == null) {
                ToastHelper.makeText(getContext(), "No QR scanned.");
            } else {
                joinParty(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @OnClick(R.id.btnJoin)
    public void onJoinParty() {
        String joinCode = etJoinCode.getText().toString().toLowerCase();
        joinParty(joinCode);
    }

    public void joinParty(String joinCode) {
        Party.joinParty(joinCode, e -> {
            if (e == null) {
                if (mListener != null) {
                    mListener.onPartyObtained();
                }
            } else {
                ToastHelper.makeText(getContext(), "Could not join party.");
            }
        });
    }
}
