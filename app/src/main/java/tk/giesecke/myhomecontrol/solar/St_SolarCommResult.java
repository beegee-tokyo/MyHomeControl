package tk.giesecke.myhomecontrol.solar;

/**
 * Wrapper to send several parameters to onPostExecute of AsyncTask
 */
public class St_SolarCommResult {
	/** Returned result from spMonitor device */
	public String taskResult;// Result of communication
	/** Month that should be synced */
	public String syncMonth; // Month to sync
}
