package tk.giesecke.myhomecontrol.devices;

import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;

import tk.giesecke.myhomecontrol.BuildConfig;

public class Cl_NsdHelper {

	private final Context mContext;

	private final NsdManager mNsdManager;
	private NsdManager.DiscoveryListener mDiscoveryListener;

//	private static final String SERVICE_TYPE = "_arduino._tcp."; //"_services._dns-sd._udp"; "_http._tcp";
	private static final String SERVICE_TYPE = "_arduino._tcp."; //"_services._dns-sd._udp"; "_http._tcp";
/** Available services:
	_workstation
	_UnoWiFi
    _udisks-ssh
	_airplay
	_raop
	_xbmc-events
	_xbmc-jsonrpc
	_xbmc-jsonrpc-h
	_http
	_sftp-ssh
	_ssh
	_arduino
 */
	private static final String TAG = "MHC-NSD";

	static final ArrayList<NsdServiceInfo> mServices = new ArrayList<>();
	public static final String[] mServicesNames = new String[100];
	public static final InetAddress[] mServicesHosts = new InetAddress[100];
	static final int[] mServicesPort = new int[100];
	static int foundServices = 0;

	Cl_NsdHelper(Context context) {
		mContext = context;
		mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
	}

	void initializeDiscoveryListener() {
		mDiscoveryListener = new NsdManager.DiscoveryListener() {

			@Override
			public void onDiscoveryStarted(String regType) {
				// Nothing to do here
			}

			@Override
			public void onServiceFound(NsdServiceInfo service) {
				if (BuildConfig.DEBUG) Log.d(TAG, "Service discovery found: " + service);
				if (foundServices != 0) { // If NOT first discovery check if we know the service already
					for (int i=0; i<foundServices; i++) {
						if (mServicesNames[i].equalsIgnoreCase(service.getServiceName())) {
							return;
						}
					}
				}

				foundServices++;
				mServices.add(service);
				mServicesNames[foundServices-1] = service.getServiceName();
			}

			@Override
			public void onServiceLost(NsdServiceInfo service) {
				// Nothing to do here
			}

			@Override
			public void onDiscoveryStopped(String serviceType) {
				if (BuildConfig.DEBUG) Log.d(TAG, "Discovery stopped: " + serviceType);
			}

			@Override
			public void onStartDiscoveryFailed(String serviceType, int errorCode) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Discovery failed: Error code: " + errorCode);
				mNsdManager.stopServiceDiscovery(this);
				sendMyBroadcast(99, false); // Send error
			}

			@Override
			public void onStopDiscoveryFailed(String serviceType, int errorCode) {
				if (BuildConfig.DEBUG) Log.e(TAG, "Discovery failed: Error code: " + errorCode);
				mNsdManager.stopServiceDiscovery(this);
				sendMyBroadcast(99, false); // Send error
			}
		};
	}

	void discoverServices() {
		if (BuildConfig.DEBUG) Log.d(TAG, "Service discovery started");
		mNsdManager.discoverServices(
				SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
	}

	void resolveService(NsdServiceInfo service) {
		mNsdManager.resolveService(service, new NsdManager.ResolveListener() {

			@Override
			public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
				for (int i=0; i<foundServices; i++) {
					if (mServices.get(i) == serviceInfo) {
						Log.e(TAG, "Resolve failed: " + errorCode
								+ " service: " + mServicesNames[i]
								+ " info: "  + serviceInfo);
						sendMyBroadcast(i, false);
					}
				}
			}

			@Override
			public void onServiceResolved(NsdServiceInfo serviceInfo) {
				Log.d(TAG, "Resolve Succeeded: " + serviceInfo);

				Log.d(TAG, "Service name: " + serviceInfo.getServiceName()
						+ " IP: " + serviceInfo.getHost() + ":" + serviceInfo.getPort());
				for (int i=0; i<foundServices; i++) {
					if (mServicesNames[i].equalsIgnoreCase(serviceInfo.getServiceName())) {
						mServicesHosts[i] = serviceInfo.getHost();
						mServicesPort[i] = serviceInfo.getPort();
						sendMyBroadcast(i, true);
						break;
					}
				}
			}
		});
	}

	void stopDiscovery() {
		mNsdManager.stopServiceDiscovery(mDiscoveryListener);
	}

	/**
	 * Send received message to all listing threads
	 *
	 * @param serviceNum
	 *            Number of Service we found
	 */
	private void sendMyBroadcast(int serviceNum, boolean resolveSuccess) {
		/** Intent for activity internal broadcast message */
		Intent broadCastIntent = new Intent();
		broadCastIntent.setAction(Sv_CheckAvailDevices.NSD_RESOLVED);
		broadCastIntent.putExtra("from", "NSD");
		broadCastIntent.putExtra("service", serviceNum);
		broadCastIntent.putExtra("resolved", resolveSuccess);
		mContext.sendBroadcast(broadCastIntent);
	}
}
