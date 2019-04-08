package com.empatica.sample;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.EmpaticaDevice;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements EmpaDataDelegate, EmpaStatusDelegate {

    private static final int REQUEST_ENABLE_BT = 1;

    private static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;


    private static final String EMPATICA_API_KEY = "62e322cb9dac410e9041afc08d977669"; // TODO insert your API Key here


    private EmpaDeviceManager deviceManager = null;

    private TextView accel_xLabel;

    private TextView accel_yLabel;

    private TextView accel_zLabel;

    private TextView bvpLabel;

    private TextView edaLabel;

    private TextView ibiLabel;

    private TextView rmssdLabel;

    private TextView sdnnLabel;

    private TextView HRLabel;

    private TextView temperatureLabel;

    private TextView batteryLabel;

    private TextView statusLabel;

    private TextView deviceNameLabel;

    private LinearLayout dataCnt;


    ///// added for calculations

    //private int ibiCount = 0;
    ArrayList<ArrayList> ibiList = new ArrayList();
    //ArrayList<Float> ibiList = new ArrayList();

    private double rmssd = 0;
    private double sdnn = 0;
    private float HR = 0;
    private double ibiAvg = 0;
    private int rmssd_y = 0;
    private int sdnn_y = 0;
    private boolean isCalm = false;
    private double timeStart = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Initialize vars that reference UI components
        statusLabel = (TextView) findViewById(R.id.status);

        dataCnt = (LinearLayout) findViewById(R.id.dataArea);

        accel_xLabel = (TextView) findViewById(R.id.accel_x);

        accel_yLabel = (TextView) findViewById(R.id.accel_y);

        accel_zLabel = (TextView) findViewById(R.id.accel_z);

        bvpLabel = (TextView) findViewById(R.id.bvp);

        edaLabel = (TextView) findViewById(R.id.eda);

        ibiLabel = (TextView) findViewById(R.id.ibi);

        rmssdLabel = (TextView) findViewById(R.id.rmssd);

        sdnnLabel = (TextView) findViewById(R.id.sdnn);

        HRLabel = (TextView) findViewById(R.id.HR);

        temperatureLabel = (TextView) findViewById(R.id.temperature);

        batteryLabel = (TextView) findViewById(R.id.battery);

        deviceNameLabel = (TextView) findViewById(R.id.deviceName);


        final Button disconnectButton = findViewById(R.id.disconnectButton);

        disconnectButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (deviceManager != null) {

                    deviceManager.disconnect();
                }
            }
        });

        initEmpaticaDeviceManager();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_ACCESS_COARSE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted, yay!
                    initEmpaticaDeviceManager();
                } else {
                    // Permission denied, boo!
                    final boolean needRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                    new AlertDialog.Builder(this)
                            .setTitle("Permission required")
                            .setMessage("Without this permission bluetooth low energy devices cannot be found, allow it in order to connect to the device.")
                            .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // try again
                                    if (needRationale) {
                                        // the "never ask again" flash is not set, try again with permission request
                                        initEmpaticaDeviceManager();
                                    } else {
                                        // the "never ask again" flag is set so the permission requests is disabled, try open app settings to enable the permission
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivity(intent);
                                    }
                                }
                            })
                            .setNegativeButton("Exit application", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // without permission exit is the only way
                                    finish();
                                }
                            })
                            .show();
                }
                break;
        }
    }

    private void initEmpaticaDeviceManager() {
        // Android 6 (API level 23) now require ACCESS_COARSE_LOCATION permission to use BLE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);
        } else {

            if (TextUtils.isEmpty(EMPATICA_API_KEY)) {
                new AlertDialog.Builder(this)
                        .setTitle("Warning")
                        .setMessage("Please insert your API KEY")
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // without permission exit is the only way
                                finish();
                            }
                        })
                        .show();
                return;
            }

            // Create a new EmpaDeviceManager. MainActivity is both its data and status delegate.
            deviceManager = new EmpaDeviceManager(getApplicationContext(), this, this);

            // Initialize the Device Manager using your API key. You need to have Internet access at this point.
            deviceManager.authenticateWithAPIKey(EMPATICA_API_KEY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (deviceManager != null) {
            deviceManager.stopScanning();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deviceManager != null) {
            deviceManager.cleanUp();
        }
    }

    @Override
    public void didDiscoverDevice(EmpaticaDevice bluetoothDevice, String deviceName, int rssi, boolean allowed) {
        // Check if the discovered device can be used with your API key. If allowed is always false,
        // the device is not linked with your API key. Please check your developer area at
        // https://www.empatica.com/connect/developer.php
        if (allowed) {
            // Stop scanning. The first allowed device will do.
            deviceManager.stopScanning();
            try {
                // Connect to the device
                deviceManager.connectDevice(bluetoothDevice);
                updateLabel(deviceNameLabel, "To: " + deviceName);
            } catch (ConnectionNotAllowedException e) {
                // This should happen only if you try to connect when allowed == false.
                Toast.makeText(MainActivity.this, "Sorry, you can't connect to this device", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void didRequestEnableBluetooth() {
        // Request the user to enable Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The user chose not to enable Bluetooth
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            // You should deal with this
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void didUpdateSensorStatus(@EmpaSensorStatus int status, EmpaSensorType type) {

        didUpdateOnWristStatus(status);
    }

    @Override
    public void didUpdateStatus(EmpaStatus status) {
        // Update the UI
        updateLabel(statusLabel, status.name());

        // The device manager is ready for use
        if (status == EmpaStatus.READY) {
            updateLabel(statusLabel, status.name() + " - Turn on your device");
            // Start scanning
            deviceManager.startScanning();
            // The device manager has established a connection

            hide();

        } else if (status == EmpaStatus.CONNECTED) {

            //add the timer stuff here
            show();




            // The device manager disconnected from a device
        } else if (status == EmpaStatus.DISCONNECTED) {

            updateLabel(deviceNameLabel, "");

            hide();
        }
    }

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        updateLabel(accel_xLabel, "" + x);
        updateLabel(accel_yLabel, "" + y);
        updateLabel(accel_zLabel, "" + z);
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        updateLabel(bvpLabel, "" + bvp);
    }

    @Override
    public void didReceiveBatteryLevel(float battery, double timestamp) {
        updateLabel(batteryLabel, String.format("%.0f %%", battery * 100));
    }

    @Override
    public void didReceiveGSR(float gsr, double timestamp) {
        updateLabel(edaLabel, "" + gsr);
    }


    //// Edit ibi here
    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        updateLabel(ibiLabel, "" + ibi);

        ArrayList timeibiList = new ArrayList();
        timeibiList.add(timestamp);
        timeibiList.add(ibi);
        ibiList.add(timeibiList);  // ((timestamp1, ibi1),(timestamp2, ibi2),...)

        //System.out.println("ibiList=" + ibiList + " ibisize=" + ibiList.size());


        ////////// To calculate RMSSD
        //// 60 IBI data points, irregardless of time

        if (ibiList.size() > 61){
            //System.out.println((Float)ibiList.get(0).get(1) - (Float)ibiList.get(1).get(1));
            double x = 0;

            for (int i = rmssd_y; i < ibiList.size()-1; i++) {
                x += Math.pow(((Float)ibiList.get(i).get(1) - (Float)ibiList.get(i+1).get(1)), 2);
            }
            rmssd_y++;
            //System.out.println(x);
            rmssd = Math.sqrt(x/(ibiList.size()-1-rmssd_y));
            System.out.println("RMSSD= " + rmssd*1000);
        }



        /*
        //// 30 seconds, irregardless of number of IBI data points
        if (ibiList.size() > 61){
            //System.out.println((Float)ibiList.get(0).get(1) - (Float)ibiList.get(1).get(1));
            //System.out.println((Double)ibiList.get(0).get(0) - (Float)ibiList.get(1).get(0));

            double x = 0;
            int y = 0;

            timeStart = ((Double)ibiList.get(ibiList.size()-1).get(0)) - 30;
            //System.out.println(timeStart);

            for (int i = 0; i < ibiList.size()-1; i++) {
                if ((Double)ibiList.get(i).get(0) > timeStart) {
                    x += Math.pow(((Float) ibiList.get(i).get(1) - (Float) ibiList.get(i + 1).get(1)), 2);
                    y++;
                }
            }
            //System.out.println(x);
            //System.out.println(y);
            rmssd = Math.sqrt(x/(y));
            System.out.println("RMSSD= " + rmssd*1000);
        }
        */


        updateLabel(rmssdLabel, "" + rmssd*1000);

        if (rmssd > 70){
            isCalm = true;
        }
        else {
            isCalm = false;
        }
        //System.out.println("isCalm=" + isCalm);
        ////////////////////////////

        ////////// Calculate SDNN

        if (ibiList.size() > 61){
            //System.out.println((Float)ibiList.get(0).get(1) - (Float)ibiList.get(1).get(1));
            double z = 0;
            double ibiSum = 0;

            for (int i = sdnn_y; i < ibiList.size()-1; i++) {
                ibiSum += (Float)ibiList.get(i).get(1);
            }
            ibiAvg = ibiSum/(ibiList.size()-1-sdnn_y);

            for (int i = sdnn_y; i < ibiList.size()-1; i++) {
                z += Math.pow(((Float)ibiList.get(i).get(1) - ibiAvg), 2);
            }
            sdnn_y++;
            //System.out.println(x);
            sdnn = Math.sqrt(z/(ibiList.size()-1-sdnn_y));
            System.out.println("SDNN=" + sdnn*1000);
        }

        updateLabel(sdnnLabel, "" + sdnn*1000);


        ////
        //System.out.println("ibi=" + ibi);
        //System.out.print("timestamp=" + timestamp + ", "); // seconds
        //ibiCount = ibiCount + 1;
        //System.out.print("ibiCount=" +ibiCount);
        HR = 60/ibi;
        System.out.println("HR=" + HR);
        updateLabel(HRLabel, "" + HR);

    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        updateLabel(temperatureLabel, "" + temp);
    }

    // Update a label with some text, making sure this is run in the UI thread
    private void updateLabel(final TextView label, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                label.setText(text);
            }
        });
    }

    @Override
    public void didReceiveTag(double timestamp) {

    }

    @Override
    public void didEstablishConnection() {

        show();
    }

    @Override
    public void didUpdateOnWristStatus(@EmpaSensorStatus final int status) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (status == EmpaSensorStatus.ON_WRIST) {

                    ((TextView) findViewById(R.id.wrist_status_label)).setText("ON WRIST");
                }
                else {

                    ((TextView) findViewById(R.id.wrist_status_label)).setText("NOT ON WRIST");
                }
            }
        });
    }

    void show() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                dataCnt.setVisibility(View.VISIBLE);
            }
        });
    }

    void hide() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                dataCnt.setVisibility(View.INVISIBLE);
            }
        });
    }
}
