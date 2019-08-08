package com.example.curate.fragments;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
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
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
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
import com.example.curate.models.Settings;
import com.example.curate.models.Song;
import com.example.curate.utils.ToastHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTouch;


public class SettingsDialogFragment extends DialogFragment {
    private static final String TAG = "SettingsDialogFragment";
    @BindView(R.id.switchLocation) Switch switchLocation;
    @BindView(R.id.switchExplicit) Switch switchExplicit;
    @BindView(R.id.etName) EditText etPartyName;
    @BindView(R.id.tvUserLimitNumber) TextView tvUserLimitNumber;
    @BindView(R.id.tvSongLimitNumber) TextView tvSongLimitNumber;
    @BindView(R.id.ivPartyImage) ImageView imageView;
    @BindView(R.id.ivBackground) ImageView ivBackground;
    @BindView(R.id.btnSave) Button button6;
    @BindView(R.id.switchUserLimit) Switch switchUserLimit;
    @BindView(R.id.switchSongLimit) Switch switchSongLimit;

    private static final String TYPE_USER = "user";
    private static final String TYPE_SONG = "song";
    private Party mCurrentParty;
    private String mPartyName;
    private boolean mIsLocationEnabled;
    private Integer mUserLimit;
    private Integer mSongLimit;

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
        mSongLimit = mCurrentParty.getSettings().getSongLimit();

        etPartyName.setText(mPartyName);
        switchLocation.setChecked(mIsLocationEnabled);
        setLimit(TYPE_USER, mUserLimit);
        setLimit(TYPE_SONG, mSongLimit);


        Song currentSong = Party.getCurrentParty().getCurrentSong();
        if(currentSong != null) {
            String url = currentSong.getImageUrl();
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

    private void setLimit(String TYPE, Integer limit) {
        boolean enabled = limit != 0;
        switch (TYPE) {
            case TYPE_USER:
                switchUserLimit.setChecked(enabled);
                tvUserLimitNumber.setTextColor(enabled ? getResources().getColor(R.color.white)
                        : getResources().getColor(R.color.white_60));
                tvUserLimitNumber.setText(enabled ? Integer.toString(limit) : "None");
                break;
            case TYPE_SONG:
                switchSongLimit.setChecked(enabled);
                tvSongLimitNumber.setTextColor(enabled ? getResources().getColor(R.color.white)
                        : getResources().getColor(R.color.white_60));
                tvSongLimitNumber.setText(enabled ? Integer.toString(limit) : "None");
                break;
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

            ColorStateList buttonColorList = createButtonColorList(backgroundColor);
            ColorStateList switchColorList = createSwitchColorList(backgroundColor);

            ivBackground.setBackgroundColor(backgroundColor);
            button6.setBackgroundTintList(buttonColorList);

            DrawableCompat.setTintList(DrawableCompat.wrap(switchLocation.getThumbDrawable()), switchColorList);
            DrawableCompat.setTintList(DrawableCompat.wrap(switchLocation.getTrackDrawable()), switchColorList);

            DrawableCompat.setTintList(DrawableCompat.wrap(switchExplicit.getThumbDrawable()), switchColorList);
            DrawableCompat.setTintList(DrawableCompat.wrap(switchExplicit.getTrackDrawable()), switchColorList);
        });
    }

    private ColorStateList createButtonColorList(int color) {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_enabled}, // enabled
                new int[] {-android.R.attr.state_enabled}, // disabled
                new int[] {-android.R.attr.state_checked}, // unchecked
                new int[] { android.R.attr.state_pressed}  // pressed
        };

        int[] colors = new int[] {
                color,
                color,
                color,
                color
        };

        return new ColorStateList(states, colors);
    }

    private ColorStateList createSwitchColorList(int color) {
        int[][] states = new int[][] {
                new int[] {-android.R.attr.state_checked},
                new int[] {android.R.attr.state_checked},
        };

        int[] colors = new int[] {
                ContextCompat.getColor(getContext(), R.color.lightGray),
                color,
        };

        return new ColorStateList(states, colors);
    }

    @OnClick(R.id.btnSave)
    public void onSaveSettings() {
        Settings saveSettings = getNewSettings();

        // TODO - check for location preferences changing somewhere else, maybe a new subscription in the main activity
        boolean isLocationEnabled = (saveSettings.getLocationEnabled() && !mCurrentParty.getLocationEnabled());
        boolean isLocationDisabled = (!saveSettings.getLocationEnabled() && mCurrentParty.getLocationEnabled());

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
        newSettings.setUserLimit(switchUserLimit.isChecked() ?
                Integer.parseInt(tvUserLimitNumber.getText().toString()) : 0);
        newSettings.setSongLimit(switchSongLimit.isChecked() ?
                Integer.parseInt(tvSongLimitNumber.getText().toString()) : 0);
        return newSettings;
    }

    @OnClick({R.id.switchUserLimit, R.id.tvUserLimitText, R.id.tvUserLimitNumber})
    void setUserLimit() {
        if (switchUserLimit.isChecked()) {
            buildAlertDialog(TYPE_USER, tvUserLimitNumber);
        } else {
            setLimit(TYPE_USER, 0);
        }
    }

    @OnClick({R.id.switchSongLimit, R.id.tvSongLimitText, R.id.tvSongLimitNumber})
    void setSongLimit() {
        if (switchSongLimit.isChecked()) {
            buildAlertDialog(TYPE_SONG, tvSongLimitNumber);
        } else {
            setLimit(TYPE_SONG, 0);
        }
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

    private void buildAlertDialog(String TYPE, TextView textView) {
        View inputView = getLayoutInflater().inflate(R.layout.fragment_input, null);
        EditText etInput = inputView.findViewById(R.id.etInput);
        TextView tvTitle = inputView.findViewById(R.id.tvTitle);

        Integer currLimit = getCurrentLimit(TYPE);
        if (currLimit != 0) {
            etInput.setText(String.format("%s", currLimit));
        }

        tvTitle.setText(String.format("Set a new %s limit...", TYPE));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(inputView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();

        inputView.findViewById(R.id.btnSubmit).setOnClickListener(view -> {
            dialog.dismiss();
            String newLimitAsString = etInput.getText().toString();
            try {
                Integer newLimit = Integer.parseInt(newLimitAsString);
                setLimit(TYPE, newLimit);
            } catch (NumberFormatException e) {
                setLimit(TYPE, currLimit);
                ToastHelper.makeText(getContext(), "Please input a number");
            }

        });
    }

    private Integer getCurrentLimit(String TYPE) {
        TextView tvLimit = tvUserLimitNumber;
        if (TYPE.equals(TYPE_SONG)) {
            tvLimit = tvSongLimitNumber;
        }
        String currLimitAsString = tvLimit.getText().toString();
        return currLimitAsString.equals("None") ? 0 : Integer.parseInt(currLimitAsString);
    }
}
