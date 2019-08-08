package com.example.curate.fragments;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.curate.R;
import com.example.curate.activities.MainActivity;
import com.example.curate.models.Party;
import com.example.curate.models.PlaylistEntry;
import com.example.curate.models.Settings;
import com.example.curate.utils.ToastHelper;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTouch;


public class SettingsDialogFragment extends DialogFragment {
    private static final String TAG = "SettingsDialogFragment";
    @BindView(R.id.switchLocation) Switch switchLocation;
    @BindView(R.id.etName) EditText etPartyName;
    @BindView(R.id.tvUserLimitNumber) TextView tvUserLimitNumber;
    @BindView(R.id.tvSongLimitNumber) TextView tvSongLimitNumber;
    @BindView(R.id.ivPartyImage) ImageView imageView;
    @BindView(R.id.ivBackground) ImageView ivBackground;
    @BindView(R.id.btnSave) Button button6;


    private Party mCurrentParty;
    private String mPartyName;
    private boolean mIsLocationEnabled;
    private int mUserLimit;
    private int mSongLimit;

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

        List<PlaylistEntry> entries = Party.getCurrentParty().getPlaylist().getEntries();
        if(!entries.isEmpty()) {
            String url = entries.get(0).getSong().getImageUrl();
            Glide.with(getContext())
                    .asBitmap()
                    .load(url)
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            setBackgroundColor(resource);
                            return false;
                        }
                    })
                    .transform(new CircleCrop())
                    .into(imageView);

        }
    }

    @OnClick(R.id.ivClose)
    public void cancel() {
        dismiss();
    }

    @OnEditorAction(R.id.etName)
    public boolean onEditorAction(int actionId) {
        if(actionId == EditorInfo.IME_ACTION_DONE){
            etPartyName.clearFocus();
        }
        return false;
    }

    private void setBackgroundColor(Bitmap bitmap) {
        Palette.from(bitmap).generate(p -> {
            // Load default colors
            int backgroundColor = ContextCompat.getColor(getContext(), R.color.colorAccent);

            Palette.Swatch swatch = p.getVibrantSwatch();
            if(swatch != null) {
                backgroundColor = swatch.getRgb();
            }

            ivBackground.setBackgroundColor(backgroundColor);



            int[][] states = new int[][] {
                    new int[] { android.R.attr.state_enabled}, // enabled
                    new int[] {-android.R.attr.state_enabled}, // disabled
                    new int[] {-android.R.attr.state_checked}, // unchecked
                    new int[] { android.R.attr.state_pressed}  // pressed
            };

            int[] colors = new int[] {
                    backgroundColor,
                    backgroundColor,
                    backgroundColor,
                    backgroundColor
            };

            ColorStateList myList = new ColorStateList(states, colors);

            button6.setBackgroundTintList(myList);
        });
    }

    @OnClick(R.id.btnSave)
    public void onSaveSettings() {
        Settings saveSettings = getNewSettings();

        // TODO - check for location preferences changing somewhere else, maybe a new subscription in the main activity
        boolean isLocationEnabled = (saveSettings.getLocationEnabled() && !mCurrentParty.getLocationEnabled());
        boolean isLocationDisabled = (!saveSettings.getLocationEnabled() && mCurrentParty.getLocationEnabled());

        // Alert the user if they set the user limit or song limit to zero
        if (saveSettings.getUserLimit() == 0) {

            ToastHelper.makeText(getContext(), "You can't set the user limit to zero!");
        }
        if (saveSettings.getSongLimit() == 0) {
            ToastHelper.makeText(getContext(), "You can't set the song limit to zero!");
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
                ToastHelper.makeText(getContext(), "Could not save settings");
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

    @OnTouch(R.id.clSettings)
    void onTouch(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Rect outRect = new Rect();
            etPartyName.getGlobalVisibleRect(outRect);
            if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                etPartyName.clearFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etPartyName.getWindowToken(), 0);
            }
        }
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
                ToastHelper.makeText(getContext(), "Please input a number");
            }
        });
    }
}
