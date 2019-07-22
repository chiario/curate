package com.example.curate;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.curate.adapters.AnimatedLinearLayoutManager;
import com.example.curate.adapters.DividerItemDecoration;
import com.example.curate.adapters.ItemTouchHelperCallbacks;
import com.example.curate.adapters.PartyAdapter;
import com.example.curate.adapters.QueueAdapter;
import com.example.curate.models.Party;
import com.example.curate.utils.LocationManager;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class JoinFragment extends Fragment {
    @BindView(R.id.rvNearby) RecyclerView rvNearby;
    @BindView(R.id.etJoinCode) EditText etJoinCode;

    private SelectFrament.OnOptionSelected mListener;
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
        if (context instanceof SelectFrament.OnOptionSelected) {
            mListener = (SelectFrament.OnOptionSelected) context;
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

    @OnClick(R.id.btnJoin)
    public void onJoinParty(View view) {
        String joinCode = etJoinCode.getText().toString();
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
