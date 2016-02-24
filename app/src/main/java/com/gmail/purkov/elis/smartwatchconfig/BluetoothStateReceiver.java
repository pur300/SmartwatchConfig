package com.gmail.purkov.elis.smartwatchconfig;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

        switch (btState) {
            case BluetoothAdapter.STATE_ON:
                context.startService(new Intent(context, BluetoothService.class));
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                context.stopService(new Intent(context, BluetoothService.class));
                break;
        }
    }
}
