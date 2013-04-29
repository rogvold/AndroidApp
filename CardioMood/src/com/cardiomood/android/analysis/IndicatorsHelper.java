package com.cardiomood.android.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cardiomood.android.analysis.indicators.AbstractIndicatorsService;
import com.cardiomood.android.analysis.indicators.HRVIndicatorsService;
import com.cardiomood.android.config.ConfigurationConstants;
import com.cardiomood.android.config.ConfigurationManager;
import com.cardiomood.android.db.HeartRateDataItemDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.tools.PointD;

import android.content.Context;
import android.util.Log;

public class IndicatorsHelper implements ConfigurationConstants {
	
	private static final String TAG = "IndicatorsHelper";
	
	Context context;
	ConfigurationManager config;
	
	public IndicatorsHelper(Context ctx) {
		this.context = ctx;
		this.config = ConfigurationManager.getInstance();
	}
	
	public List<PointD> getPlotOfParameters(long sessionId, AbstractIndicatorsService iService, String indicatorName, long msStep) throws Exception {        
        HeartRateDataItemDAO dao = new HeartRateDataItemDAO(context);
        dao.open();
        List<HeartRateDataItem> rates = dao.getAllItemsOfSession(sessionId);
        dao.close();
        List<PointD> points = new ArrayList<PointD>();
        int dur = config.getInteger(WINDOW_DURATION_KEY, 120);
        iService.setDuration(dur * 1000);      
        
        int beginIndex = 0;
        long sum = 0;
        for (int i = 0; i < rates.size(); i++) {
            beginIndex = i;
            if (sum >= iService.getDuration()) {
                break;
            }
            sum += (int) rates.get(i).getRrTime();
        }

        //System.out.println("beginIndex = " + beginIndex);
        if (beginIndex == rates.size() - 1){
            //System.out.println("not enough data...");
            return Collections.EMPTY_LIST;
        }
        
        
        int curr = beginIndex;
        int prev = 0;//not right
        List<Integer> list = new ArrayList<Integer>();

        while (curr < rates.size()) {
            list.clear();
            for (int i = prev; i < curr; i++) {
                list.add((int) rates.get(i).getRrTime());
            }
            iService.setIntervals(list);
            //System.out.println("calculating indicator " + indicatorName );
            
            PointD point = new PointD( (rates.get(0).getTimeStamp().getTime() + sum), (double) iService.parameter(indicatorName)); 
            points.add(point);
            Log.d(TAG, "point = " + (long )point.x + ", " + point.y);
            int t = 0;
            int curr2 = curr, prev2 = prev;
            for (int u = curr; u < rates.size(); u++) {
                if (t >= msStep) {
                    break;
                }
                t += rates.get(u).getRrTime();
                curr2++;
                prev2++;
            }
            prev = prev2;
            curr = curr2;
            sum += t;
        }

        return points;
    }
	
	public List<PointD> getTensionIndex(long sessionId) {
		try {
			return getPlotOfParameters(sessionId, new HRVIndicatorsService(), "IN", config.getInteger(STEP_DURATION_KEY, 5000));
		} catch (Exception ex) {
			return Collections.EMPTY_LIST;
		}
	}

}
