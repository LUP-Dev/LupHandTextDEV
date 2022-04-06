package com.example.lupworkmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

//import androidx.work.impl.utils.ForceStopRunnable;

public class Boot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Intent i = new Intent(context, Inicio.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

    }
}

//EN PROCESO ....