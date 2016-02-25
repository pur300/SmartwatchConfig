package com.gmail.purkov.elis.smartwatchconfig;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.UUID;


public class BluetoothService extends Service {

    private static final String SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private static final String NEW_SMS_NOTIF_MESSAGE = "N_SMS\n";
    private static final String MISSED_CALL_NOTIF_MESSAGE = "N_M_CALL\n";
    private static final String NEW_EMAIL_NOTIF_MESSAGE = "N_EMAIL\n";

    private boolean smsNotifEnabled;
    private boolean callsNotifEnabled;
    private boolean emailNotifEnabled;

    private String deviceMac;

    private SharedPreferences sharedPreferences;
    private Bundle intentBundle;
    private BluetoothAdapter btAdapter;
    private BluetoothGatt btGatt;
    private BluetoothGattCallback btGattCallback;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("ServiceLog", "Service has been started!");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Save values from sharedPreferences into variables
        smsNotifEnabled = sharedPreferences.getBoolean("smsNotifications", false);
        callsNotifEnabled = sharedPreferences.getBoolean("callsNotifications", false);
        emailNotifEnabled = sharedPreferences.getBoolean("emailNotifications", false);
        deviceMac = sharedPreferences.getString("deviceMac", "");

        //Bluetooth Gatt callback
        btGattCallback = new BluetoothGattCallback() {
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Get service which matches SERVICE_UUID. We don't need to perform check for null value, because we can select only bluetooth devices, which support SERVICE_UUID
                    BluetoothGattService btGattService = btGatt.getService(UUID.fromString(SERVICE_UUID));
                    BluetoothGattCharacteristic btCharasteristic = btGattService.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));
                    // We have to check if a given service has a characteristic UUID equals CHARACTERISTIC_UUID
                    if (btCharasteristic != null) {
                        // Start reliable write
                        btGatt.beginReliableWrite();
                        if (smsNotifEnabled && intentBundle.getBoolean("smsNotifications", false)) {
                            btCharasteristic.setValue(NEW_SMS_NOTIF_MESSAGE.getBytes());
                        } else if (callsNotifEnabled && intentBundle.getBoolean("callsNotifications", false)) {
                            btCharasteristic.setValue(MISSED_CALL_NOTIF_MESSAGE.getBytes());
                        } else if (emailNotifEnabled && intentBundle.getBoolean("emailNotifications", false)) {
                            btCharasteristic.setValue(NEW_EMAIL_NOTIF_MESSAGE.getBytes());
                        }

                        btGatt.writeCharacteristic(btCharasteristic);
                    }
                }

            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("ServiceLog", "Connected!");
                    // Run service discovery
                    btGatt.discoverServices();
                } else
                    // Finish service
                    stopSelf();

            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);

                if (status == BluetoothGatt.GATT_SUCCESS)
                    btGatt.executeReliableWrite();
                else {
                    btGatt.abortReliableWrite();
                    // Finish service
                    stopSelf();
                }
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS)
                    Log.d("ServiceLog", "Value has been successfully written!");
                // Finish service
                stopSelf();
            }

        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // Get bundle from intent
        intentBundle = intent.getExtras();

        // Create bluetooth adapter if necessary
        if (btAdapter == null)
            btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check if adapter is enabled
        if (btAdapter.isEnabled()) {

            if (btAdapter.checkBluetoothAddress(deviceMac))
                btGatt = btAdapter.getRemoteDevice(deviceMac).connectGatt(this, true, btGattCallback);
            else
                // Finish service
                stopSelf();

        }
        else
            // Finish service
            stopSelf();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {

        // Close Gatt
        if (btGatt != null) {
            btGatt.disconnect();
            btGatt.close();
        }
        Log.d("ServiceLog", "Service has been stopped!");

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
