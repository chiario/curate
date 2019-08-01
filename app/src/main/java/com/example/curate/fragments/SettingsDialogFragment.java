package com.example.curate.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.curate.R;
import com.example.curate.activities.MainActivity;
import com.example.curate.models.Party;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class SettingsDialogFragment extends DialogFragment {
    @BindView(R.id.switchLocation) Switch switchLocation;
    @BindView(R.id.etName) EditText etPartyName;
    @BindView(R.id.tvUserLimitNumber) TextView tvUserLimitNumber;
    @BindView(R.id.tvSongLimitNumber) TextView tvSongLimitNumber;

    private Party mCurrentParty;
    private String mPartyName;
    private boolean mIsLocationEnabled;
    private int mUserLimit;
    private int mSongLimit;
    private Toolbar mToolbar;

    public SettingsDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SettingsDialogFragment.
     */
    public static SettingsDialogFragment newInstance() {
        SettingsDialogFragment fragment = new SettingsDialogFragment();
        Bundle args = new Bundle();
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
        mCurrentParty = Party.getCurrentParty();
        // Fetch arguments and set views
        mPartyName = mCurrentParty.getName();
        mIsLocationEnabled = mCurrentParty.getLocationEnabled();
        mUserLimit = mCurrentParty.getUserLimit();
        mSongLimit = mCurrentParty.getSongLimit();

        etPartyName.setText(mPartyName);
        switchLocation.setChecked(mIsLocationEnabled);
        tvUserLimitNumber.setText(Integer.toString(mUserLimit));
        tvSongLimitNumber.setText(Integer.toString(mSongLimit));

        setToolbar();
    }

    private void setToolbar() {
        mToolbar.setNavigationOnClickListener(v -> {
            dismiss();
        });
        mToolbar.getNavigationIcon().setTint(ContextCompat.getColor(getContext(), R.color.white));
        mToolbar.inflateMenu(R.menu.menu_info);
        mToolbar.setOnMenuItemClickListener(menuItem -> {
            onSaveSettings();
            return true;
        });
    }

    private void onSaveSettings() {
        String newName = etPartyName.getText().toString();
        boolean newLocationEnabled = switchLocation.isChecked();
        boolean isLocationEnabled = (newLocationEnabled && !mCurrentParty.getLocationEnabled());
        boolean isLocationDisabled = (!newLocationEnabled && mCurrentParty.getLocationEnabled());
        int newUserLimit = Integer.parseInt(tvUserLimitNumber.getText().toString());
        int newSongLimit = Integer.parseInt(tvSongLimitNumber.getText().toString());
        if (newUserLimit == 0) {
            Toast.makeText(getContext(), "You can't set the user limit to zero!", Toast.LENGTH_LONG).show();
        }
        mCurrentParty.saveSettings(newLocationEnabled, newName, newUserLimit, newSongLimit, e -> {
            if(e == null) {
                if (isLocationEnabled) {
                    ((MainActivity) getActivity()).registerLocationUpdater();
                } else if (isLocationDisabled){
                    ((MainActivity) getActivity()).deregisterLocationUpdater();
                }
                dismiss();
            } else {
                Toast.makeText(getContext(), "Could not save settings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @OnClick({R.id.tvUserLimitText, R.id.tvUserLimitNumber})
    void setUserLimit() {
        buildAlertDialog("user", tvUserLimitNumber);

    }

    @OnClick({R.id.tvSongLimitNumber, R.id.tvSongLimitText})
    void setSongLimit() {
        buildAlertDialog("song", tvSongLimitNumber);
    }

    private void buildAlertDialog(String type, TextView textView) {
        EditText etLimit = new EditText(getContext());
        etLimit.setText(textView.getText().toString());

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        builder.setView(etLimit)
                .setTitle(String.format("Set a new %s limit...", type))
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    String newLimit = etLimit.getText().toString();
                    try {
                        Integer.parseInt(newLimit);
                        textView.setText(newLimit);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Please input a number", Toast.LENGTH_LONG).show();
                    }
                })
                .show();

    }
}
