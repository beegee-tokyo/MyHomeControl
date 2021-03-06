package tk.giesecke.myhomecontrol.devices;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.github.druk.dnssd.BrowseListener;
import com.github.druk.dnssd.DNSSD;
import com.github.druk.dnssd.DNSSDBindable;
import com.github.druk.dnssd.DNSSDException;
import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.QueryListener;
import com.github.druk.dnssd.ResolveListener;
import com.github.druk.rxdnssd.BonjourService;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Map;

import tk.giesecke.myhomecontrol.BuildConfig;

public class CheckAvailDevices extends Service {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG  = "MHC-NSD";

	/** Service type for Arduino devices */
	private static final String SERVICE_TYPE2 = "_arduino._tcp.";
	/** Service type for other devices */
	private static final String SERVICE_TYPE = "_http._tcp.";
	/* Available services:
	 _workstation           _UnoWiFi            _udisks-ssh
	 _airplay               _raop               _xbmc-events
	 _xbmc-jsonrpc          _xbmc-jsonrpc-h     _http
	 _sftp-ssh              _ssh                _arduino
	 */

	/** Address of Diskstation for still CCTV pictures */
	public static String dsURL;

	/** Countdown timer to stop the discovery after some time */
	private CountDownTimer timer = null;

	//	/** RxDnssd bindable */
//	private RxDnssd rxDnssd;
//	/** Service browser subscription */
//	private Subscription browseSubscription;

	/** mDNS / NSD browser service */
	private DNSSDService browseService;
	/** mDNS / NSD bindable */
	private DNSSD dnssd;
	/** List of found services */
	private static final String[] dnssdServicesNames = new String[100];
	/** Number of found services */
	private static int dnssdFoundServices;

	/** Handler for looper to resolve the found services */
	private Handler mHandler;
	/** Looper for resolving the found services */
	private Runnable queryLooper;

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public int onStartCommand( Intent intent, int flags, int startId ) {
		Bundle extras = intent.getExtras();
		/* Flag if we are searching for none Arduino type of services */
		boolean searchAlternative = extras != null;

		mHandler = new Handler(Looper.getMainLooper());

		dnssd = new DNSSDBindable(this);
		for (int i=0; i<100; i++) {
			dnssdServicesNames[i] = "";
		}
		dnssdFoundServices = 0;
		try {
			String discoverService;
			if (searchAlternative) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start discovery services for " + SERVICE_TYPE2);
				discoverService = SERVICE_TYPE2;
			} else {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start discovery services for " + SERVICE_TYPE);
				discoverService = SERVICE_TYPE;
			}
			browseService = dnssd.browse(discoverService, new BrowseListener() {
				@Override
				public void serviceFound(DNSSDService browser, int flags, int ifIndex,
																 final String serviceName, final String regType, final String domain) {
					try {
						dnssd.resolve(flags, ifIndex, serviceName, regType, domain, new ResolveListener() {
							@Override
							public void serviceResolved(DNSSDService resolver, int flags, int ifIndex,
																					String fullName, final String hostName, final int port,
																					final Map<String, String> txtRecord) {
								try {
									QueryListener listener = new QueryListener() {
										@Override
										public void queryAnswered(DNSSDService query, final int flags, final int ifIndex,
																							final String fullName, int resolveClass, int resolveType,
																							final InetAddress address, int ttl) {
											if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Query address " + fullName);
											queryLooper = new Runnable() {
												@Override
												public void run() {
													BonjourService.Builder builder =
																	new BonjourService.Builder(flags, ifIndex, serviceName,
																					regType, domain).dnsRecords(txtRecord).port(port).hostname(hostName);
													if (address instanceof Inet4Address) {
														builder.inet4Address((Inet4Address) address);
													} else if (address instanceof Inet6Address) {
														return; // No need to search for IPv6 devices
													}
													if (BuildConfig.DEBUG) {
														Log.d(DEBUG_LOG_TAG+"FOUND", "\nhostName " + hostName +
																		"\nfullName " + fullName +
																		"\nport " + port +
																		"\naddress " + address +
																		"\ntxtRecords " + txtRecord.size() +
																		"\nserviceName " + serviceName);
													}

													if (serviceName.equalsIgnoreCase("DiskStation")) {
														dsURL = address.toString();
														if (dsURL.startsWith("/")) dsURL = dsURL.substring(1);
													}
												}
											};
											mHandler.post(queryLooper);
										}

										@Override
										public void operationFailed(DNSSDService service, int errorCode) {

										}
									};
									dnssd.queryRecord(0, ifIndex, hostName, 1, 1, listener);
									dnssd.queryRecord(0, ifIndex, hostName, 28, 1, listener);
								} catch (DNSSDException e) {
									e.printStackTrace();
								}
							}

							@Override
							public void operationFailed(DNSSDService service, int errorCode) {

							}
						});
					} catch (DNSSDException e) {
						e.printStackTrace();
					}

					if (dnssdFoundServices != 0) { // If NOT first discovery check if we know the service already
						for (int i=0; i<dnssdFoundServices; i++) {
							if (dnssdServicesNames[i].equalsIgnoreCase(serviceName)) {
								return;
							}
						}
					}
					dnssdFoundServices++;
					dnssdServicesNames[dnssdFoundServices-1] = serviceName;
				}

				@Override
				public void serviceLost(DNSSDService browser, int flags, int ifIndex,
																String serviceName, String regType, String domain) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Lost " + serviceName + " domain " + domain);
				}

				@Override
				public void operationFailed(DNSSDService service, int errorCode) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "error: " + errorCode);
				}
			});
		} catch (DNSSDException e) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "error", e);
			// Try to restart the search
			// Start discovery of mDNS/NSD services available if not running already
			startService(new Intent(getApplicationContext(), CheckAvailDevices.class));
		}

		// Start a countdown to stop the service after 60 seconds
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		timer = new CountDownTimer(60000, 30000) {
			public void onTick(long millisUntilFinished) {
				//Nothing here!
			}

			public void onFinish() {
				mHandler.removeCallbacksAndMessages(null);
				mHandler.removeCallbacks(null);
				browseService.stop();
				timer.cancel();
				timer = null;
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "CheckAvailDevices - Discovery finished!");
				stopSelf();
			}
		};
		timer.start();

		return super.onStartCommand( intent, flags, startId );
	}
}
