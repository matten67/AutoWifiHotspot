package de.matlen67.awh;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MagicActivity extends PermissionsActivity {
    private static final String TAG = MagicActivity.class.getSimpleName();

    public static void useMagicActivityToTurnOn(Context c){
        Utils.isHotspotRunning = true;
        Uri uri = new Uri.Builder().scheme(c.getString(R.string.intent_data_scheme)).authority(c.getString(R.string.intent_data_host_turnon)).build();
        //Toast.makeText(c,"Turn on. Uri: "+uri.toString(),Toast.LENGTH_LONG).show();
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(uri);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    public static void useMagicActivityToTurnOff(Context c){
        Utils.isHotspotRunning = false;
        Uri uri = new Uri.Builder().scheme(c.getString(R.string.intent_data_scheme)).authority(c.getString(R.string.intent_data_host_turnoff)).build();
        //Toast.makeText(c,"Turn off. Uri: "+uri.toString(),Toast.LENGTH_LONG).show();
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(uri);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");

    }

    @Override
    void onPermissionsOkay() {
        carryOnWithHotSpotting();
    }


    /**
     * The whole purpose of this activity - to start {@link HotSpotIntentService}
     * This may be called straright away in {@code onCreate} or after permissions granted.
     */
    private void carryOnWithHotSpotting() {
        Intent intent = getIntent();
        HotSpotIntentService.start(this, intent);
        finish();
    }




}