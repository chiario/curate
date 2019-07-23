package com.example.curate.fragments;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.curate.R;
import com.example.curate.models.Party;
import com.example.curate.models.Song;
import com.example.curate.utils.LocationManager;
import com.example.curate.utils.SpotifyPlayer;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.parse.ParseException;
import com.parse.SaveCallback;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerContext;
import com.spotify.protocol.types.PlayerState;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BottomPlayerClientFragment extends Fragment {
    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvArtist) TextView tvArtist;
    @BindView(R.id.ivAlbum) ImageView ivAlbum;

    private SaveCallback mCurrentSongUpdatedCallback;
    private Party mParty;

    public BottomPlayerClientFragment() {
        // Required empty public constructor
    }

    public static BottomPlayerClientFragment newInstance() {
        BottomPlayerClientFragment fragment = new BottomPlayerClientFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bottom_player_client, container, false);
        ButterKnife.bind(this, view);
        mParty = Party.getCurrentParty();

        initializeSongUpdateCallback();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeSongUpdateCallback();
    }

    private void initializeSongUpdateCallback() {
        mCurrentSongUpdatedCallback = e -> {
            if (e == null) {
                Song currentSong = Party.getCurrentParty().getCurrentSong();
                if (currentSong != null) {
                    try {
                        currentSong.fetchIfNeeded(); // TODO - work around this fetch; add ParseCloud function??
                        tvTitle.setText(currentSong.getTitle());
                        tvArtist.setText(currentSong.getArtist());
                        Glide.with(this).load(currentSong.getImageUrl()).into(ivAlbum);
                    } catch (ParseException e1) {
                        e1.printStackTrace();
                    }
                }
            } else {
                Log.d("BottomPlayerClient", "Error in song update callback", e);
            }
        };
        mParty.registerPlaylistUpdateCallback(mCurrentSongUpdatedCallback);
    }

    private void removeSongUpdateCallback() {
        mParty.deregisterPlaylistUpdateCallback(mCurrentSongUpdatedCallback);
    }
}
