package com.example.AanwezigheidsApp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Modifications Luc Quaedvlieg - added comments, added ranging functions and API requests
 */
public class RangingActivity extends Activity {
    protected static final String TAG = "RangingActivity";
    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
    public Boolean presence = false;
    public String presenceString = "";
    public String currentBeacon = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranging);
    }

    @Override
    protected void onResume() {
        super.onResume();
        RangeNotifier rangeNotifier = new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Log.d(TAG, "didRangeBeaconsInRegion called with beacon count:  "+beacons.size());
                    Beacon firstBeacon = beacons.iterator().next();

                    DecimalFormat df = new DecimalFormat("#.##");

                    // Converts the Major of the beacon to the floor of the building
                    int major = firstBeacon.getId2().toInt();
                    String convertedMajor = Integer.toHexString(major);
                    convertedMajor = convertedMajor.substring(0, convertedMajor.length()-2);

                    // Displays the closest beacon id with the range and the floor + room number
                    logToDisplay("The closest beacon " + firstBeacon.getId1().toString() +
                            " is about " + df.format(firstBeacon.getDistance()) +
                            " meters away." + "\n" + convertedMajor + "." + firstBeacon.getId3());

                    currentBeacon = firstBeacon.getId1().toString();

                    if (firstBeacon.getDistance() >= 2.0)  {
                        presenceDisplay("Afwezig");
                        if (presenceString.equals("aanwezig") || presenceString.equals("")) {
                            presence = false;
                            presenceString = "afwezig";
                            sendPost("186e14cb-87dc-4e96-b0f9-f5fd4cbc56ac", firstBeacon.getId1().toString(), presence);
                        }
                    }
                    if (firstBeacon.getDistance() <= 2.0) {
                        presenceDisplay("Aanwezig");
                        if (presenceString.equals("afwezig") || presenceString.equals("")) {
                            presence = true;
                            presenceString = "aanwezig";
                            sendPost("186e14cb-87dc-4e96-b0f9-f5fd4cbc56ac", firstBeacon.getId1().toString(), presence);
                        }
                    }
                }
            }
        };
        beaconManager.addRangeNotifier(rangeNotifier);
        beaconManager.startRangingBeacons(MainActivity.wildcardRegion);
    }


    @Override
    protected void onPause() {
        super.onPause();
        beaconManager.stopRangingBeacons(MainActivity.wildcardRegion);
        beaconManager.removeAllRangeNotifiers();
        if (presenceString.equals("aanwezig")){
            sendPost("186e14cb-87dc-4e96-b0f9-f5fd4cbc56ac", currentBeacon, false);
        }
    }

    // Displays the beacon information and distance, replaces the text
    private void logToDisplay(final String distanceLine) {
        runOnUiThread(new Runnable() {
            public void run() {
                EditText editText = (EditText)RangingActivity.this.findViewById(R.id.rangingText);
                editText.setText(distanceLine);
            }
        });
    }

    // Displays the presence, changes between aanwezig (present) and afwezig (absent)
    private void presenceDisplay(final String presenceLine) {
        runOnUiThread(new Runnable() {
            public void run() {
                TextView presence = (TextView)RangingActivity.this.findViewById(R.id.textView_aangemeld_afgemeld);
                presence.setText(presenceLine);
            }
        });
    }

    // sends a request to the API to give the application id, beacon id and status
    public void sendPost(String appid, String beaconid, Boolean presence) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept","application/json");
                    conn.setRequestProperty("Connection", "keep-alive");
                    conn.setRequestProperty("automaticPost", "true");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("appid", appid);
                    jsonParam.put("beaconid", beaconid);
                    jsonParam.put("presence", presence);

                    Log.i("JSON", jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG" , conn.getResponseMessage());

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }
}
