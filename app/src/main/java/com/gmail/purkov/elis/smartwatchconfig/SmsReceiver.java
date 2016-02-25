package com.gmail.purkov.elis.smartwatchconfig;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if(sharedPreferences.getBoolean("smsNotifications", false)){
            Intent i = new Intent(context, BluetoothService.class);
            i.putExtra("smsNotifications", true);
            context.startService(i);
        }
    }
}
