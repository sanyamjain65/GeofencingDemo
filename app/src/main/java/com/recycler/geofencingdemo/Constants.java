package com.recycler.geofencingdemo;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

/**
 * Created by Manu on 11/24/2017.
 */

public class Constants {

    //Location
    public static final String GEOFENCE_ID = "Lakshman Rekha";
    public static float GEOFENCE_RADIUS_IN_METERS = 50;
    public static double latitude = 28.6139;
    public static double longitude = 77.2090;

    /**
     * Map for storing information about tacme in the dubai.
     */
    public static final HashMap<String, LatLng> AREA_LANDMARKS = new HashMap<String, LatLng>();

    static {
        // Tacme
        AREA_LANDMARKS.put(GEOFENCE_ID, new LatLng(latitude, longitude));
    }
}