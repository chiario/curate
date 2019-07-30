package com.example.curate.service;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.spotify.protocol.types.PlayerState;

import java.io.ByteArrayOutputStream;

public class ServiceUtils {

    // Data keys
    public static final String RECEIVER_KEY = "receiver";
    public static final String SONG_ID_KEY = "spotifyId";
    public static final String PLAYBACK_POS_KEY = "playbackPosition";
    public static final String DURATION_KEY = "duration";
    public static final String PAUSED_KEY = "isPaused";
    public static final String TITLE_KEY = "title";
    public static final String ARTIST_KEY = "artist";
    public static final String IMAGE_KEY = "image";

    // Action keys
    public static final String ACTION_PLAY = "action.PLAY";
    public static final String ACTION_UPDATE = "action.UPDATE";
    public static final String ACTION_CONNECT = "action.CONNECT";
    public static final String ACTION_SKIP = "action.SKIP";
    public static final String ACTION_PLAY_PAUSE = "action.PLAY_PAUSE";

    // Result keys
    public static final int RESULT_NEW_SONG = 123;
    public static final int RESULT_PLAY_PAUSE = 456;
    public static final int RESULT_ALBUM_ART = 789;
    public static final int RESULT_PLAYBACK = 1000;

    // Unique job ID for this service
    private static final int PLAYER_JOB_ID = 1000;

    /**
     * Convenience method for enqueuing work into this service.
     */
    public static void enqueuePlayer(Context context, PlayerResultReceiver playerResultReceiver, String ACTION, @Nullable Bundle data) {
        Intent intent = new Intent(context, PlayerService.class);
        intent.putExtra(RECEIVER_KEY, playerResultReceiver);

        switch (ACTION) {
            case ACTION_UPDATE:
                long seekTo = data.getLong(PLAYBACK_POS_KEY);
                intent.putExtra(PLAYBACK_POS_KEY, seekTo);
                break;
            case ACTION_PLAY:
                String newSongId = data.getString(SONG_ID_KEY);
                intent.putExtra(SONG_ID_KEY, newSongId); // TODO - more efficient way to do this??
                break;
        }

        intent.setAction(ACTION);
        PlayerService.enqueueWork(context, PlayerService.class, PLAYER_JOB_ID, intent);
    }

    public static Bundle bundleTrack(PlayerState playerState) {
        Bundle bundle = new Bundle();
        bundle.putString(SONG_ID_KEY, playerState.track.uri);
        bundle.putString(TITLE_KEY, playerState.track.name);
        bundle.putString(ARTIST_KEY, playerState.track.artist.name);
        bundle.putLong(DURATION_KEY, playerState.track.duration);
        bundle.putBoolean(PAUSED_KEY, playerState.isPaused);
        bundle.putLong(PLAYBACK_POS_KEY, playerState.playbackPosition);
        return bundle;
    }

    public static Bundle bundlePlayback(PlayerState playerState) {
        Bundle bundle = new Bundle();
        bundle.putLong(PLAYBACK_POS_KEY, playerState.playbackPosition);
        bundle.putBoolean(PAUSED_KEY, playerState.isPaused);
        return bundle;
    }

    public static Bundle bundleBitmap(Bitmap bitmap) {
        Bundle bundle = new Bundle();
        // convert bitmap to Byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        bundle.putByteArray(IMAGE_KEY,byteArray);
        return bundle;
    }
}
