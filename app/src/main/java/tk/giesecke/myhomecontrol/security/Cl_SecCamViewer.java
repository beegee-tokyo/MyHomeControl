package tk.giesecke.myhomecontrol.security;

import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;

import tk.giesecke.myhomecontrol.R;

public class Cl_SecCamViewer extends AppCompatActivity {

	/** Flag for viewing mode */
	private boolean multiWindow;
	/** Number of video to show */
	private int selectedWindow;
	/** Video selection button */
	private ImageButton switchCam;
	/** WebView for video 1 */
	private WebView cam1WebView;
	/** WebView for video 2 */
	private WebView cam2WebView;

	// URLs for mjpg-streamer
	private String camIP;
	private String camPort1;
	private String camPort2;
	@SuppressWarnings("FieldCanBeLocal")
	private String urlCam1;
	@SuppressWarnings("FieldCanBeLocal")
	private String urlCam2;

	public Cl_SecCamViewer() {
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

		// URLs for mjpg-streamer
		camIP = getString(R.string.SECURITY_CAM_FRONT);
		camPort1 = getString(R.string.SECURITY_CAM_FRONT_PORT1);
		camPort2 = getString(R.string.SECURITY_CAM_FRONT_PORT2);
		urlCam1 = "http://" + camIP + ":" + camPort1 + "/?action=stream";
//	urlCam1 = "http://192.168.1.5:8080/?action=stream";
		urlCam2 = "http://" + camIP + ":" + camPort2 + "/?action=stream";
//	urlCam2 = "http://192.168.1.5:8081/?action=stream";

		multiWindow = false;
		selectedWindow = 0;

		// Find the views in view_video.xml layout
		cam1WebView = (WebView) findViewById(R.id.wv_seccam1);
		cam2WebView = (WebView) findViewById(R.id.wv_seccam2);
		ImageButton camRefresh = (ImageButton) findViewById((R.id.ib_refresh));
		ImageButton closeView = (ImageButton) findViewById((R.id.ib_close_view));
		final ImageButton viewSelect = (ImageButton) findViewById((R.id.ib_change_view));
		switchCam = (ImageButton) findViewById((R.id.ib_switch_video));

		cam1WebView.getSettings().setJavaScriptEnabled(true);
		cam1WebView.getSettings().setLoadWithOverviewMode(true);
		cam1WebView.getSettings().setUseWideViewPort(true);
		cam1WebView.setWebViewClient(new WebViewClient());
		cam1WebView.setWebChromeClient(new WebChromeClient());
		cam1WebView.getSettings().setAppCacheEnabled(false);

		cam1WebView.setWebViewClient(new WebViewClient() {
		});

		cam2WebView.getSettings().setJavaScriptEnabled(true);
		cam2WebView.getSettings().setLoadWithOverviewMode(true);
		cam2WebView.getSettings().setUseWideViewPort(true);
		cam2WebView.setWebViewClient(new WebViewClient());
		cam2WebView.setWebChromeClient(new WebChromeClient());
		cam2WebView.getSettings().setAppCacheEnabled(false);

		cam2WebView.setWebViewClient(new WebViewClient() {
		});

		handleWebViews();

		camRefresh.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				handleWebViews();
			}
		});
		switchCam.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectedWindow = (selectedWindow==0) ? 1 : 0;
				handleWebViews();
			}
		});
		closeView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		switchCam.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectedWindow = (selectedWindow==0) ? 1 : 0;
				handleWebViews();
			}
		});
		viewSelect.setOnClickListener(new View.OnClickListener() {
			@SuppressWarnings("deprecation")
			@Override
			public void onClick(View v) {
				if (multiWindow) {
					multiWindow = false;
					viewSelect.setImageDrawable(getResources().getDrawable(R.drawable.ic_multi_view));
					switchCam.setVisibility(View.VISIBLE);
				} else {
					multiWindow = true;
					viewSelect.setImageDrawable(getResources().getDrawable(R.drawable.ic_single_view));
					switchCam.setVisibility(View.GONE);
				}
				handleWebViews();
			}
		});
	}

	@SuppressWarnings("deprecation")
	private void handleWebViews() {
		/* Pointer to Image button */
		ImageButton viewSelect = (ImageButton) findViewById((R.id.ib_change_view));
		if (multiWindow) {
			cam1WebView.setVisibility(View.VISIBLE);
			cam2WebView.setVisibility(View.VISIBLE);
			cam1WebView.loadUrl(urlCam1);
			cam2WebView.loadUrl(urlCam2);
			viewSelect.setImageDrawable(getResources().getDrawable(R.drawable.ic_single_view));
			switchCam.setVisibility(View.GONE);
		} else {
			viewSelect.setImageDrawable(getResources().getDrawable(R.drawable.ic_multi_view));
			if (selectedWindow == 0) {
				cam1WebView.setVisibility(View.VISIBLE);
				cam2WebView.setVisibility(View.GONE);
				cam1WebView.loadUrl(urlCam1);
			} else {
				cam1WebView.setVisibility(View.GONE);
				cam2WebView.setVisibility(View.VISIBLE);
				cam2WebView.loadUrl(urlCam2);
			}
			switchCam.setVisibility(View.VISIBLE);
		}
	}
}
