package de.matlen67.awh.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.matlen67.awh.MagicActivity;
import de.matlen67.awh.R;



public class HotSpotIntentReceiver extends BroadcastReceiver {

    private final static String TAG = HotSpotIntentReceiver.class.getSimpleName();
    private final static String ACTION_TURNON = "de.matlen67.awh.TURN_ON";
    private final static String ACTION_TURNOFF = "de.matlen67.awh.TURN_OFF";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG,"Received intent");

        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_TURNON.equals(action)) {
                MagicActivity.useMagicActivityToTurnOn(context);
            } else if (ACTION_TURNOFF.equals(action)) {
                MagicActivity.useMagicActivityToTurnOff(context);
            }
        }

    }
}
