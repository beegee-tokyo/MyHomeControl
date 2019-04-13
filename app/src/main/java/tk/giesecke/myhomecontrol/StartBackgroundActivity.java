package tk.giesecke.myhomecontrol;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

public class StartBackgroundActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("BOOTRECEIVER", "StartBackgroundActivity was called");
        startService(new Intent(this, StartBackgroundServices.class));
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.i("BOOTRECEIVER", "StartBackgroundActivity killing itself");
                finish();
            }
        });
        finish();
    }
}
