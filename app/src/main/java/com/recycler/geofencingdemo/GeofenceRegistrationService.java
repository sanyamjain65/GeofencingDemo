package com.recycler.geofencingdemo;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

public class GeofenceRegistrationService extends IntentService {

    private static final String TAG = "GeoIntentService";
    private CountDownTimer countDownTimer;

    public GeofenceRegistrationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.d(TAG, "GeofencingEvent error " + geofencingEvent.getErrorCode());
        } else {
            int transaction = geofencingEvent.getGeofenceTransition();
            List<Geofence> geofences = geofencingEvent.getTriggeringGeofences();
            Geofence geofence = geofences.get(0);
            if (transaction == Geofence.GEOFENCE_TRANSITION_ENTER && geofence.getRequestId()
                    .equals(Constants.GEOFENCE_ID)) {
                Log.d(TAG, "You are inside Tacme");
            } else {
                Log.d(TAG, "You are outside Tacme");
            }
            String geofenceTransitionDetails = getGeofenceTrasitionDetails(transaction, geofences);

            if (!geofenceTransitionDetails.isEmpty()) {
                sendNotification(geofenceTransitionDetails);
                startVibrator();
            }
        }
    }

    private void startVibrator() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(3000, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(3000);
        }
    }


    // Create a detail message with Geofences received
    private String getGeofenceTrasitionDetails(
            int geoFenceTransition, List<Geofence> triggeringGeofences) {
        // get the ID of each geofence triggered
        ArrayList<String> triggeringGeofencesList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesList.add(geofence.getRequestId());
        }

        String status = null;
        if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }
            return "";
        } else if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            status = "Exiting ";
            countDownTimer = new CountDownTimer(10000, 1000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "onFinish: Countdown timer has finished");
                    countDownTimer.cancel();
                    countDownTimer = null;
                }
            };
        }
        return status + TextUtils.join(", ", triggeringGeofencesList);
    }

    // Send a notification
    private void sendNotification(String msg) {
        Log.i(TAG, "sendNotification: " + msg);

        // Intent to start the main Activity
        Intent notificationIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager notificatioMng = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder notificationBuilder = createNotification(msg, notificationPendingIntent);
        int notifyID = 1;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "lakshman_rekha";// The id of the channel.
            CharSequence name = "lakshman_rekha";// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationBuilder.setChannelId(CHANNEL_ID);
            notificatioMng.createNotificationChannel(mChannel);
        }
        notificatioMng.notify(notifyID, notificationBuilder.build());

        // Creating and sending Notification
    }

    // Create a notification
    private NotificationCompat.Builder createNotification(
            String msg, PendingIntent notificationPendingIntent) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher_round)
                .setColor(Color.RED)
                .setContentTitle(msg)
                .setContentText("Geofence Notification!")
                .setContentIntent(notificationPendingIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setAutoCancel(true);
        return notificationBuilder;
    }

    // Handle errors
    private static String getErrorString(int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "GeoFence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many GeoFences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending intents";
            default:
                return "Unknown error.";

        }
    }
}
