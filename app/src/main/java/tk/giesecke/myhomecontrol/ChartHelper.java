package tk.giesecke.myhomecontrol;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

public class ChartHelper extends MyHomeControl {

	/** MPAndroid chart view for the current chart */
	public static LineChart lineChart;
	/** LineData for the plot */
	public static LineData plotData;
	/** Text view for the date */
	public static TextView chartTitle;

	/** List to hold the timestamps for the chart from a log file */
	public static final ArrayList<String> timeSeries = new ArrayList<>();
	/** List to hold the measurements of the solar panel for the chart from a log file */
	public static final ArrayList<Entry> solarSeries = new ArrayList<>();
	/** List to hold the measurement of the consumption for the chart from a log file */
	public static final ArrayList<Entry> consPSeries = new ArrayList<>();
	/** List to hold the measurement of the consumption for the chart from a log file */
	public static final ArrayList<Entry> consMSeries = new ArrayList<>();
	/** List to hold the timestamps for a continuously updated chart */
	private static final ArrayList<String> timeStamps = new ArrayList<>();
	/** List to hold the measurements of the solar panel for a continuously updated chart */
	private static final ArrayList<Float> solarPower = new ArrayList<>();
	/** List to hold the measurement of the consumption for a continuously updated chart */
	private static final ArrayList<Float> consumPPower = new ArrayList<>();
	/** List to hold the measurement of the consumption for a continuously updated chart */
	private static final ArrayList<Float> consumMPower = new ArrayList<>();
	/** List to hold the timestamps for a chart from logged data */
	public static final ArrayList<String> timeStampsCont = new ArrayList<>();
	/** List to hold the measurements of the solar panel for a chart from logged data */
	public static final ArrayList<Float> solarPowerCont = new ArrayList<>();
	/** List to hold the measurement of the consumption for a chart from logged data */
	public static final ArrayList<Float> consumPPowerCont = new ArrayList<>();
	/** List to hold the measurement of the consumption for a chart from logged data */
	public static final ArrayList<Float> consumMPowerCont = new ArrayList<>();

	/** Solar power received from spMonitor device as minute average */
	public static Float solarPowerMin = 0.0f;
	/** Solar power received from spMonitor device as minute average */
	public static Float lastSolarPowerMin = 0.0f;
	/** Consumption received from spMonitor device as minute average */
	public static Float consPowerMin = 0.0f;
	/** Consumption received from spMonitor device as minute average */
	public static Float lastConsPowerMin = 0.0f;
	/** Flag for showing solar power data */
	private static final boolean showSolar = true;
	/** Flag for showing consumption data */
	private static final boolean showCons = true;
	/** Flag if UI auto refresh is on or off */
	public static boolean autoRefreshOn = true;

	/** Day stamp of data */
	private static String dayToShow;


	/**
	 * Initialize chart to show solar power, consumption and light values
	 *
	 * @param isContinuous
	 *          Flag for display mode
	 *          true = continuous display of data received from spMonitor
	 *          false = display content of a log file
	 */
	@SuppressWarnings("deprecation")
	@SuppressLint("SimpleDateFormat")
	public static void initChart(boolean isContinuous) {

		timeSeries.clear();
		solarSeries.clear();
		consPSeries.clear();
		consMSeries.clear();
		if (!isContinuous) {
			for (int i=0; i<timeStamps.size(); i++) {
				timeSeries.add(timeStamps.get(i));
			}
			for (int i=0; i<solarPower.size(); i++) {
				solarSeries.add(new Entry(solarPower.get(i), i));
			}
			for (int i=0; i<consumPPower.size(); i++) {
				consPSeries.add(new Entry(consumPPower.get(i), i));
			}
			for (int i=0; i<consumMPower.size(); i++) {
				consMSeries.add(new Entry(consumMPower.get(i), i));
			}
		} else {
			if (timeStampsCont.size() != 0) {
				for (int i=0; i<timeStampsCont.size(); i++) {
					timeSeries.add(timeStampsCont.get(i));
				}
				for (int i=0; i<solarPowerCont.size(); i++) {
					solarSeries.add(new Entry(solarPowerCont.get(i), i));
				}
				for (int i=0; i<consumPPowerCont.size(); i++) {
					consPSeries.add(new Entry(consumPPowerCont.get(i), i));
				}
				for (int i=0; i<consumMPowerCont.size(); i++) {
					consMSeries.add(new Entry(consumMPowerCont.get(i), i));
				}
			}
		}
		/** Line data set for solar data */
		LineDataSet solar = new LineDataSet(solarSeries, "Solar");
		/** Line data set for consumption data */
		LineDataSet consP = new LineDataSet(consPSeries, "Export");
		/** Line data set for consumption data */
		LineDataSet consM = new LineDataSet(consMSeries, "Import");

		solar.setLineWidth(1.75f);
		solar.setCircleSize(0f);
		solar.setColor(0xFFFFBB33);
		solar.setCircleColor(0xFFFFBB33);
		solar.setHighLightColor(0xFFFFBB33);
		solar.setFillColor(0xAAFFBB33);
		solar.setDrawCubic(true);
		if (showSolar) {
			solar.setVisible(true);
		} else {
			solar.setVisible(false);
		}
		solar.setDrawValues(false);
		solar.setDrawFilled(true);

		consP.setLineWidth(1.75f);
		consP.setCircleSize(0f);
		consP.setColor(Color.GREEN);
		consP.setCircleColor(Color.GREEN);
		consP.setHighLightColor(Color.GREEN);
		consP.setFillColor(0xAA00FF00);
		consP.setDrawCubic(true);
		if (showCons) {
			consP.setVisible(true);
		} else {
			consP.setVisible(false);
		}
		consP.setDrawValues(false);
		consP.setDrawValues(false);
		consP.setDrawFilled(true);
		consP.setAxisDependency(YAxis.AxisDependency.LEFT);

		consM.setLineWidth(1.75f);
		consM.setCircleSize(0f);
		consM.setColor(Color.RED);
		consM.setCircleColor(Color.RED);
		consM.setHighLightColor(Color.RED);
		consM.setFillColor(0xAAFF0000);
		consM.setDrawCubic(true);
		if (showCons) {
			consM.setVisible(true);
		} else {
			consM.setVisible(false);
		}
		consM.setDrawValues(false);
		consM.setDrawValues(false);
		consM.setDrawFilled(true);
		consM.setAxisDependency(YAxis.AxisDependency.LEFT);


		/** Data object with the data set and the y values */
		plotData = new LineData(timeSeries);
		plotData.addDataSet(solar);
		plotData.addDataSet(consP);
		plotData.addDataSet(consM);

		lineChart.setBackgroundColor(Color.BLACK);
		lineChart.setDrawGridBackground(false);
		lineChart.setTouchEnabled(true);
		lineChart.setDragEnabled(true);
		lineChart.setAutoScaleMinMaxEnabled(true);
		lineChart.setData(plotData);

		if (dayToShow != null) {
			/** Calendar instance */
			Calendar c = Calendar.getInstance();
			@SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("yy-MM-dd");
			try {
				/** Date for formatting output */
				Date myDate = df.parse(dayToShow.trim());
				c.setTime(myDate);
				df = new SimpleDateFormat("yyyy-MMM-dd");
				dayToShow = df.format(c.getTime());
			} catch (ParseException ignore) {
			}

			chartTitle.setText(dayToShow);
		}

		/** Instance of left y axis */
		YAxis lYAx = lineChart.getAxisLeft();
		lYAx.setEnabled(true);
		lYAx.setTextColor(Color.WHITE);
		lYAx.setStartAtZero(false);
		lYAx.setSpaceTop(1);
		lYAx.setSpaceBottom(1);

		/** Instance of right y axis */
		YAxis rYAx = lineChart.getAxisRight();
		rYAx.setEnabled(true);
		rYAx.setTextColor(Color.WHITE);
		rYAx.setStartAtZero(false);
		rYAx.setSpaceTop(1);
		rYAx.setSpaceBottom(1);
		/** Hide right axis */
		rYAx.setDrawLabels(false);
		rYAx.setEnabled(false);

		/** Instance of x axis */
		XAxis xAx = lineChart.getXAxis();
		xAx.setEnabled(true);
		xAx.setTextColor(Color.WHITE);
		xAx.setPosition(XAxis.XAxisPosition.BOTTOM);

		lineChart.getLegend().setEnabled(false);

		// create a custom MarkerView (extend MarkerView) and specify the layout
		// to use for it
		/** Instance of custom marker view handler */
		ChartCustomMarkerView mv = new ChartCustomMarkerView(appContext);
//		lineChart.setMarkerView(mv);

		// set the marker to the chart
		lineChart.setMarkerView(mv);

		// let the chart know it's data has changed
		lineChart.notifyDataSetChanged();
		lineChart.invalidate();
	}

	/**
	 * Load plot series with data received from database
	 *
	 * @param data
	 *        database cursor with recorded values
	 *        each cursor entry has 8 values
	 *        cursor[0] = year stamp
	 *        cursor[1] = month stamp
	 *        cursor[2] = day stamp
	 *        cursor[3] = hour stamp
	 *        cursor[4] = minute stamp
	 *        cursor[5] = sensor power
	 *        cursor[6] = consumed power
	 *        cursor[7] = light value
	 */
	public static void fillSeries(Cursor data) {

		data.moveToFirst();
		/* Solar energy generated up to now on the displayed day */
		float solarEnergy = 0f;
		/* Consumed energy generated up to now on the displayed day */
		float consEnergy = 0f;

		/** Array list to hold time stamps */
		ArrayList<String> tempTimeStamps;
		/** Array list to hold solar power values */
		ArrayList<Float> tempSolarStamps;
		/** Array list to hold consumption values */
		ArrayList<Float> tempConsPStamps;
		/** Array list to hold consumption values */
		ArrayList<Float> tempConsMStamps;
		if (showingLog) {
			tempTimeStamps = timeStamps;
			tempSolarStamps = solarPower;
			tempConsPStamps = consumPPower;
			tempConsMStamps = consumMPower;
		} else {
			tempTimeStamps = timeStampsCont;
			tempSolarStamps = solarPowerCont;
			tempConsPStamps = consumPPowerCont;
			tempConsMStamps = consumMPowerCont;
		}
		tempTimeStamps.clear();
		tempSolarStamps.clear();
		tempConsPStamps.clear();
		tempConsMStamps.clear();
		for (int cursorIndex=0; cursorIndex<data.getCount(); cursorIndex++) {
			tempTimeStamps.add(("00" +
					data.getString(3)).substring(data.getString(3).length())
					+ ":" + ("00" +
					data.getString(4)).substring(data.getString(4).length()));
			dayToShow = String.valueOf(data.getInt(0)) + "-" +
					String.valueOf(data.getInt(1)) + "-" +
					String.valueOf(data.getInt(2));
			tempSolarStamps.add(data.getFloat(5));
			if (data.getFloat(6) < 0.0f) {
				tempConsPStamps.add(data.getFloat(6));
				tempConsMStamps.add(0.0f);
			} else {
				tempConsMStamps.add(data.getFloat(6));
				tempConsPStamps.add(0.0f);
			}
			solarEnergy += data.getFloat(5)/60/1000;
			consEnergy += Math.abs(data.getFloat(6)/60/1000);
			data.moveToNext();
		}

		/** Text for update of text view */
		String updateTxt;
		if (showingLog) {
			/** Text view to show consumed / produced energy */
			TextView energyText = (TextView) MyHomeControl.appView.findViewById(R.id.tv_cons_energy);
			energyText.setVisibility(View.VISIBLE);
			updateTxt = "Consumed: " + String.format("%.3f", consEnergy) + "kWh";
			energyText.setText(updateTxt);
			energyText = (TextView) MyHomeControl.appView.findViewById(R.id.tv_solar_energy);
			energyText.setVisibility(View.VISIBLE);
			updateTxt = "Produced: " + String.format("%.3f", solarEnergy) + "kWh";
			energyText.setText(updateTxt);
		}
		/** Text view to show max consumed / produced power */
		if (tempConsMStamps.size() != 0 && tempSolarStamps.size() != 0) {
			/** Text view to show max consumed / produced energy */
			TextView maxPowerText = (TextView) MyHomeControl.appView.findViewById(R.id.tv_cons_max);
			updateTxt = "(" + String.format("%.0f", Collections.max(tempConsMStamps)) + "W)";
			maxPowerText.setText(updateTxt);
			maxPowerText = (TextView) MyHomeControl.appView.findViewById(R.id.tv_solar_max);
			updateTxt = "(" + String.format("%.0f", Collections.max(tempSolarStamps)) + "W)";
			maxPowerText.setText(updateTxt);
		}
	}
}
