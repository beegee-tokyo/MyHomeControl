package tk.giesecke.myhomecontrol.solar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;

import tk.giesecke.myhomecontrol.R;

/**
 * Show time, consumption and solar power when user touches a data point
 *
 */
public class ChartCustomMarkerView extends MarkerView {

	/** Pointer to text view for time */
	private final TextView tvMarkerTime;
	/** Pointer to text view for consumption */
	private final TextView tvMarkerCons;
	/** Pointer to text view for consumption */
	private final TextView tvMarkerConsAll;
	/** Pointer to text view for solar power */
	private final TextView tvMarkerSolar;
	/** Pointer to text view for solar power */
	private final TextView tvMarkerSolarAll;

	public ChartCustomMarkerView(Context context) {
		super(context, R.layout.cu_plot_marker);
		/** Text view for time in marker */
		tvMarkerTime = (TextView) findViewById(R.id.tv_marker_time);
		/** Text view for consumption in marker */
		tvMarkerCons = (TextView) findViewById(R.id.tv_marker_cons);
		/** Text view for consumption in marker */
		tvMarkerConsAll = (TextView) findViewById(R.id.tv_marker_cons_all);
		/** Text view for solar power in marker */
		tvMarkerSolar = (TextView) findViewById(R.id.tv_marker_solar);
		/** Text view for solar power in marker */
		tvMarkerSolarAll = (TextView) findViewById(R.id.tv_marker_solar_all);
	}

	// callbacks every time the MarkerView is redrawn, can be used to update the
	// content (user-interface)
	@SuppressLint("DefaultLocale")
	@Override
	public void refreshContent(Entry e, Highlight highlight) {
		/** Index for the series at the touched data point */

		int dataIndex = highlight.getXIndex();
		/** Entry with data of solar power at given index */
		Entry touchSolar = ChartHelper.solarSeries.get(dataIndex);
		/** Entry with data of consumption at given index */
		Entry touchCons = ChartHelper.consMSeries.get(dataIndex);
		if (touchCons.getVal() == 0) {
			touchCons = ChartHelper.consPSeries.get(dataIndex);
		}

		tvMarkerTime.setText(ChartHelper.timeSeries.get(dataIndex));
		/** Float value with consumption until touched data point */
		float consAll = 0;
		/** Entry for accumulate power consumption / production */
		Entry consAllEntry;
		/** Float value with production until touched data point */
		float solAll = 0;
		/** Entry for accumulate power production / production */
		Entry solAllEntry;
		for (int i=0; i<=dataIndex; i++) {
			consAllEntry = ChartHelper.consMSeries.get(i);
			if (touchCons.getVal() == 0) {
				consAllEntry = ChartHelper.consPSeries.get(i);
			}
			consAll += consAllEntry.getVal()/60/1000;
			solAllEntry = ChartHelper.solarSeries.get(i);
			solAll += solAllEntry.getVal()/60/1000;
		}
		/** Text for update text view */
		String updateTxt = (Float.toString(touchCons.getVal()) + "W");
		tvMarkerCons.setText(updateTxt);
		updateTxt = (String.format("%.3f", consAll) + "kWh");
		tvMarkerConsAll.setText(updateTxt);
		updateTxt = (Float.toString(touchSolar.getVal())+"W");
		tvMarkerSolar.setText(updateTxt);
		updateTxt = (String.format("%.3f", solAll) + "kWh");
		tvMarkerSolarAll.setText(updateTxt);
	}

	/**
	 * Use this to return the desired offset you wish the MarkerView to have on the x-axis. By returning -(getWidth() /
	 * 2) you will center the MarkerView horizontally.
	 *
	 * @param xpos
	 * 		the position on the x-axis in pixels where the marker is drawn
	 * @return
	 *      offset on x-axis
	 */
	@Override
	public int getXOffset(float xpos) {
		return -(getWidth() / 2);
	}

	/**
	 * Use this to return the desired position offset you wish the MarkerView to have on the y-axis. By returning
	 * -getHeight() you will cause the MarkerView to be above the selected value.
	 *
	 * @param ypos
	 * 		the position on the y-axis in pixels where the marker is drawn
	 * @return
	 *      offset on y-axis
	 */
	@Override
	public int getYOffset(float ypos) {
		return -getHeight();
	}

	@Override
	public void draw(Canvas canvas, float posx, float posy)
	{
		// take offsets into consideration
//		posx = getDeviceWidth(MyHomeControl.appContext)/2;
		posx = getDeviceWidth(getContext())/2;
		posy = 30;

		// translate to the correct position and draw
		canvas.translate(posx, posy);
		draw(canvas);
		canvas.translate(-posx, -posy);
	}

	/**
	 * Get device display width in pixel
	 *
	 * @param context
	 *          Application context
	 * @return <code>int</code>
	 *          display width in pixel
	 */
	@SuppressWarnings("deprecation")
	private static int getDeviceWidth(Context context){
		/** Window manager instance */
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		if (wm != null) {
			/** Display instance */
			Display display = wm.getDefaultDisplay();
			return display.getWidth();
		}
		return 1024; // Guess a screen width
	}
}
