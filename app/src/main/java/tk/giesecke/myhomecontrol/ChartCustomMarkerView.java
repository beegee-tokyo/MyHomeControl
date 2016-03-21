package tk.giesecke.myhomecontrol;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;

/**
 * Show time, consumption and solar power when user touches a data point
 *
 */
public class ChartCustomMarkerView extends MarkerView {

	/** Pointer to text view for time */
	private final TextView tvMarkerTime;
	/** Pointer to text view for consumption */
	private final TextView tvMarkerCons;
	/** Pointer to text view for solar power */
	private final TextView tvMarkerSolar;

	public ChartCustomMarkerView(Context context) {
		super(context, R.layout.plot_marker);
		/** Text view for time in marker */
		tvMarkerTime = (TextView) findViewById(R.id.tv_marker_time);
		/** Text view for consumption in marker */
		tvMarkerCons = (TextView) findViewById(R.id.tv_marker_cons);
		/** Text view for solar power in marker */
		tvMarkerSolar = (TextView) findViewById(R.id.tv_marker_solar);
	}

	// callbacks every time the MarkerView is redrawn, can be used to update the
	// content (user-interface)
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
		/** Text for update text view */
		String updateTxt = (Float.toString(touchCons.getVal())+"W");
		tvMarkerCons.setText(updateTxt);
		updateTxt = (Float.toString(touchSolar.getVal())+"W");
		tvMarkerSolar.setText(updateTxt);
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
}
