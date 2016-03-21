package tk.giesecke.myhomecontrol;

/**
 * Wrapper to send several parameters to onPostExecute of AsyncTask
 *
 * comResult = return string as JSON from the ESP device
 */
public class ESPcommResultWrapper {
	/** URL to be called */
	public String httpURL;
	/** Command to be send */
	public String comCmd; // Requested command
	/** Returned result from URL */
	public String comResult; // Result of communication
	/** Caller id (sec, air or spm) */
	public String callID;
	/** Device index (for multiple aircons) */
	public int deviceIndex;
}
