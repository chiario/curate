package com.example.curate.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.curate.R;
import com.example.curate.models.Party;
import com.example.curate.utils.ToastHelper;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SelectFragment extends Fragment {
    @BindView(R.id.buttonContainer) ConstraintLayout mButtonContainer;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;

    private OnOptionSelected mListener;
    private View mRootView;

    public SelectFragment() {
        // Required empty public constructor
    }

    public static SelectFragment newInstance() {
        SelectFragment fragment = new SelectFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_select, container, false);
        ButterKnife.bind(this, mRootView);

        ensureCurrentUserExists();
        return mRootView;
    }

    private void ensureCurrentUserExists() {
        if(ParseUser.getCurrentUser() == null) {
            ParseAnonymousUtils.logIn((user, e) -> {
                if (e != null) {
                    Log.e("JoinActivity", "Anonymous login failed!", e);
                    ToastHelper.makeText(getContext(), "User error.");
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    tryJoinExistingParty();
                }
            });
        } else {
            tryJoinExistingParty();
        }
    }

    private void tryJoinExistingParty() {
        Party.getExistingParty(e -> {
            if(e == null && Party.getCurrentParty() != null) {
                if (mListener != null) {
                    mListener.onPartyObtained();
                }
            } else {
                mProgressBar.setVisibility(View.GONE);
                mButtonContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnOptionSelected) {
            mListener = (OnOptionSelected) context;
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

    @OnClick(R.id.btnCreateParty)
    public void onCreateParty() {
        InputDialogFragment.SubmitListener submit = input -> Party.createParty(input, (ParseException e) -> {
            if (e == null) {
                if (mListener != null) {
                    mListener.onPartyObtained();
                }
            } else {
                ToastHelper.makeText(SelectFragment.this.getContext(),"Could not create party.");
            }
        });
        InputDialogFragment dialog = InputDialogFragment.newInstance(submit, "Party Name", "Give your party a name", false);
        dialog.show(getFragmentManager(), "Input");
    }

    @OnClick(R.id.btnJoinParty)
    public void onJoinParty() {
        mListener.onJoinPartySelected();
    }

    public interface OnOptionSelected {
        void onPartyObtained();

        void onJoinPartySelected();
    }
}
