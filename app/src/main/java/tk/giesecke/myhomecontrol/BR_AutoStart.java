package tk.giesecke.myhomecontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BR_AutoStart extends BroadcastReceiver {
	@Override
	public void onReceive(Context thisAppContext, Intent thisAppIntent) {

		thisAppIntent.getAction();
		// Start background services
		thisAppContext.startService(new Intent(thisAppContext, Sv_StartBackgroundServices.class));
	}
}
