package com.example.curate.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.curate.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class InfoDialogFragment extends DialogFragment {
    private static final String PARTY_NAME_KEY = "partyName";
    private static final String JOIN_CODE_KEY = "joinCode";

    @BindView(R.id.etName) EditText etPartyName;
    @BindView(R.id.tvCode) TextView tvJoinCode;
    @BindView(R.id.btnDelete) Button btnDeleteParty;

    public interface InfoDialogListener {
        void onFinishInfoDialog();
    }

    // Empty constructor required
    public InfoDialogFragment() {}

    public static InfoDialogFragment newInstance(String partyName, String joinCode) {
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

    //Use this instead of onCreateDialog because the entire view is defined by our custom xml
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View inflateView = inflater.inflate(R.layout.fragment_info_dialog, container, false); //TODO- attach to root??
        //set a toolbar here if we want
        return inflateView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.bind(this, view);
        //add toolbar ops here if we want

        // Fetch arguments from bundle
        String partyName = getArguments().getString(PARTY_NAME_KEY);
        String joinCode = getArguments().getString(JOIN_CODE_KEY);
        if (partyName != null) {
            etPartyName.setText(partyName);
        } else {
            etPartyName.setHint("Add a party name...");
        }
        tvJoinCode.setText(joinCode);
    }

    @Override
    public void onResume() {
        WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes(params);

        super.onResume();
    }

    @OnClick(R.id.btnDelete)
    public void onDeleteParty() {
        // Close dialog and return to the activity through the listener
        InfoDialogListener listener = (InfoDialogListener) getActivity();
        listener.onFinishInfoDialog();
        dismiss();
    }
}
