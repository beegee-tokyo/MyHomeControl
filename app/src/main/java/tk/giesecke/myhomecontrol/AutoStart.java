package tk.giesecke.myhomecontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoStart extends BroadcastReceiver {
	@Override
	public void onReceive(Context thisAppContext, Intent thisAppIntent) {

	    Log.i("BOOTRECEIVER","Bootreceiver was called");
		thisAppIntent.getAction();
		// Start background services
		Intent startIntent = new Intent(thisAppContext, StartBackgroundActivity.class);
		startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		thisAppContext.startActivity(startIntent);
	}
}
