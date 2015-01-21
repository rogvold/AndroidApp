package com.cardiomood.framework.service;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A Parcelable class that holds data package obtained from BLE Heart Rate Service.
 *
 * Created by Anton Danshin on 20/10/14.
 */
public class CardioDataPackage implements Parcelable {

    private long timestamp;

    private int bpm;

    private int rr[];

    public CardioDataPackage() {
        rr = new int[0];
    }

    public CardioDataPackage(long timestamp, int bpm, int[] rr) {
        this.timestamp = timestamp;
        this.bpm = bpm;
        this.rr = rr;
    }

    private CardioDataPackage(Parcel parcel) {
        this.timestamp = parcel.readLong();
        this.bpm = parcel.readInt();
        this.rr = parcel.createIntArray();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int[] getRr() {
        return rr;
    }

    public void setRr(int[] rr) {
        this.rr = rr;
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(timestamp);
        parcel.writeInt(bpm);
        parcel.writeIntArray(rr);
    }

    public static final Creator<CardioDataPackage> CREATOR
            = new Creator<CardioDataPackage>() {

        @Override
        public CardioDataPackage createFromParcel(Parcel in) {
            return new CardioDataPackage(in);
        }

        @Override
        public CardioDataPackage[] newArray(int size) {
            return new CardioDataPackage[size];
        }
    };
}
