package com.devfest.manualbluetoothparsing;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.math.BigInteger;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    public static final double TEMPERATURE_THRESHOLD_COLD = 18.5;
    public static final double TEMPERATURE_THRESHOLD_HOT = 19.5;
    private TextView distanceBeacon;
    private TextView temperatureBeacon;
    private TextView temperatureTrend;
    private RelativeLayout mainLayout;
    private double lastMeasuredTemperature = 0.00;
    private double lastMeasuredDistance = 0.00;
    private BluetoothLeScanner bluetoothLeScanner;

    private ScanCallback callback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        distanceBeacon = (TextView) findViewById(R.id.distance_beacon);
        temperatureBeacon = (TextView) findViewById(R.id.beacon_temperature);
        temperatureTrend = (TextView) findViewById(R.id.temperature_trend);
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        callback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                //noinspection ConstantConditions
                byte[] data = result.getScanRecord()
                        .getServiceData(ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB"));

                if (data == null) {
                    return;
                }

                //Get the FrameType to check which data is available
                byte frameType = data[0];
                //Log.d("FrameType: ", frameType + "");
                if (frameType == 0x00) {

                    String namespace = new BigInteger(1, Arrays.copyOfRange(data, 2, 12)).toString(16);
                    String instance = new BigInteger(1, Arrays.copyOfRange(data, 12, 18)).toString(16);

                    //Log.d("namespace instance", namespace + " " + instance);
                    //My Beacon info
                    if ((namespace + instance).equals("edd1ebeac04e5defa017" + "d7b4fb5665ad")) {

                        //Signal Strength
                        int rssi = result.getRssi();

                        //Signal strength emitted by the beacon in dBm at 0 meters distance
                        int txPower = data[1];

                        //pathLoss = (txPower at 0m - rssi)
                        double distance = Math.pow(10, ((txPower - rssi) - 41) / 20.0);

                        //because rssi is unstable, usually only proximity zones are used:
                        // immediate (very close to the beacon)
                        // near (about 1-3m away from the beacon)
                        // far (further away or the signal is fluctuating too much to make a better calculation)
                        // unknown
                        Log.i("distance", String.format("%.2f m", distance));

                        distanceBeacon.setText(String.format("%.2f m", distance));
                        lastMeasuredDistance = distance;
                    }
                } else if (frameType == 0x20) {
                    //Get the temperature for my beacon
                    if (result.getDevice().getAddress().equals("D7:B4:FB:56:65:AD")) {
                        double temperature = new BigInteger(Arrays.copyOfRange(data, 4, 6)).intValue() / 256.00;

                        //Calculate the trend of the temperature
                        if (lastMeasuredTemperature < temperature) {
                            temperatureTrend.setText(R.string.trend_warmer);
                        } else if (lastMeasuredTemperature > temperature) {
                            temperatureTrend.setText(R.string.trend_colder);
                        } else {
                            temperatureTrend.setText(R.string.trend_no_difference);
                        }
                        lastMeasuredTemperature = temperature;

                        Log.d("Temperature", String.format("%.2f°C", temperature));
                        temperatureBeacon.setText(String.format("%.2f°C", temperature));

                        //Set the background color according to the temperature
                        if (temperature <= TEMPERATURE_THRESHOLD_COLD) {
                            mainLayout.setBackgroundColor(
                                    ContextCompat.getColor(MainActivity.this, android.R.color.holo_blue_light));
                        } else if (temperature >= TEMPERATURE_THRESHOLD_HOT) {
                            mainLayout.setBackgroundColor(
                                    ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_light));
                        } else {
                            mainLayout.setBackgroundColor(
                                    ContextCompat.getColor(MainActivity.this, android.R.color.holo_green_light));
                        }
                    }
                }
            }
        };
        bluetoothLeScanner.startScan(callback);
    }

    @Override
    protected void onDestroy() {
        bluetoothLeScanner.stopScan(callback);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble("TEMP", lastMeasuredTemperature);
        outState.putDouble("DIST", lastMeasuredDistance);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            lastMeasuredDistance = savedInstanceState.getDouble("DIST");
            lastMeasuredTemperature = savedInstanceState.getDouble("TEMP");

            if (lastMeasuredTemperature != 0) {
                //Set the background color according to the temperature
                if (lastMeasuredTemperature <= TEMPERATURE_THRESHOLD_COLD) {
                    mainLayout.setBackgroundColor(
                            ContextCompat.getColor(MainActivity.this, android.R.color.holo_blue_light));
                } else if (lastMeasuredTemperature >= TEMPERATURE_THRESHOLD_HOT) {
                    mainLayout.setBackgroundColor(
                            ContextCompat.getColor(MainActivity.this, android.R.color.holo_red_light));
                } else {
                    mainLayout.setBackgroundColor(
                            ContextCompat.getColor(MainActivity.this, android.R.color.holo_green_light));
                }
                temperatureBeacon.setText(String.format("%.2f°C", lastMeasuredTemperature));
            }
            if (lastMeasuredDistance != 0) {
                distanceBeacon.setText(String.format("%.2f m", lastMeasuredDistance));
            }
        }
    }
}
