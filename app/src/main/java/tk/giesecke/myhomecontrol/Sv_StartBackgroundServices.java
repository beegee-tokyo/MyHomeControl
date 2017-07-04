package tk.giesecke.myhomecontrol;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import tk.giesecke.myhomecontrol.devices.Sv_CheckAvailDevices;
import tk.giesecke.myhomecontrol.solar.Sv_SolarSyncDB;

public class Sv_StartBackgroundServices extends Service {
//	public Sv_StartBackgroundServices() {
//	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// Start service to listen to UDP & MQTT broadcast messages
		startService(new Intent(this, Sv_MessageListener.class));

		// Start discovery of mDNS/NSD services available if not running already
		startService(new Intent(this, Sv_CheckAvailDevices.class));

			/** Pending intent for sync every 2 hours */
		PendingIntent pi = PendingIntent.getService(this, 5002,
				new Intent(this, Sv_SolarSyncDB.class), PendingIntent.FLAG_UPDATE_CURRENT);
		/** Alarm manager for sync every 2 hours */
		AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3600000,
				7200000, pi);
		stopSelf();
	}
}
