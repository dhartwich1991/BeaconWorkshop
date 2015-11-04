package com.devfest.manualbluetoothparsing;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.math.BigInteger;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private TextView distanceBeacon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        distanceBeacon = (TextView) findViewById(R.id.distance_beacon);
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        bluetoothLeScanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                //noinspection ConstantConditions
                byte[] data = result.getScanRecord()
                        .getServiceData(ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB"));

                if (data == null) {
                    return;
                }

                byte frameType = data[0];
                if (frameType != 0x00) {
                    return;
                }

                String namespace = new BigInteger(1, Arrays.copyOfRange(data, 2, 12)).toString(16);
                String instance = new BigInteger(1, Arrays.copyOfRange(data, 12, 18)).toString(16);

                //My Beacon info
                if (!(namespace + instance).equals("edd1ebeac04e5defa017" + "d7b4fb5665ad")) {
                    return;
                }

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
                Log.i("distance", String.format("%.2fm", distance));

                distanceBeacon.setText(String.format("%.2fm", distance));
            }
        });
    }
}
