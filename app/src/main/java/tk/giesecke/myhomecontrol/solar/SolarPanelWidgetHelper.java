package tk.giesecke.myhomecontrol.solar;

import android.app.IntentService;
import android.content.Intent;

import tk.giesecke.myhomecontrol.MyHomeControl;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class SolarPanelWidgetHelper extends IntentService {

	public SolarPanelWidgetHelper() {
		super("SolarPanelWidgetHelper");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			Intent appIntent = new Intent(this, MyHomeControl.class);
			appIntent.putExtra("vID",MyHomeControl.view_solar_id);
			appIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
			startActivity(appIntent);
		}
	}
}
