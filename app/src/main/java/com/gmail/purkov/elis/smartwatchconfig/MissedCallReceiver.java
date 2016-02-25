package com.gmail.purkov.elis.smartwatchconfig;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MissedCallReceiver extends BroadcastReceiver {

    static boolean isRinging=false;
    static boolean isReceived=false;

    @Override
    public void onReceive(Context context, Intent intent) {

        // Get current phone state
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if(state==null)
            return;

        // Phone is ringing
        if(state.equals(TelephonyManager.EXTRA_STATE_RINGING)){
            isRinging =true;
        }

        // Phone is received
        if(state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
            isReceived=true;
        }

        Log.d("State", state);

        // phone is idle
        if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)){
            // detect missed call
            if(isRinging==true && isReceived==false){
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                if(sharedPreferences.getBoolean("callsNotifications", false)) {
                    Intent i = new Intent(context, BluetoothService.class);
                    i.putExtra("callsNotifications", true);
                    context.startService(i);
                }
            }
        }
    }

}
