package tk.giesecke.myhomecontrol;

/**
 * Wrapper to send several parameters to onPostExecute of AsyncTask
 */
class SolarCommResultWrapper {
	/** Returned result from spMonitor device */
	String taskResult;// Result of communication
	/** Month that should be synced */
	String syncMonth; // Month to sync
}
