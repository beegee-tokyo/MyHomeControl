package tk.giesecke.myhomecontrol.security;

import java.util.ArrayList;

/**
 * All available CCTV footages
 */
public class CCTVfootages {
	/** List of footage and/or directories as nice string */
	public String commError = "";
	/** List of todays footage as array */
	public final ArrayList<String> todaysList = new ArrayList<>();
	/** List of available history as array */
	public final ArrayList<String> availDaysList = new ArrayList<>();
	/** List of lists with daily footages */
	public final ArrayList<ArrayList<String>> daysList = new ArrayList<>();
}
