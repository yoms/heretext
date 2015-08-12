package com.orlkuk.dropit.receiver;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.orlkuk.dropit.R;
import com.orlkuk.dropit.hmi.ContactListActivity;
import com.orlkuk.dropit.model.Common;
import com.orlkuk.dropit.model.DataProvider;

/**
 * @author appsrox.com
 *
 */
public class GcmBroadcastReceiver extends BroadcastReceiver {
	
	private static final String TAG = "GcmBroadcastReceiver";
	
	private Context ctx;	

	@Override
	public void onReceive(Context context, Intent intent) {
		ctx = context;
		
		PowerManager mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		WakeLock mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		mWakeLock.acquire();
		
		try {
			GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
			
			String messageType = gcm.getMessageType(intent);
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
				sendNotification("Send error", false);
				
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				sendNotification("Deleted messages on server", false);
				
			} else {
                Bundle extras = intent.getExtras();
                String msg = extras.getString(DataProvider.COL_MSG);
                String email = extras.getString(DataProvider.COL_FROM);
                double lat = Double.parseDouble(extras.getString(DataProvider.COL_LAT));
                double lon = Double.parseDouble(extras.getString(DataProvider.COL_LON));
				
				ContentValues values = new ContentValues(2);
				values.put(DataProvider.COL_MSG, msg);
                values.put(DataProvider.COL_FROM, email);
                values.put(DataProvider.COL_LAT, lat);
                values.put(DataProvider.COL_LON, lon);
				context.getContentResolver().insert(DataProvider.CONTENT_URI_MESSAGES, values);

                addProximityAlert(lat, lon);
			}
			setResultCode(Activity.RESULT_OK);
			
		} finally {
			mWakeLock.release();
		}
	}
    private void addProximityAlert(double latitude, double longitude) {

        Intent intent = new Intent(Common.PROX_ALERT_INTENT);
        PendingIntent proximityIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0);

        LocationManager locationManager = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
        locationManager.addProximityAlert(
                latitude, // the latitude of the central point of the alert region
                longitude, // the longitude of the central point of the alert region
                Common.POINT_RADIUS, // the radius of the central point of the alert region, in meters
                Common.PROX_ALERT_EXPIRATION, // time for this proximity alert, in milliseconds, or -1 to indicate no expiration
                proximityIntent // will be used to generate an Intent to fire when entry to or exit from the alert region is detected
        );

        IntentFilter filter = new IntentFilter(Common.PROX_ALERT_INTENT);
        ctx.getApplicationContext().registerReceiver(new LocationReceiver(), filter);

    }

	private void sendNotification(String text, boolean launchApp) {
		NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		
		Notification.Builder mBuilder = new Notification.Builder(ctx)
			.setAutoCancel(true)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle(ctx.getString(R.string.app_name))
			.setContentText(text);

		if (!TextUtils.isEmpty(Common.getRingtone())) {
			mBuilder.setSound(Uri.parse(Common.getRingtone()));
		}
		
		if (launchApp) {
			Intent intent = new Intent(ctx, ContactListActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder.setContentIntent(pi);
		}
		
		mNotificationManager.notify(1, mBuilder.getNotification());
	}
}
