package com.example.curate.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.curate.R;
import com.example.curate.adapters.PartyAdapter;
import com.example.curate.models.Party;
import com.example.curate.utils.LocationManager;
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

        mLocationManager = new LocationManager(this);
        if(mLocationManager.hasNecessaryPermissions()) {
            getNearbyParties();
        } else {
            mLocationManager.requestPermissions();
        }


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

            Log.d("LOCATION", String.format("lat: %f, long: %f", location.getLatitude(), location.getLongitude()));

            Party.getNearbyParties(LocationManager.createGeoPointFromLocation(location), (parties, e) -> {
                if(e == null) {
                    displayNearbyParties(parties);
                } else {
                    Toast.makeText(getContext(), "Could not get nearby parties!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void displayNearbyParties(List<Party> parties) {
        mAdapter = new PartyAdapter(getContext(), parties, mListener);
        rvNearby.setAdapter(mAdapter);
        rvNearby.setLayoutManager(new LinearLayoutManager(getContext()));

    }

    @OnClick(R.id.btnScan)
    public void onScan() {
       IntentIntegrator.forSupportFragment(this)
               .setOrientationLocked(false)
               .setRequestCode(BARCODE_READER_REQUEST_CODE)
               .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
               .setPrompt("Scan a party barcode from the Admin's info page")
               .setBeepEnabled(false)
               .initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == BARCODE_READER_REQUEST_CODE) {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result == null || result.getContents() == null) {
                Toast.makeText(getContext(), "No QR Scanned", Toast.LENGTH_LONG).show();
            } else {
                joinParty(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @OnClick(R.id.btnJoin)
    public void onJoinParty() {
        String joinCode = etJoinCode.getText().toString();
        joinParty(joinCode);
    }

    public void joinParty(String joinCode) {
        Party.joinParty(joinCode, e -> {
            if (e == null) {
                if (mListener != null) {
                    mListener.onPartyObtained();
                }
            } else {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
