package com.orlkuk.dropit.receiver;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.orlkuk.dropit.R;
import com.orlkuk.dropit.hmi.ContactListActivity;
import com.orlkuk.dropit.model.Common;

public class LocationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String key = LocationManager.KEY_PROXIMITY_ENTERING;

        Boolean entering = intent.getBooleanExtra(key, false);

        if (entering) {
            Log.d(getClass().getSimpleName(), "entering");
        }
        else {
            Log.d(getClass().getSimpleName(), "exiting");
        }

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder mBuilder = new Notification.Builder(context)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("You are near a message.");
        if (!TextUtils.isEmpty(Common.getRingtone())) {
            mBuilder.setSound(Uri.parse(Common.getRingtone()));
        }

        Intent launchAppIntent = new Intent(context, ContactListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 0, launchAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);

        mNotificationManager.notify(1, mBuilder.getNotification());

    }
}