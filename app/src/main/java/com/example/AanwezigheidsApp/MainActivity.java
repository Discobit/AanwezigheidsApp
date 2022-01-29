package com.example.AanwezigheidsApp;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ArmaRssiFilter;
import org.altbeacon.beacon.service.RunningAverageRssiFilter;

/**
 * Created by dyoung on 12/13/13.
 * Modification Luc Quaedvlieg - deleted unused code/comments and added comments
 */
public class MainActivity extends Application implements MonitorNotifier {
    private static final String TAG = "AanwezigheidsApp";
    public static final Region wildcardRegion = new Region("wildcardRegion", null, null, null);
    public static boolean insideRegion = false;

    public void onCreate() {
        super.onCreate();
        BeaconManager beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);
        // Filter to more accurately range the beacon when the user is moving
        BeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter.class);
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(RunningAverageRssiFilter.DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS/4);

        beaconManager.setDebug(true);

        Log.d(TAG, "setting up background monitoring in app onCreate");
        beaconManager.addMonitorNotifier(this);

        // If we were monitoring *different* regions on the last run of this app, they will be
        // remembered.  In this case we need to disable them here
        for (Region region: beaconManager.getMonitoredRegions()) {
            beaconManager.stopMonitoring(region);
        }

        beaconManager.startMonitoring(wildcardRegion);
    }

    @Override
    public void didEnterRegion(Region arg0) {
        Log.d(TAG, "did enter region.");
        insideRegion = true;
        // Send a notification to the user whenever a Beacon
        // matching a Region (defined above) are first seen.
        Log.d(TAG, "Sending notification.");
        sendNotification();
    }

    @Override
    public void didExitRegion(Region region) {
        insideRegion = false;
        // do nothing here. logging happens in MonitoringActivity
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        // do nothing here. logging happens in MonitoringActivity
    }

    private void sendNotification() {
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Beacon Reference Notifications",
                    "Beacon Reference Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
            builder = new Notification.Builder(this, channel.getId());
        }
        else {
            builder = new Notification.Builder(this);
            builder.setPriority(Notification.PRIORITY_HIGH);
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MonitoringActivity.class));
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentTitle("I detect a beacon");
        builder.setContentText("Tap here to see details in the reference app");
        builder.setContentIntent(resultPendingIntent);
        notificationManager.notify(1, builder.build());
    }
}
