package tk.giesecke.myhomecontrol;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoStart extends BroadcastReceiver {
//	public AutoStart() {
//	}

	@SuppressLint("UnsafeProtectedBroadcastReceiver")
	@Override
	public void onReceive(Context context, Intent intent) {

		// Start background services
		context.startService(new Intent(context, StartBackgroundServices.class));
	}
}
