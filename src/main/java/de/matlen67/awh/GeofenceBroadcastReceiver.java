package de.matlen67.awh;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("GeofenceBroadcastReceiver", "onReceive");
        GeofenceTransitionsJobIntentService.enqueueWork(context, intent);
    }
}