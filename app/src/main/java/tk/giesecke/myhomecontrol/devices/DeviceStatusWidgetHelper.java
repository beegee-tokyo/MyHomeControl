package tk.giesecke.myhomecontrol.devices;

import android.app.IntentService;
import android.content.Intent;

import tk.giesecke.myhomecontrol.MyHomeControl;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class DeviceStatusWidgetHelper extends IntentService {

	public DeviceStatusWidgetHelper() {
		super("DeviceStatusWidgetHelper");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			Intent appIntent = new Intent(this, MyHomeControl.class);
			appIntent.putExtra("vID",MyHomeControl.view_devDebug_id);
			appIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
			startActivity(appIntent);
		}
	}
}
