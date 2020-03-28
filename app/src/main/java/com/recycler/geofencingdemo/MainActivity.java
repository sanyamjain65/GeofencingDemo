package com.recycler.geofencingdemo;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.recycler.geofencingdemo.activities.SplashActivity;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 101;
    private static final int RC_SIGN_IN = 101;
    private GoogleMap googleMap;
    private GeofencingRequest geofencingRequest;
    private GoogleApiClient googleApiClient;
    private boolean isMonitoring = false;
    private MarkerOptions markerOptions;
    private Marker currentLocationMarker;
    private PendingIntent pendingIntent;
    private SeekBar seekBar;
    private TextView tv_seekbar;
    private Button bt_start;
    private RelativeLayout rl_seekbar;
    Circle circle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseDb.INSTANCE.triggerAlert();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        long minimumRadius = FirebaseRemoteConfig.getInstance().getLong("minimum_radius");
        long maximumRadius = FirebaseRemoteConfig.getInstance().getLong("maximum_radius");

        if (minimumRadius <= 0) minimumRadius = 5;

        if (maximumRadius <= 10) maximumRadius = 10;


        seekBar = findViewById(R.id.seekBar);
        tv_seekbar = findViewById(R.id.tv_seekbar);
        bt_start = findViewById(R.id.bt_start);
        rl_seekbar = findViewById(R.id.rl_seekbar);

        seekBar.setMax((int) maximumRadius);
        seekBar.setMin((int) minimumRadius);
        seekBar.setProgress((int) (minimumRadius + ((maximumRadius - minimumRadius) / 2)));
        tv_seekbar.setText(String.valueOf(seekBar.getProgress()));

        googleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE);
        }


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.d(TAG, "onProgressChanged: value of seekbar is " + i);
                Constants.GEOFENCE_RADIUS_IN_METERS = (float) i;
                tv_seekbar.setText(String.valueOf(i));
                circle.setRadius(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //                Log.d(TAG, "onProgressChanged: value of seekbar is " + seekBar.get);

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //                Log.d(TAG, "onProgressChanged: value of seekbar is " + i);

            }
        });

        bt_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isMonitoring = true;
                startGeofencing();
                startLocationMonitor();
                rl_seekbar.setVisibility(View.GONE);
            }
        });

    }

    private void startLocationMonitor() {
        Log.d(TAG, "start location monitor");
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    if (currentLocationMarker != null) {
                        currentLocationMarker.remove();
                    }
                    markerOptions = new MarkerOptions();
                    markerOptions.position(new LatLng(location.getLatitude(), location.getLongitude()));
                    markerOptions.title("Current Location");
                    currentLocationMarker = googleMap.addMarker(markerOptions);
                    Log.d(TAG, "Location Change Lat Lng " + location.getLatitude() + " " + location.getLongitude());
                }
            });
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
        }

    }

    private void startGeofencing() {
        Log.d(TAG, "Start geofencing monitoring call");
        pendingIntent = getGeofencePendingIntent();
        geofencingRequest = new GeofencingRequest.Builder().setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
                .addGeofence(getGeofence())
                .build();

        if (!googleApiClient.isConnected()) {
            Log.d(TAG, "Google API client not connected");
        } else {
            try {
                LocationServices.GeofencingApi.addGeofences(googleApiClient, geofencingRequest, pendingIntent)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    Log.d(TAG, "Successfully Geofencing Connected");
                                } else {
                                    Log.d(TAG, "Failed to add Geofencing " + status.getStatus());
                                }
                            }
                        });
            } catch (SecurityException e) {
                Log.d(TAG, e.getMessage());
            }
        }
        isMonitoring = true;
        invalidateOptionsMenu();
    }

    @NonNull
    private Geofence getGeofence() {
        LatLng latLng = Constants.AREA_LANDMARKS.get(Constants.GEOFENCE_ID);
        return new Geofence.Builder().setRequestId(Constants.GEOFENCE_ID)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(latLng.latitude, latLng.longitude, Constants.GEOFENCE_RADIUS_IN_METERS)
                .setNotificationResponsiveness(1000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (pendingIntent != null) {
            return pendingIntent;
        }
        Intent intent = new Intent(this, GeofenceRegistrationService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    private void stopGeoFencing() {
        pendingIntent = getGeofencePendingIntent();
        LocationServices.GeofencingApi.removeGeofences(googleApiClient, pendingIntent)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) Log.d(TAG, "Stop geofencing");
                        else Log.d(TAG, "Not stop geofencing");
                    }
                });
        isMonitoring = false;
        rl_seekbar.setVisibility(View.VISIBLE);
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int response = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(MainActivity.this);
        if (response != ConnectionResult.SUCCESS) {
            Log.d(TAG, "Google Play Service Not Available");
            GoogleApiAvailability.getInstance()
                    .getErrorDialog(MainActivity.this, response, 1)
                    .show();
        } else {
            Log.d(TAG, "Google play service available");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.reconnect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manu_map_activity, menu);
        if (isMonitoring) {
            menu.findItem(R.id.action_stop_monitor).setVisible(true);
        } else {
            menu.findItem(R.id.action_stop_monitor).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.login:
                stopGeoFencing();
                Intent intent = new Intent(MainActivity.this, SplashActivity.class);
                startActivity(intent);
                break;
            case R.id.action_stop_monitor:
                stopGeoFencing();
                break;
            case R.id.action_share_updates:
                startShareUpdatesFlow();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        this.googleMap = googleMap;
        googleMap.setMyLocationEnabled(true);
        googleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                Constants.AREA_LANDMARKS.put(Constants.GEOFENCE_ID, new LatLng(location.getLatitude(), location
                        .getLongitude()));
                googleMap.addMarker(new MarkerOptions().position(latLng).title("TACME"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));
                googleMap.getUiSettings().setZoomControlsEnabled(true);

                circle = googleMap.addCircle(new CircleOptions().center(new LatLng(latLng.latitude, latLng.longitude))
                        .radius(Constants.GEOFENCE_RADIUS_IN_METERS)
                        .strokeColor(Color.RED)
                        .strokeWidth(4f));
                googleMap.setOnMyLocationChangeListener(null);
            }
        });


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Google Api Client Connected");

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Google Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        isMonitoring = false;
        Log.e(TAG, "Connection Failed:" + connectionResult.getErrorMessage());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                FirebaseDb.INSTANCE.insertUser(user);
                Toast.makeText(this, "Welcome " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                showShareSheet();
            } else {
                Toast.makeText(this, "Something went wrong, try again", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showShareSheet() {
        MainApplication mainApplication = (MainApplication) getApplication();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDb.INSTANCE.insertInstanceId(user, mainApplication.deviceId);

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, Constants.DEEPLINK + mainApplication.getDeviceId());
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    private void startShareUpdatesFlow() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Log.d(TAG, "User: " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            showShareSheet();
        } else {
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.GoogleBuilder().build(),
                    new AuthUI.IdpConfig.FacebookBuilder().build());

            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);
        }
    }
}