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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.curate.R;
import com.example.curate.activities.MainActivity;
import com.example.curate.models.Party;
import com.example.curate.models.Settings;

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
        mPartyName = mCurrentParty.getSettings().getName();
        mIsLocationEnabled = mCurrentParty.getSettings().getLocationEnabled();
        mUserLimit = mCurrentParty.getSettings().getUserLimit();
//        mSongLimit = mCurrentParty.getSettings().getSongLimit();

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
        Settings saveSettings = getNewSettings();

        // TODO - check for location preferences changing somewhere else, maybe a new subscription in the main activity
        boolean isLocationEnabled = (saveSettings.getLocationEnabled() && !mCurrentParty.getLocationEnabled());
        boolean isLocationDisabled = (!saveSettings.getLocationEnabled() && mCurrentParty.getLocationEnabled());

        // Alert the user if they set the user limit or song limit to zero
        if (saveSettings.getUserLimit() == 0) {
            Toast.makeText(getContext(), "You can't set the user limit to zero!", Toast.LENGTH_LONG).show();
        }
        if (saveSettings.getSongLimit() == 0) {
            Toast.makeText(getContext(), "You can't set the song limit to zero!", Toast.LENGTH_LONG).show();
        }

        mCurrentParty.saveSettings(saveSettings, e -> {
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

    private Settings getNewSettings() {
        Settings newSettings = new Settings();
        newSettings.setName(etPartyName.getText().toString());
        newSettings.setLocationEnabled(switchLocation.isChecked());
        newSettings.setUserLimit(Integer.parseInt(tvUserLimitNumber.getText().toString()));
        newSettings.setSongLimit(Integer.parseInt(tvSongLimitNumber.getText().toString()));
        return newSettings;
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

        View inputView = getLayoutInflater().inflate(R.layout.fragment_input, null);
        EditText etInput = inputView.findViewById(R.id.etInput);
        etInput.setText(textView.getText().toString());
        TextView tvTitle = inputView.findViewById(R.id.tvTitle);
        tvTitle.setText(String.format("Set a new %s limit...", type));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(inputView);
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();

        inputView.findViewById(R.id.btnSubmit).setOnClickListener(view -> {
            dialog.dismiss();
            String newLimit = etInput.getText().toString();
            try {
                Integer.parseInt(newLimit);
                textView.setText(newLimit);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please input a number", Toast.LENGTH_LONG).show();
            }
        });
    }
}
