package com.cardiomood.sport.android.tools;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * PointD holds two double coordinates
 */
public class PointD implements Parcelable {
    public double x;
    public double y;
    
    public PointD() {}

    public PointD(double x, double y) {
        this.x = x;
        this.y = y; 
    }
    
    public PointD(PointD p) { 
        this.x = p.x;
        this.y = p.y;
    }
    
    /**
     * Set the point's x and y coordinates
     */
    public final void set(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Set the point's x and y coordinates to the coordinates of p
     */
    public final void set(PointD p) { 
        this.x = p.x;
        this.y = p.y;
    }
    
    public final void negate() { 
        x = -x;
        y = -y; 
    }
    
    public final void offset(double dx, double dy) {
        x += dx;
        y += dy;
    }
    
    /**
     * Returns true if the point's coordinates equal (x,y)
     */
    public final boolean equals(double x, double y) { 
        return this.x == x && this.y == y; 
    }

    /**
     * Return the euclidian distance from (0,0) to the point
     */
    public final double length() { 
        return length(x, y); 
    }
    
    /**
     * Returns the euclidian distance from (0,0) to (x,y)
     */
    public static double length(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    /**
     * Parcelable interface methods
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Write this point to the specified parcel. To restore a point from
     * a parcel, use readFromParcel()
     * @param out The parcel to write the point's coordinates into
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(x);
        out.writeDouble(y);
    }

    public static final Creator<PointD> CREATOR = new Creator<PointD>() {
        /**
         * Return a new point from the data in the specified parcel.
         */
        public PointD createFromParcel(Parcel in) {
            PointD r = new PointD();
            r.readFromParcel(in);
            return r;
        }

        /**
         * Return an array of rectangles of the specified size.
         */
        public PointD[] newArray(int size) {
            return new PointD[size];
        }
    };

    /**
     * Set the point's coordinates from the data stored in the specified
     * parcel. To write a point to a parcel, call writeToParcel().
     *
     * @param in The parcel to read the point's coordinates from
     */
    public void readFromParcel(Parcel in) {
        x = in.readDouble();
        y = in.readDouble();
    }
}
