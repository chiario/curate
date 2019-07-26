package com.example.curate;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;

public class PlayerResultReceiver extends ResultReceiver {
    private static final String TAG = "PlayerResultReceiver";
    private Receiver mReceiver;


    /**
     * Create a new ResultReceive to receive results.  Your
     * {@link #onReceiveResult} method will be called from the thread running
     * <var>handler</var> if given, or from an arbitrary thread if null.
     *
     * @param handler
     */
    public PlayerResultReceiver(Handler handler) {
        super(handler);
    }


    // Setter assigns the receiver
    public void setReceiver(Receiver receiver) {
        this.mReceiver = receiver;
    }

    // Interface for communication between activity and service
    public interface Receiver {
        void onReceiveResult(int resultCode, Bundle resultData);
    }


    // This method passes result to the receiver if receiver has been assigned
    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }
}
