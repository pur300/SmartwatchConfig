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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.UUID;


public class BluetoothService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private static final String CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";

    private boolean smsNotifEnabled;
    private boolean callsNotifEnabled;
    private boolean emailNotifEnabled;

    private boolean isDeviceSupported;
    private boolean isDeviceConnected;

    private SharedPreferences sharedPreferences;
    private BluetoothGatt btGatt;
    private BluetoothGattCallback btGattCallback;

    @Override
    public void onCreate(){
        super.onCreate();

        Log.d("ServiceLog", "Service has been started!");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Set event listener for shared preferences
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        //Bluetooth Gatt callback
        btGattCallback = new BluetoothGattCallback() {
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                if(status == BluetoothGatt.GATT_SUCCESS){
                    // Get service which matches SERVICE_UUID. We don't need to perform check for null value, because we can select only bluetooth devices, which support SERVICE_UUID
                    BluetoothGattService btGattService = btGatt.getService(UUID.fromString(SERVICE_UUID));
                    BluetoothGattCharacteristic btCharasteristic = btGattService.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));
                    // We have to check if a given service has a characteristic UUID equals CHARACTERISTIC_UUID
                    if(btCharasteristic != null){
                        isDeviceSupported = true;
                        // Set notification for newly received data
                        btGatt.setCharacteristicNotification(btCharasteristic, true);
                    }
                    else
                        isDeviceSupported = false;
                }
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                if(newState == BluetoothProfile.STATE_CONNECTED){
                    Log.d("ServiceLog", "Connected!");
                    isDeviceConnected = true;
                    // Run service discovery
                    btGatt.discoverServices();
                }
                else {
                    isDeviceConnected = false;
                    Log.d("ServiceLog", "Disconnected!");
                }

            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, final   BluetoothGattCharacteristic characteristic){
                String strBuffer = new String(characteristic.getValue());
                Log.d("ServiceLog", strBuffer);
                if(strBuffer.contains("\n") && strBuffer.contains("request=getClock")){
                    // Get clock and send it back to Smartwatch
                    Log.d("ServiceLog", "Send clock back!");
                }
            }

        };

        // Get BluetoothGatt object
        btGatt = getBtGatt(sharedPreferences.getString("deviceMac", ""));

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        // Close Gatt connection
        if(btGatt != null){
            btGatt.disconnect();
            btGatt.close();
        }
        Log.d("ServiceLog", "Service has been stopped!");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("ServiceLog", "Shared preferences have been changed! " + key);
        if(key.equals("smsNotifications"))
            smsNotifEnabled = sharedPreferences.getBoolean(key, false);
        else if(key.equals("callsNotifications"))
            callsNotifEnabled = sharedPreferences.getBoolean(key, false);
        else if(key.equals("emailNotifications"))
            emailNotifEnabled = sharedPreferences.getBoolean(key, false);
        else if(key.equals("deviceMac")){
            // Close Gatt connection
            if(btGatt != null){
                btGatt.disconnect();
                btGatt.close();
                // Create new Gatt connection
                btGatt = getBtGatt(sharedPreferences.getString("deviceMac", ""));
            }
        }
    }

    private BluetoothGatt getBtGatt(String mac){
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        if(btAdapter.isEnabled()){
            try {
                return btAdapter.getRemoteDevice(mac).connectGatt(this, true, btGattCallback);
            }
            catch (IllegalArgumentException e){}
        }

        return null;
    }

}
