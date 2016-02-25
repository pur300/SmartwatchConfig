package com.gmail.purkov.elis.smartwatchconfig;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;

public class SmartwatchConfiguration extends AppCompatActivity implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    private static final long SCAN_PERIOD = 1500;
    private static final String DEV_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private static final int REQUEST_ENABLE_BT = 1;

    private boolean btScanStarted;

    private Spinner btDeviceList;
    private ScanSettings scSettings;
    private ScanFilter scFilter;
    private List<ScanFilter> scFilters;
    private BluetoothLeScanner btScanner;
    private ScanCallback btScanCallback;
    private BluetoothAdapter btAdapter;
    private List<BluetoothDevice> btDevices;
    private ArrayAdapter<String> btDevicesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_smartwatch_configuration);

        // Get configuration data from shared preferences
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        ((Switch) findViewById(R.id.smsNotifSwitch)).setChecked(sharedPref.getBoolean("smsNotifications", false));
        ((Switch) findViewById(R.id.callsNotifSwitch)).setChecked(sharedPref.getBoolean("callsNotifications", false));
        ((Switch) findViewById(R.id.emailNotifSwitch)).setChecked(sharedPref.getBoolean("emailNotifications", false));

        // Set event listeners for switches
        ((Switch) findViewById(R.id.smsNotifSwitch)).setOnCheckedChangeListener(this);
        ((Switch) findViewById(R.id.callsNotifSwitch)).setOnCheckedChangeListener(this);
        ((Switch) findViewById(R.id.emailNotifSwitch)).setOnCheckedChangeListener(this);

        // Init variables
        btDeviceList = (Spinner) findViewById(R.id.btDevicesDropdown);
        btDevices = new ArrayList<BluetoothDevice>();
        scSettings = new ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build();
        scFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(DEV_UUID)).build();
        scFilters = new ArrayList<>(); scFilters.add(scFilter);
        btDevicesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Bluetooth LE scan callback
        btScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice btDevice = result.getDevice();
                if (btDevice.getBondState() == BluetoothDevice.BOND_BONDED && !btDevices.contains(btDevice)) {
                    btDevices.add(btDevice);
                    btDevicesAdapter.add(btDevice.getName());
                }
            }
        };

    }

    @Override
    public void onStart() {
        super.onStart();

        // Check if bluetooth is enabled
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
            btScanner = btAdapter.getBluetoothLeScanner();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (!btScanStarted) {
            btScanStarted = true;
            // Set event listener for spinner to null
            ((Spinner) findViewById(R.id.btDevicesDropdown)).setOnItemSelectedListener(null);
            // Clear list of all available bt devices
            btDevicesAdapter.clear();
            btDevices.clear();
            // Add initial text
            btDevicesAdapter.add("Select bluetooth device");
            btDeviceList.setAdapter(btDevicesAdapter);
            // Run scanning sequence for SCAN_PERIOD of time
            btScanner.startScan(scFilters, scSettings, btScanCallback);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    btScanner.stopScan(btScanCallback);
                    // Enable scan button
                    btDeviceList.setAdapter(btDevicesAdapter);
                    // Set event listener for spinner
                    ((Spinner) findViewById(R.id.btDevicesDropdown)).setOnItemSelectedListener(SmartwatchConfiguration.this);
                    btScanStarted = false;
                }
            }, SCAN_PERIOD);
        }

        return true;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            ((Switch) findViewById(R.id.smsNotifSwitch)).setChecked(savedInstanceState.getBoolean("smsNotifications"));
            ((Switch) findViewById(R.id.callsNotifSwitch)).setChecked(savedInstanceState.getBoolean("callsNotifications"));
            ((Switch) findViewById(R.id.emailNotifSwitch)).setChecked(savedInstanceState.getBoolean("emailNotifications"));

        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("smsNotifications", ((Switch) findViewById(R.id.smsNotifSwitch)).isChecked());
        savedInstanceState.putBoolean("callsNotifications", ((Switch) findViewById(R.id.callsNotifSwitch)).isChecked());
        savedInstanceState.putBoolean("emailNotifications", ((Switch) findViewById(R.id.emailNotifSwitch)).isChecked());

        super.onSaveInstanceState(savedInstanceState);

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        // Save changes into shared preferences
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        if (buttonView == findViewById(R.id.smsNotifSwitch))
            editor.putBoolean("smsNotifications", isChecked);
        else if (buttonView == findViewById(R.id.callsNotifSwitch))
            editor.putBoolean("callsNotifications", isChecked);
        else
            editor.putBoolean("emailNotifications", isChecked);

        editor.commit();

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(position > 0) {
            // Save changes into shared preferences
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("deviceMac", btDevices.get(position - 1).getAddress());

            editor.commit();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            finish();
        else
            btScanner = btAdapter.getBluetoothLeScanner();
    }

}
