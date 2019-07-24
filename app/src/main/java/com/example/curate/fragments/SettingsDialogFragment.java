package com.example.curate.fragments;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.curate.R;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SettingsDialogFragment extends DialogFragment {
    private static final String PARTY_NAME_KEY = "partyName";
    private static final String LOCATION_PERMISSIONS_KEY = "locationEnabled";
    private static final String SAVE_TAG = "SaveInfo";

    @BindView(R.id.switchLocation) Switch switchLocation;
    @BindView(R.id.etName) EditText etPartyName;

    private String partyName;
    private Boolean locationEnabled;
    private Toolbar toolbar;

    private OnFragmentInteractionListener mListener;

    public SettingsDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param name the name of the current party.
     * @param locationEnabled the current party's location enabled setting
     * @return A new instance of fragment SettingsDialogFragment.
     */
    public static SettingsDialogFragment newInstance(String name, boolean locationEnabled) {
        SettingsDialogFragment fragment = new SettingsDialogFragment();
        Bundle args = new Bundle();
        args.putString(PARTY_NAME_KEY, name);
        args.putBoolean(LOCATION_PERMISSIONS_KEY, locationEnabled);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout
        View view = inflater.inflate(R.layout.fragment_settings_dialog, container, false);
        // Set the inflated layout's toolbar before returning
        toolbar = view.findViewById(R.id.toolbar);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        // Store the listener (activity)
        mListener = (OnFragmentInteractionListener) getContext();

        // Set the toolbar and click listeners
        toolbar.setNavigationOnClickListener(v -> {
            dismiss();
        });
        toolbar.getNavigationIcon().setColorFilter(ContextCompat.getColor(getContext(), R.color.white), PorterDuff.Mode.SRC_IN);
        toolbar.inflateMenu(R.menu.menu_info);
        toolbar.setOnMenuItemClickListener(menuItem -> {
            Log.d("SettingsDialogFragment", "Save button selected");
            String newName = etPartyName.getText().toString();
            Boolean newLocationEnabled = switchLocation.isChecked();
            if (newName.equals(partyName)) {
                newName = null;
            }
            if (newLocationEnabled == locationEnabled) {
                newLocationEnabled = null;
            }
            mListener.onFragmentMessage(SAVE_TAG, newName, newLocationEnabled);
            dismiss();
            return true;
        });

        // Fetch arguments from bundle
        partyName = getArguments().getString(PARTY_NAME_KEY);
        locationEnabled = getArguments().getBoolean(LOCATION_PERMISSIONS_KEY);

        etPartyName.setText(partyName);
        switchLocation.setChecked(locationEnabled);

    }
}
