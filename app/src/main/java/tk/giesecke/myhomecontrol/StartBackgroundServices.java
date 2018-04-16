package tk.giesecke.myhomecontrol;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import tk.giesecke.myhomecontrol.devices.CheckAvailDevices;
import tk.giesecke.myhomecontrol.devices.MessageListener;
import tk.giesecke.myhomecontrol.solar.SolarSyncDataBase;

public class StartBackgroundServices extends Service {
//	public StartBackgroundServices() {
//	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// Start service to listen to UDP & MQTT broadcast messages
		startService(new Intent(this, MessageListener.class));

		// Start discovery of mDNS/NSD services available if not running already
		startService(new Intent(this, CheckAvailDevices.class));

		/** Pending intent for sync every 2 hours */
		PendingIntent pi = PendingIntent.getService(this, 5002,
				new Intent(this, SolarSyncDataBase.class), PendingIntent.FLAG_UPDATE_CURRENT);
		/** Alarm manager for sync every 2 hours */
		AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		if (am != null) {
			am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + 3600000,
							7200000, pi);
		}
		stopSelf();
	}
}
