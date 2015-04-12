package geonote.app.Receiver;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import geonote.app.Services.LocationListenerService;

public class AlarmReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "Received");
        context.startService(new Intent(context, LocationListenerService.class));
    }
}