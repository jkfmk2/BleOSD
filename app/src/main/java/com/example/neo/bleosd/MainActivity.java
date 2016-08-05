package com.example.neo.bleosd;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    boolean mScan = false;
    private BluetoothAdapter mbleAdaptoer;
    private BluetoothDevice mbleDevice;
    private BluetoothGatt mbtGatt;
    private Handler mHandler;

    private TextView mConnectStatus;
    private TextView mReceivedData;

    private static final String DEVICE_NAME = "CC41-A";
    private static final UUID UUID_BLE_UART = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long BT_SCAN_PERIOD = 5000;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConnectStatus = (TextView) findViewById(R.id.connect_status);
        mReceivedData = (TextView) findViewById(R.id.received_data);

        mHandler = new Handler();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final BluetoothManager bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mbleAdaptoer = bleManager.getAdapter();

        if (mbleAdaptoer == null) {
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int item_id = item.getItemId();
        switch (item_id) {
            case R.id.menu_scan:
                scanBleDevice();
                break;
        }
        return true;
    }

    private void scanBleDevice() {
        if (!mScan) {
            mScan = true;
            mbleDevice = null;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScan = false;
                        if (mbleDevice == null) {
                            updateConnectStatusText(R.string.unconnected);
                        }
                    mbleAdaptoer.stopLeScan(mLeScanCallback);
                }
            }, BT_SCAN_PERIOD);

            updateConnectStatusText(R.string.scanning);
            mbleAdaptoer.startLeScan(mLeScanCallback);
        }
    }

    private void updateConnectStatusText(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectStatus.setText(resourceId);
            }
        });
    }

    private void updateReceivedDataText(final String string) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mReceivedData.setText(string);
            }
        });
    }

    private void connectBleDevice() {
        if (mbleDevice != null) {
            if (mbtGatt == null) {
                mbtGatt = mbleDevice.connectGatt(this, false, mGattCallback);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mbleAdaptoer.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && requestCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            if (DEVICE_NAME.equals(bluetoothDevice.getName())) {
                if (mbleDevice == null) {
                    mbleDevice = bluetoothDevice;
                    connectBleDevice();
                }
            }
        }
    };

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
                updateConnectStatusText(R.string.connected);
            }
            else{
                updateConnectStatusText(R.string.unconnected);
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //if (UUID_BLE_UART.equals(characteristic.getUuid())) {
            final byte [] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                updateReceivedDataText(stringBuilder.toString());
            }
            //}
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //if (UUID_BLE_UART.equals(characteristic.getUuid())) {
                final byte [] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    updateReceivedDataText(stringBuilder.toString());
                }
            //}
        }
    };
}
