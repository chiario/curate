package com.example.curate.fragments;

import android.app.AlertDialog;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.curate.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class SettingsDialogFragment extends DialogFragment {
    private static final String PARTY_NAME_KEY = "partyName";
    private static final String LOCATION_PERMISSIONS_KEY = "locationEnabled";

    @BindView(R.id.switchLocation) Switch switchLocation;
    @BindView(R.id.etName) EditText etPartyName;
    @BindView(R.id.clUserLimit) ConstraintLayout clUserLimit;
    @BindView(R.id.tvUserLimitNumber) TextView tvUserLimitNumber;

    private String mPartyName;
    private Boolean isLocationEnabled;
    private Toolbar mToolbar;
    private OnSaveListener mListener;

    public interface OnSaveListener {
        void onSaveInfo(@Nullable String newName, @Nullable Boolean locationEnabled);
    }

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
        mToolbar = view.findViewById(R.id.toolbar);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        // Fetch arguments from bundle
        mPartyName = getArguments().getString(PARTY_NAME_KEY);
        isLocationEnabled = getArguments().getBoolean(LOCATION_PERMISSIONS_KEY);
        etPartyName.setText(mPartyName);
        switchLocation.setChecked(isLocationEnabled);

        // Store the listener
        mListener = (OnSaveListener) getContext();

        // Set the toolbar and click listeners
        mToolbar.setNavigationOnClickListener(v -> {
            dismiss();
        });
        mToolbar.getNavigationIcon().setColorFilter(ContextCompat.getColor(getContext(), R.color.white), PorterDuff.Mode.SRC_IN);
        mToolbar.inflateMenu(R.menu.menu_info);
        mToolbar.setOnMenuItemClickListener(menuItem -> {
            Log.d("SettingsDialogFragment", "Save button selected");
            String newName = etPartyName.getText().toString();
            Boolean newLocationEnabled = switchLocation.isChecked();
            if (newName.equals(mPartyName)) {
                newName = null;
            }
            if (newLocationEnabled == isLocationEnabled) {
                newLocationEnabled = null;
            }
            mListener.onSaveInfo(newName, newLocationEnabled);
            dismiss();
            return true;
        });
    }

    @OnClick(R.id.clUserLimit)
    public void setUserLimit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        EditText etUserLimitNumber = new EditText(getContext());
        builder.setView(etUserLimitNumber)
                .setTitle("Set a user limit")
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    String limit = etUserLimitNumber.getText().toString();
                    tvUserLimitNumber.setText(limit);
                })
                .setNegativeButton("CANCEL", (dialogInterface, i) -> dismiss())
                .show();

    }
}
