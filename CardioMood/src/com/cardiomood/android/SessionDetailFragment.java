package com.cardiomood.android;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.cardiomood.android.analysis.IndicatorsHelper;
import com.cardiomood.android.db.HeartRateDataItemDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.tools.PointD;
import com.cardiomood.android.tools.Tools;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;

/**
 * A fragment representing a single Session detail screen. This fragment is
 * either contained in a {@link SessionListActivity} in two-pane mode (on
 * tablets) or a {@link SessionDetailActivity} on handsets.
 */
public class SessionDetailFragment extends Fragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_SESSION_ID = "session_id";

	private Long sessionId;
	private GraphViewSeries series;
	
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public SessionDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_SESSION_ID)) {
			// Load the dummy content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
			sessionId = getArguments().getLong(ARG_SESSION_ID, -1L);
			if (sessionId != -1L) {
				getActivity().setTitle("Session #" + sessionId);
			}
		}
	}

	@SuppressLint("SimpleDateFormat")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View rootView = inflater.inflate(R.layout.fragment_session_detail,
				container, false);
		if (sessionId == -1L) {
			return rootView;
		}
		
		
		HeartRateDataItemDAO hrDAO = new HeartRateDataItemDAO(getActivity());
		hrDAO.open();
		List<HeartRateDataItem> items = hrDAO.getAllItemsOfSession(sessionId);
		hrDAO.close();
		if (!items.isEmpty()) {
			final DateFormat dateFormatter = new SimpleDateFormat("h:mm:ss");
			
			LinearLayout layout = (LinearLayout) rootView
					.findViewById(R.id.session_detail_graphs);
			layout.addView(createTensionIndexGraphView(dateFormatter));
			layout.addView(createHeartRateGraphView(items, dateFormatter));
			layout.addView(createRRIntervalsGraphView(items, dateFormatter));
		}

		return rootView;
	}
	
	private GraphView createHeartRateGraphView(List<HeartRateDataItem> items, final DateFormat dateFormatter) {
		GraphViewData[] data = new GraphViewData[items.size()];
		for (int i = 0; i < data.length; i++) {
			data[i] = new GraphViewData(items.get(i).getTimeStamp().getTime(), items.get(i).getHeartBeatsPerMinute());
		}
		series = new GraphViewSeries(data);
		
		GraphView graphView = new LineGraphView(getActivity(), "Heart Rate Graph: Session #" + sessionId) {
			@Override  
			protected String formatLabel(double value, boolean isValueX) {  
				if (isValueX) {
					// convert unix time to human time lol
					return dateFormatter.format(new Date((long) value));
				} else
					return super.formatLabel(value, isValueX); 
			}
			
			@Override
			protected double getMaxY() {
				return super.getMaxY() + 20;
			}
			
			@Override
			protected double getMinY() {
				return Math.max(0.0d, super.getMinY()-20);
			}
		};
		
		graphView.addSeries(series); // data
		graphView.setScrollable(true);
		graphView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int) 440));
		return graphView;
	}
	
	private GraphView createRRIntervalsGraphView(List<HeartRateDataItem> items, final DateFormat dateFormatter) {
		GraphViewData[] data = new GraphViewData[items.size()];
		long startDate = items.get(0).getTimeStamp().getTime();
		for (int i = 0; i < data.length; i++) {
			data[i] = new GraphViewData(startDate, items.get(i).getRrTime());
			startDate += (long) items.get(i).getRrTime();
		}
		series = new GraphViewSeries(data);
		
		GraphView graphView = new LineGraphView(getActivity(), "RR Intervals Graph: Session #" + sessionId) {
			@Override  
			protected String formatLabel(double value, boolean isValueX) {  
				if (isValueX) {
					// convert unix time to human time lol
					return dateFormatter.format(new Date((long) value));
				} else
					return super.formatLabel(value, isValueX); 
			}
		};
		
		graphView.addSeries(series); // data
		graphView.setScrollable(true);
		graphView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int) 440));
		
		return graphView;
	}
	
	private GraphView createTensionIndexGraphView(final DateFormat dateFormatter) {
		IndicatorsHelper iHelper = new IndicatorsHelper(getActivity());
		List<PointD> points = iHelper.getTensionIndex(sessionId);
		
		GraphView graphView = new LineGraphView(getActivity(), "Tension Index Graph: Session #" + sessionId) {
			@Override  
			protected String formatLabel(double value, boolean isValueX) {  
				if (isValueX) {
					// convert unix time to human time :)
					return dateFormatter.format(new Date((long) value));
				} else
					return super.formatLabel(value, isValueX); 
			}
			
//			@Override
//			protected double getMaxY() {
//				return super.getMaxY() + 20;
//			}
//			
//			@Override
//			protected double getMinY() {
//				return Math.max(0.0d, super.getMinY()-20);
//			}
			
			@Override
			public void drawSeries(Canvas canvas, GraphViewData[] values, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY, float horstart) {
				// draw background
				double lastEndY = 0;
				double lastEndX = 0;

				// draw data
				lastEndY = 0;
				lastEndX = 0;
				
				for (int i = 0; i < values.length; i++) {
					if (values[i].valueY < 80) {
						paint.setColor(Color.GRAY);
					} else if (values[i].valueY <= 150) {
						paint.setColor(Color.GREEN);
					} else paint.setColor(Color.RED);
					
					double valY = values[i].valueY - minY;
					double ratY = valY / diffY;
					double y = graphheight * ratY;

					double valX = values[i].valueX - minX;
					double ratX = valX / diffX;
					double x = graphwidth * ratX;

					if (i > 0) {
						float startX = (float) lastEndX + (horstart + 1);
						float startY = (float) (border - lastEndY) + graphheight;
						float endX = (float) x + (horstart + 1);
						float endY = (float) (border - y) + graphheight;

						canvas.drawLine(startX, startY, endX, endY, paint);
					}
					lastEndY = y;
					lastEndX = x;
				}
			}
		};
		
		if (!points.isEmpty()) {
			GraphViewData[] data = new GraphViewData[points.size()];
			for (int i = 0; i < data.length; i++) {
				data[i] = new GraphViewData((long) points.get(i).x, points.get(i).y);
			}
			series = new GraphViewSeries(data);
			
			graphView.addSeries(series); // data
			graphView.setScalable(true);
			graphView.setScrollable(true);
		}
		graphView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, (int) 440));
		return graphView;
	}
	
//	private GraphViewData[] loadData() {
//		List<HeartRateDataItem> items = hrDAO.getAllItemsOfSession(sessionId);
//		if (!items.isEmpty()) {
//			GraphViewData[] data = new GraphViewData[items.size()];
//			for (int i = 0; i < data.length; i++) {
//				data[i] = new GraphViewData(items.get(i).getTimeStamp().getTime(), items.get(i).getHeartBeatsPerMinute());
//			}
//			return data;
//		}
//		return new GraphViewData[] {};
//	}
	
//	@Override
//	public void onResume() {
//		super.onResume();
//		series.resetData(loadData());
//	}
}
