package tk.giesecke.myhomecontrol.devices;

import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;

import tk.giesecke.myhomecontrol.BuildConfig;

public class NSDHelper {

	private final Context mContext;

	private final NsdManager mNsdManager;
	private NsdManager.DiscoveryListener mDiscoveryListener;

//	private static final String SERVICE_TYPE = "_arduino._tcp."; //"_services._dns-sd._udp"; "_http._tcp";
	private static final String SERVICE_TYPE = "_arduino._tcp."; //"_services._dns-sd._udp"; "_http._tcp";
	private static final String SERVICE_TYPE2 = "_http._tcp."; //"_services._dns-sd._udp"; "_http._tcp";
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
	private static final String DEBUG_LOG_TAG = "MHC-NSD";

	static final ArrayList<NsdServiceInfo> mServices = new ArrayList<>();
	public static final String[] mServicesNames = new String[100];
	public static final InetAddress[] mServicesHosts = new InetAddress[100];
	static final int[] mServicesPort = new int[100];
	public static int foundServices;

	NSDHelper(Context context) {
		mContext = context;
		mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
	}

	void initializeDiscoveryListener() {
		mDiscoveryListener = new NsdManager.DiscoveryListener() {

			@Override
			public void onDiscoveryStarted(String regType) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Discovery service started");
				mServices.clear();
				for (int i=0; i<100; i++) {
					mServicesNames[i] = "";
					mServicesHosts[i] = null;
					mServicesPort[i] = 0;
				}
			}

			@Override
			public void onServiceFound(NsdServiceInfo service) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Service discovery found: " + service);
				String newServiceName = service.getServiceName();
				if (newServiceName.contains("[")) {
					int startOfIP6 = newServiceName.indexOf("[");
					if (startOfIP6 != -1) {
						newServiceName = newServiceName.substring(0,startOfIP6-1);
					}
				}
				if (foundServices != 0) { // If NOT first discovery check if we know the service already
					for (int i=0; i<foundServices; i++) {
						if (mServicesNames[i].equalsIgnoreCase(newServiceName)) {
							return;
						}
					}
				}

				foundServices++;
				mServices.add(service);
				mServicesNames[foundServices-1] = newServiceName;
			}

			@Override
			public void onServiceLost(NsdServiceInfo service) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Discovery service lost");
				// Nothing to do here
			}

			@Override
			public void onDiscoveryStopped(String serviceType) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Discovery stopped: " + serviceType);
			}

			@Override
			public void onStartDiscoveryFailed(String serviceType, int errorCode) {
				if (BuildConfig.DEBUG) Log.e(DEBUG_LOG_TAG, "Discovery failed: Error code: " + errorCode);
//				mNsdManager.stopServiceDiscovery(this);
				sendMyBroadcast(99, false); // Send error
			}

			@Override
			public void onStopDiscoveryFailed(String serviceType, int errorCode) {
				if (BuildConfig.DEBUG) Log.e(DEBUG_LOG_TAG, "Discovery failed: Error code: " + errorCode);
				mNsdManager.stopServiceDiscovery(this);
				sendMyBroadcast(99, false); // Send error
			}
		};
	}

	void discoverServices() {
		foundServices = 0;
		if (CheckAvailDevices.searchAlternative) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start discovery services for " + SERVICE_TYPE2);
			mNsdManager.discoverServices(
					SERVICE_TYPE2, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
		} else {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start discovery services for " + SERVICE_TYPE);
			mNsdManager.discoverServices(
					SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
		}
	}

	void resolveService(NsdServiceInfo service) {
		mNsdManager.resolveService(service, new NsdManager.ResolveListener() {

			@Override
			public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
				for (int i=0; i<foundServices; i++) {
					if (mServices.get(i) == serviceInfo) {
						Log.e(DEBUG_LOG_TAG, "Resolve failed: " + errorCode
								+ " service: " + mServicesNames[i]
								+ " info: "  + serviceInfo);
						sendMyBroadcast(i, false);
					}
				}
			}

			@Override
			public void onServiceResolved(NsdServiceInfo serviceInfo) {
				Log.d(DEBUG_LOG_TAG, "Resolve Succeeded: " + serviceInfo);

				Log.d(DEBUG_LOG_TAG, "Service name: " + serviceInfo.getServiceName()
						+ " IP: " + serviceInfo.getHost() + ":" + serviceInfo.getPort());

				for (int i=0; i<foundServices; i++) {
					String newServiceName = serviceInfo.getServiceName();
					int startOfIP6 = newServiceName.indexOf("[");
					if (startOfIP6 != -1) {
						newServiceName = newServiceName.substring(0,startOfIP6-1);
					}
					if (mServicesNames[i].equalsIgnoreCase(newServiceName)) {
						InetAddress ipV4;
						try {
							InetAddress hostInet= InetAddress.getByName(serviceInfo.getHost().getHostAddress());
							byte [] addressBytes = hostInet.getAddress();
							ipV4 = Inet4Address.getByAddress(addressBytes);
						} catch (UnknownHostException e) {
							Log.d(DEBUG_LOG_TAG, "Error getting IP V4 address: " + e.getMessage());
							ipV4 = serviceInfo.getHost();
						}
//						mServicesHosts[i] = serviceInfo.getHost();
						mServicesHosts[i] = ipV4;
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
		Log.d(DEBUG_LOG_TAG, "Discovery stopped");
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
		broadCastIntent.setAction(CheckAvailDevices.NSD_RESOLVED);
		broadCastIntent.putExtra("from", "NSD");
		broadCastIntent.putExtra("service", serviceNum);
		broadCastIntent.putExtra("resolved", resolveSuccess);
		mContext.sendBroadcast(broadCastIntent);
	}
}
