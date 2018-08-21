package com.example.exercise1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent){
        String action=intent.getStringExtra("action");
        if(action.equals("action1")){

        }
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);
    }
}
