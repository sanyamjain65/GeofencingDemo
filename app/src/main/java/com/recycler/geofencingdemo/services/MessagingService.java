package com.recycler.geofencingdemo.services;


import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.recycler.geofencingdemo.utils.TinyDB;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "MessagingService";

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        TinyDB tinyDB = new TinyDB(this);
        tinyDB.putString("token", s);
        Log.d(TAG, s);
    }
}
