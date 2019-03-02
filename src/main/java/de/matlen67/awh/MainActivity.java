package de.matlen67.awh;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    private GoogleMap mMap;
    private Geocoder gCoder;
    private LocationManager locationManager = null;
    private LocationListener locationListener = null;

    private GeofencingClient mGeofencingClient;
    private ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;
    private LocationRequest mLocationRequest;

    private long UPDATE_INTERVAL = 60 * 1000;  /* 60 secs */
    private long FASTEST_INTERVAL = 5000; /* 5 sec */

    private Context context;
    private TextView textViewCurrentLocation;
    private TextView textViewSeekBarProgress;
    private Switch mySwitch;
    private SeekBar seekBar;
    private boolean switchEnabled = false;
    private SharedPreferences sPrefs;
    private String currentAddress;
    private double currentDoubleLat;
    private double currentDoubleLon;
    private double HotSpotOnDoubleLat;
    private double HotSpotOnDoubleLon;
    private int hotSpotOnDistance;
    private int currentDistance;
    private int seekBarValue = 250;
    private boolean firstStart = false;


    private FusedLocationProviderClient fusedLocationClient;


    private enum PendingGeofenceTask {
        ADD, REMOVE, SUCCESS, FAILURE, NONE
    }

    private PendingGeofenceTask mPendingGeofenceTask = PendingGeofenceTask.NONE;


    public int getCurrentDistance() {
        return currentDistance;
    }

    public int getHotspotDistance() {
        return hotSpotOnDistance;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        sPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mGeofencingClient = LocationServices.getGeofencingClient(this);
        fusedLocationClient = getFusedLocationProviderClient(this);
        mGeofencePendingIntent = null;
        mGeofenceList = new ArrayList<>();


        HotSpotOnDoubleLat = Double.valueOf(sPrefs.getString("strLat", "0.0"));
        HotSpotOnDoubleLon = Double.valueOf(sPrefs.getString("strLon", "0.0"));
        hotSpotOnDistance = Integer.valueOf(sPrefs.getString("progress", "250"));
        seekBarValue = hotSpotOnDistance;

        textViewCurrentLocation = findViewById(R.id.textViewCurrentLocation);
        textViewCurrentLocation.setText("Waiting for Location");

        textViewSeekBarProgress = findViewById(R.id.textViewSeebBarProgress);
        textViewSeekBarProgress.setText("Starte Hotpot bei einer Entfernung von " + String.valueOf(hotSpotOnDistance) + " m zum Ziel ( " + currentDistance + " )");
        gCoder = new Geocoder(context);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        Button buttonPlus = findViewById(R.id.button_plus);
        buttonPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int value = seekBar.getProgress();
                if (value < 1000) {
                    seekBar.setProgress(value += 1);
                }
            }
        });


        Button buttonMinus = findViewById(R.id.button_minus);
        buttonMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int value = seekBar.getProgress();
                if (value > 10) {
                    seekBar.setProgress(value -= 1);
                }
            }
        });


        seekBar = findViewById(R.id.seekBar);
        seekBar.setProgress(hotSpotOnDistance);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarValue = progress;
                hotSpotOnDistance = progress;
                textViewSeekBarProgress.setText("Starte Hotpot bei einer Entfernung von " + String.valueOf(seekBarValue) + "m zum Ziel");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                SharedPreferences.Editor editor = sPrefs.edit();
                editor.putString("progress", String.valueOf(seekBarValue));
                editor.commit();

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                   // Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    setHotSpotCircleOnMap( getString(R.string.textStandort));
                }


            }
        });


        mySwitch = findViewById(R.id.switchEnableService);
        mySwitch.setChecked(sPrefs.getBoolean("switch", false));
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    Log.i("MainActivity", "mySwitch - isChecked = true");
                    SharedPreferences.Editor editor = sPrefs.edit();
                    editor.putBoolean("switch", true).apply();
                    switchEnabled = true;

                    HotSpotOnDoubleLat = Double.valueOf(sPrefs.getString("strLat", "0.0"));
                    HotSpotOnDoubleLon = Double.valueOf(sPrefs.getString("strLon", "0.0"));

                    if (mPendingGeofenceTask == PendingGeofenceTask.NONE) {
                        setGeofancing(HotSpotOnDoubleLat, HotSpotOnDoubleLon, seekBarValue);
                    } else if (mPendingGeofenceTask == PendingGeofenceTask.ADD) {
                        removeGeofencesClient();
                        setGeofancing(HotSpotOnDoubleLat, HotSpotOnDoubleLon, seekBarValue);
                    }

                } else {
                    Log.i("MainActivity", "mySwitch - isChecked = false");

                    SharedPreferences.Editor editor = sPrefs.edit();
                    editor.putBoolean("switch", false).apply();
                    switchEnabled = false;
                    if (mPendingGeofenceTask == PendingGeofenceTask.ADD) {
                        removeGeofencesClient();
                    }

                    Log.i("MainActivity", "mySwitch isHotspotRunning:" + Utils.isHotspotRunning);
                    Intent intentHotSpot = new Intent(getString(R.string.intent_action_turnoff));
                    sendImplicitBroadcast(getApplicationContext(), intentHotSpot);



                }
            }
        });


        startLocationUpdates();
        getLastLocation();

    }

    protected void startLocationUpdates() {

        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // do work here
                onLocationChanged(locationResult.getLastLocation());
            }
        }, Looper.myLooper());
    }


    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (!firstStart) {
            getLastLocation();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
            firstStart = true;
        }else {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            mMap.getUiSettings().setZoomControlsEnabled(true);
        }

        String errorMessage = "";

        setHotSpotCircleOnMap( getString(R.string.textStandort));
        currentDoubleLat = location.getLatitude();
        currentDoubleLon = location.getLongitude();
        Log.i("LOG_TAG", "Latitude = " + String.valueOf(currentDoubleLat) + " Longitude = " + String.valueOf(currentDoubleLon));

        //entfernung zum Ziel berechnen
        double entfernungInKm = PythagorasEquirectangular(currentDoubleLat, currentDoubleLon, HotSpotOnDoubleLat, HotSpotOnDoubleLon);
        double entfernungInMeter = (double) Math.round((entfernungInKm * 1000) * 1d) / 1d;
        currentDistance = (int) entfernungInMeter;

        textViewSeekBarProgress.setText("Starte Hotpot bei " + String.valueOf(hotSpotOnDistance) + " m zum Ziel ( " + currentDistance + " m )");

        Log.i("MainActivity", "Entfernung zum Ziel = " + currentDistance + " m");

        try {

            Geocoder geo = new Geocoder(this.getApplicationContext(), Locale.getDefault());
            List<Address> addresses = null;
            addresses = geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            if(addresses.isEmpty()){
                textViewCurrentLocation.setText("Waiting for Location");
            }
            else{
                //currentAddress = addresses.get(0).getAddressLine(0);
                textViewCurrentLocation.setText(addresses.get(0).getAddressLine(0));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void getLastLocation() {
        // Get last known recent location using new Google Play Services SDK (v11+)
        FusedLocationProviderClient locationClient = getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // GPS location can be null if GPS is switched off
                        if (location != null) {
                            onLocationChanged(location);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("MapDemoActivity", "Error trying to get last GPS location");
                        e.printStackTrace();
                    }
                });
    }


    private boolean isLocationPermissionEnable() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
            return false;
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
               // Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                //centreMapOnLocation(lastKnownLocation, getString(R.string.textStandort));
            }
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);

        LatLng enableHotSpot = new LatLng(HotSpotOnDoubleLat, HotSpotOnDoubleLon);
        mMap.addMarker(new MarkerOptions().position(enableHotSpot).title(getString(R.string.titleHotspot)));


        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                HotSpotOnDoubleLat = latLng.latitude;
                HotSpotOnDoubleLon = latLng.longitude;

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                   // Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    setHotSpotCircleOnMap( "Update");

                    SharedPreferences.Editor editor = sPrefs.edit();
                    editor.putString("strLat", String.valueOf(HotSpotOnDoubleLat));
                    editor.putString("strLon", String.valueOf(HotSpotOnDoubleLon));
                    editor.commit();
                    Log.i("MainActivity", "Start Hotspot Location " + HotSpotOnDoubleLat + ", " +HotSpotOnDoubleLon);

                    if (switchEnabled) {
                        switch (mPendingGeofenceTask) {
                            case NONE:
                                setGeofancing(latLng.latitude, latLng.longitude, seekBarValue);
                                break;

                            case ADD:
                                removeGeofencesClient();
                                setGeofancing(latLng.latitude, latLng.longitude, seekBarValue);
                                break;

                        }
                    }

                }
            }
        });
    }



    private void setGeofancing(double lat, double lng, int radius) {
        Log.d("MainAcivity", "setGeofancing");
        addGeofenceList(lat, lng, radius);
        addGeofencesClient();
    }


    private void addGeofencesClient() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("MainAcivity addGeofence ", "onSuccess");
                        mPendingGeofenceTask = PendingGeofenceTask.ADD;

                    }
                })

                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("MainAcivity addGeofence ", "onFailure" + e.toString());
                        mPendingGeofenceTask = PendingGeofenceTask.FAILURE;
                    }
                })

                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d("MainAcivity addGeofence ", "onComplete");
                    }
                });
    }


    private void removeGeofencesClient() {

        mGeofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("MainAcivity removeGeofence ", "onSuccess");
                        mPendingGeofenceTask = PendingGeofenceTask.NONE;

                    }
                })

                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("MainAcivity removeGeofence ", "onFailure" + e.toString());
                        mPendingGeofenceTask = PendingGeofenceTask.FAILURE;
                    }
                });

    }


    private void addGeofenceList(double lat, double lng, int radiusMeter){

        mGeofenceList.add(new  Geofence.Builder()
                .setRequestId("Ziel")
                .setCircularRegion(lat, lng, radiusMeter)
                .setExpirationDuration(-1) //newer Expire
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        );
    }


    private GeofencingRequest getGeofencingRequest() {
        Log.d("MainAcivity", "getGeofencingRequest");

        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }


    private PendingIntent getGeofencePendingIntent() {
        Log.d("MainAcivity Geofence ", "PendingIntent");

        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        mGeofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }




    private static void sendImplicitBroadcast(Context context, Intent i) {
        PackageManager pm=context.getPackageManager();
        List<ResolveInfo> matches=pm.queryBroadcastReceivers(i, 0);

        for (ResolveInfo resolveInfo : matches) {
            Intent explicit=new Intent(i);
            ComponentName cn=
                    new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName,
                            resolveInfo.activityInfo.name);

            explicit.setComponent(cn);
            context.sendBroadcast(explicit);
        }
    }


    public void setHotSpotCircleOnMap( String title) {

        try {
            LatLng enableHotSpot = new LatLng(HotSpotOnDoubleLat, HotSpotOnDoubleLon);
            //LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

            mMap.clear();
            //mMap.addMarker(new MarkerOptions().position(userLocation).title(title).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(HotSpotOnDoubleLat, HotSpotOnDoubleLon))
                    .radius(seekBarValue)
                    .strokeColor(Color.rgb(230, 200, 200))
                    .strokeWidth(3)
                    .fillColor(Color.TRANSPARENT));

            mMap.addMarker(new MarkerOptions().position(enableHotSpot).title(getString(R.string.titleHotspot)).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_hotspoton)));
            //.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))


        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }


    private double Deg2Rad( double deg ) {
        return deg * Math.PI / 180;
    }


    private double PythagorasEquirectangular( double lat1, double lon1, double lat2, double lon2 ) {
        lat1 = Deg2Rad(lat1);
        lat2 = Deg2Rad(lat2);
        lon1 = Deg2Rad(lon1);
        lon2 = Deg2Rad(lon2);
        double R = 6371.0; // km
        double x = (lon2-lon1) * Math.cos((lat1+lat2)/2);
        double y = (lat2-lat1);
        double d = Math.sqrt(x*x + y*y) * R;

        return d;
    }

}
