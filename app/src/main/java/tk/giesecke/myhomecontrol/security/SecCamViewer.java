package tk.giesecke.myhomecontrol.security;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

import tk.giesecke.myhomecontrol.BuildConfig;
import tk.giesecke.myhomecontrol.R;

import static tk.giesecke.myhomecontrol.MyHomeControl.lists;
import static tk.giesecke.myhomecontrol.devices.CheckAvailDevices.dsURL;

public class SecCamViewer extends AppCompatActivity {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-CCTV";

	/** This activities context */
	private Context seccamContext;

	/** VideoView for CCTV footage */
	private VideoView footageVV;
	/** Menu item for status message */
	private MenuItem statusMenuItem;
	/** Layout for status message */
	private RelativeLayout statusMenuLayout;
	/** Text view for status message */
	private TextView statusMenuText;

	/** Media controller for CCTV footage */
	private MediaController footageMC;
	/** URL of footage to be played */
	private String urlFootage = "";

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sec_video);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		int statusBarColor = getResources().getColor(android.R.color.holo_orange_dark);
		int actionBarColor = getResources().getColor(android.R.color.holo_orange_light);
		// Color of toolBar background
		Drawable toolBarDrawable;

		if (android.os.Build.VERSION.SDK_INT >= 21) {
			getWindow().setStatusBarColor(statusBarColor);
		}
		if (android.os.Build.VERSION.SDK_INT >= 16) {
			toolBarDrawable = new ColorDrawable(actionBarColor);
			toolbar.setBackground(toolBarDrawable);
		}

		// Initialize variables
		seccamContext = this;
		/** Error text view */
		/* Error text view */
		TextView errorMsg = (TextView) findViewById(R.id.et_cctv_error);
		/** VideoView for CCTV footage */
		footageVV = (VideoView) findViewById(R.id.vv_cctv_footage);
		footageVV.setVisibility(View.INVISIBLE);

		// Check if there are any stored CCTV footage
		if (!lists.commError.equalsIgnoreCase("")) {
			errorMsg.setText(lists.commError);
		} else {
			errorMsg.setVisibility(View.INVISIBLE);
		}

		// Done, wait for user to select a stored CCTV footage to play
	}

	@Override
	public void onResume() {
		if (!urlFootage.equalsIgnoreCase("")) {
			startVideoStream();
		}
		super.onResume();
	}

	@Override
	public void onPause() {
		if (footageVV.isPlaying()) {
			stopVideoStream();
		}
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_seccam, menu);
		/* Action menu */
		statusMenuItem = menu.findItem(R.id.action_status);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.action_today:
				// If playback is running, stop it first
				stopVideoStream();
				// Show list with todays CCTV footage
				CharSequence[] todaysListAlert = lists.todaysList.toArray(new CharSequence[lists.todaysList.size()]);
				AlertDialog.Builder todaysListBuilder = new AlertDialog.Builder(this);
				todaysListBuilder.setTitle("Select footage");
				todaysListBuilder.setItems(todaysListAlert, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						urlFootage = lists.todaysList.get(which);
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Selected day = " + urlFootage);
						// "http://" + dsURL + ":8080/getdir.php";
						urlFootage = "http://" + dsURL + ":8080/MHCV/" + urlFootage;
						startVideoStream();
						dialog.dismiss();
					}
				});
				todaysListBuilder.create();
				todaysListBuilder.show();
				break;
			case R.id.action_history:
				// If playback is running, stop it first
				stopVideoStream();
				// Show list with days available
				CharSequence[] availDaysListAlert = lists.availDaysList.toArray(new CharSequence[lists.availDaysList.size()]);
				AlertDialog.Builder availDaysBuilder = new AlertDialog.Builder(this);
				availDaysBuilder.setTitle("Select day");
				availDaysBuilder.setItems(availDaysListAlert, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						// Show list with selected days CCTV footage
						final int daySelected = which;
						CharSequence[] thisDayListAlert = lists.daysList.get(which).toArray(new CharSequence[lists.daysList.get(which).size()]);
						AlertDialog.Builder thisDayListBuilder = new AlertDialog.Builder(seccamContext);
						thisDayListBuilder.setTitle("Select command");
						thisDayListBuilder.setItems(thisDayListAlert, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								urlFootage = lists.daysList.get(daySelected).get(which);
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Selected day = " + urlFootage);
								urlFootage = "http://" + dsURL + ":8080/MHCV/"
										+ lists.availDaysList.get(daySelected)
										+ "/"
										+ urlFootage;
								startVideoStream();
								dialog.dismiss();
							}
						});
						thisDayListBuilder.create();
						thisDayListBuilder.show();
					}
				});
				availDaysBuilder.create();
				availDaysBuilder.show();
				break;
			case R.id.action_close:
				// If playback is running, stop it first
				stopVideoStream();
				// Go back to main app
				this.finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void startVideoStream() {
		Uri vidUri = Uri.parse(urlFootage);
		footageVV.setVisibility(View.VISIBLE);
		// Start the video stream
		footageVV.setVideoURI(vidUri);
		footageVV.setHardwareDecoder(true);
		footageVV.start();
		statusMenuLayout = (RelativeLayout) statusMenuItem.getActionView();
		statusMenuText = (TextView) statusMenuLayout.findViewById(R.id.tv_status);
		statusMenuText.setText(getString(R.string.buffer_msg));

		footageVV.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mediaPlayer) {
				footageMC = new MediaController(seccamContext) {
					@Override
					public void hide() {
						//Do not hide.
					}
				};
				footageVV.setMediaController(footageMC);
				footageMC.setAnchorView(footageVV);
				footageMC.show();
				footageMC.requestFocus();
				mediaPlayer.setVideoQuality(MediaPlayer.VIDEOQUALITY_LOW);
				mediaPlayer.setPlaybackSpeed(1.0f);
				mediaPlayer.setBufferSize(2000);
				mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
					@Override
					public boolean onInfo(MediaPlayer mp, int what, int extra) {
						switch (what) {
							case MediaPlayer.MEDIA_INFO_BUFFERING_START:
								statusMenuItem.setActionView(R.layout.vi_seccam_buffer);
								statusMenuLayout = (RelativeLayout) statusMenuItem.getActionView();
								statusMenuText = (TextView) statusMenuLayout.findViewById(R.id.tv_status);
								statusMenuText.setText(getString(R.string.buffer_msg));
								break;
							case MediaPlayer.MEDIA_INFO_BUFFERING_END:
								statusMenuItem.setActionView(R.layout.vi_seccam_buffer);
								statusMenuLayout = (RelativeLayout) statusMenuItem.getActionView();
								statusMenuText = (TextView) statusMenuLayout.findViewById(R.id.tv_status);
								statusMenuText.setText("");
								break;
						}
						return false;
					}
				});
			}
		});
	}

	private void stopVideoStream() {
		if (footageVV.isPlaying()) {
			footageVV.stopPlayback();
		}
		footageVV.setVisibility(View.INVISIBLE);
		statusMenuItem.setActionView(R.layout.vi_seccam_buffer);
		statusMenuLayout = (RelativeLayout) statusMenuItem.getActionView();
		statusMenuText = (TextView) statusMenuLayout.findViewById(R.id.tv_status);
		statusMenuText.setText(getString(R.string.cctv_footage_info));
	}
}
