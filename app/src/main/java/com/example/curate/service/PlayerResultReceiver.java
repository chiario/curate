package com.example.curate.service;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;

import static com.example.curate.service.ServiceUtils.RESULT_CONNECTED;
import static com.example.curate.service.ServiceUtils.RESULT_DISCONNECTED;

public class PlayerResultReceiver extends ResultReceiver {
    private static final String TAG = "PlayerResultReceiver";
    private Receiver mReceiver;
    private static boolean mIsSpotifyConnected;


    /**
     * Create a new ResultReceive to receive results.  Your
     * {@link #onReceiveResult} method will be called from the thread running
     * <var>handler</var> if given, or from an arbitrary thread if null.
     *
     * @param handler
     */
    public PlayerResultReceiver(Handler handler) {
        super(handler);
        mIsSpotifyConnected = false;
    }


    // Setter assigns the receiver
    public void setReceiver(Receiver receiver) {
        this.mReceiver = receiver;
    }

    // Interface for communication between activity and service
    public interface Receiver {
        void onReceiveResult(int resultCode, Bundle resultData);
    }

    /**
     * This method is called on result from the service.
     * If the Spotify remote player is connected or disconnected, it sets mIsSpotifyConnected.
     * Otherwise, it passes the result to the receiver if the receiver has been assigned.
     *
     * @param resultCode the integer code for the result from the service
     * @param resultData any data from the service
     */
    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode == RESULT_CONNECTED) {
            mIsSpotifyConnected = true;
        } else if (resultCode == RESULT_DISCONNECTED) {
            mIsSpotifyConnected = false;
        } else if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }

    public static boolean isSpotifyConnected() {
        return mIsSpotifyConnected;
    }
}
