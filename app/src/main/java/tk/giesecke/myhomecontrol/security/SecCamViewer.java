package tk.giesecke.myhomecontrol.security;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import tk.giesecke.myhomecontrol.BuildConfig;
import tk.giesecke.myhomecontrol.MyHomeControl;
import tk.giesecke.myhomecontrol.R;

import static android.R.id.list;
import static tk.giesecke.myhomecontrol.MyHomeControl.lists;

public class SecCamViewer extends AppCompatActivity implements View.OnClickListener{

	/** Debug tag */
	static final String DEBUG_LOG_TAG = "MHC-CCTV";

	/** Video selection button */
	private ImageButton showSelector;
	/** Error text view */
	private TextView errorMessage;
	/** Layout with selector buttons */
	private LinearLayout selectorButtonsFrame;
	/** Button to show todays footage */
	private Button todaysFootage;
	/** Button to show history footage */
	private Button historyFootage;
	/** VideoView for CCTV footage */
	private VideoView camVideoView;

	/** URL of footage to be played */
	private String urlFootage;
	/** Flag if video is playing */
	private boolean isPlaying;

	public SecCamViewer() {
		super();
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("SetJavaScriptEnabled")
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
		showSelector = (ImageButton) findViewById(R.id.ib_view_selection);
		showSelector.setOnClickListener(this);
		/** Error text view */
		errorMessage = (TextView) findViewById(R.id.et_cctv_error);
		/** Layout with selector buttons */
		selectorButtonsFrame = (LinearLayout) findViewById(R.id.ll_select_footage);
		/** Button to show todays footage */
		todaysFootage = (Button) findViewById(R.id.bt_today);
		todaysFootage.setOnClickListener(this);
		/** Button to show todays footage */
		historyFootage = (Button) findViewById(R.id.bt_history);
		historyFootage.setOnClickListener(this);
		/** VideoView for CCTV footage */
		camVideoView = (VideoView) findViewById(R.id.vv_cctv_footage);

		camVideoView.setVisibility(View.INVISIBLE);

		// Check if there are any stored CCTV footage
		if (!lists.commError.equalsIgnoreCase("")) {
			showSelector.setVisibility(View.INVISIBLE);
			selectorButtonsFrame.setVisibility(View.INVISIBLE);
			errorMessage.setText(lists.commError);
			errorMessage.setVisibility(View.VISIBLE);
		} else {
			showSelector.setVisibility(View.VISIBLE);
			selectorButtonsFrame.setVisibility(View.VISIBLE);
			errorMessage.setVisibility(View.INVISIBLE);
		}

		// Done, wait for user to select a stored CCTV footage to play
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) { // Handle buttons
			case R.id.ib_view_selection:
				if (selectorButtonsFrame.getVisibility() == View.INVISIBLE) {
					selectorButtonsFrame.setVisibility(View.VISIBLE);
					if (isPlaying) {
						stopVideoStream();
					}
				} else {
					selectorButtonsFrame.setVisibility(View.INVISIBLE);
				}
				break;
			case R.id.bt_today:
				// Show list with todays CCTV footage
				CharSequence[] todaysListAlert = lists.todaysList.toArray(new CharSequence[lists.todaysList.size()]);
				AlertDialog.Builder cmdListBuilder = new AlertDialog.Builder(this);
				cmdListBuilder.setTitle("Select command");
				cmdListBuilder.setItems(todaysListAlert, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						urlFootage = lists.todaysList.get(which);
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Selected day = " + urlFootage);
						urlFootage = "192.168.0.252:8080/MHCV/" + urlFootage;
						startVideoStream();
						dialog.dismiss();
					}
				});
				cmdListBuilder.create();
				cmdListBuilder.show();
				break;
			case R.id.bt_history:
				// Show list with days available
				break;
		}
	}

	public void startVideoStream() {
		Uri vidUri = Uri.parse(urlFootage);
		camVideoView.setVisibility(View.VISIBLE);
		selectorButtonsFrame.setVisibility(View.INVISIBLE);
		camVideoView.setVideoURI(vidUri);
		camVideoView.start();

		isPlaying = true;
	}

	public void stopVideoStream() {
		camVideoView.stopPlayback();

		isPlaying = false;
	}
}
