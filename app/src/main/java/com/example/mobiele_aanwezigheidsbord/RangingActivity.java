package com.example.mobiele_aanwezigheidsbord;

import static com.example.mobiele_aanwezigheidsbord.BuildConfig.API_URL;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpResponse;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.HttpClient;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpPost;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.entity.StringEntity;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.HttpClientBuilder;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Collection;

public class RangingActivity extends Activity {
    protected static final String TAG = "RangingActivity";
    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
    public Boolean presence = false;
    public String presenceString = "";

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

                    logToDisplay("The first beacon " + firstBeacon.getId1().toString() + " is about " + df.format(firstBeacon.getDistance()) + " meters away.");

                    if (firstBeacon.getDistance() >= 2.0)  {
                        System.out.println("Groter dan 2");
                        presenceDisplay("Afwezig");
                        if (presenceString.equals("aanwezig") || presenceString.equals("")) {
                            System.out.println("stuur afwezig");
                            presence = false;
                            presenceString = "afwezig";
                            sendPost("53b67f99-1b08-4b17-a806-00dd34043716", firstBeacon.getId1().toString(), presence);
                            testDisplay("afwezig");
                        }
                    }
                    if (firstBeacon.getDistance() <= 2.0) {
                        System.out.println("kleiner dan 2");
                        presenceDisplay("Aanwezig");
                        if (presenceString.equals("afwezig") || presenceString.equals("")) {
                            System.out.println("stuur aanwezig");
                            presence = true;
                            presenceString = "aanwezig";
                            sendPost("53b67f99-1b08-4b17-a806-00dd34043716", firstBeacon.getId1().toString(), presence);
                            testDisplay("aanwezig");
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

    private void testDisplay(final String testPresence) {
        runOnUiThread(new Runnable() {
            public void run() {
                TextView presence = (TextView)RangingActivity.this.findViewById(R.id.textView_test_aanwezigheid);
                presence.setText(testPresence);
            }
        });
    }

    public void sendPost(String appid, String beaconid, Boolean presence) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_URL);
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
