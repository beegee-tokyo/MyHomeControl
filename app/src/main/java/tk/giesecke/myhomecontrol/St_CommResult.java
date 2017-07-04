package tk.giesecke.myhomecontrol;

/**
 * Wrapper to send several parameters to onPostExecute of AsyncTask
 */
class St_CommResult {
	/** URL to be called */
	String httpURL;
	/** Command to be send */
	String comCmd; // Requested command
	/** Returned result from URL */
	String comResult; // Result of communication
	/** Device index (for multiple aircons) */
	int deviceIndex;
	/** Flag for communication result*/
	boolean comFailed;
}
