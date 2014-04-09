package com.cardiomood.math;

import com.cardiomood.math.window.DataWindow;

import java.util.HashSet;
import java.util.Set;

public class DataWindowSet {

    private double totalDuration = 0;
    private int totalCount = 0;
    private Set<DataWindow> windows = new HashSet<DataWindow>();

    public DataWindowSet() {
    }

    public DataWindowSet(double[] rrIntervals) {
        this.totalDuration = 0;
        for (double rrInterval: rrIntervals) {
            this.totalDuration += rrInterval;
            this.totalCount++;
            addElementToWindows(rrInterval);
        }
    }

    private void addElementToWindows(double rrInterval) {
        for (DataWindow window: windows) {
            window.add(rrInterval);
        }
    }

    public void addWindow(DataWindow window) {
        this.windows.add(window);
    }

    public double getTotalDuration() {
        return totalDuration;
    }

    public void addIntervals(double... rrIntervals) {
        for (double rrInterval : rrIntervals) {
            this.totalDuration += rrInterval;
            this.totalCount++;
            addElementToWindows(rrInterval);
        }
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void clear() {
        this.totalCount = 0;
        this.totalDuration = 0.0;
        for (DataWindow window: windows) {
            window.clear();
        }
    }

    public void removeWindow(DataWindow window) {
        windows.remove(window);
    }
}
