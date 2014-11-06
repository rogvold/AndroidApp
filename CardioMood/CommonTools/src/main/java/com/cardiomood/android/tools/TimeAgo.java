package com.cardiomood.android.tools;

import android.content.Context;

/**
 * Created by antondanhsin on 22/10/14.
 */
public class TimeAgo {

    private TimeAgo() {
        // to prevent instantiation
    }

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;
    private static final int MONTH_MILLIS = 30 * DAY_MILLIS;


    public static String getTimeAgo(Context ctx, long time) {
        long now = System.currentTimeMillis();
        if (time > now || time < 0) {
            return "(none)";
        }

        // TODO: localize
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "just now";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return "a minute ago";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " minutes ago";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return "an hour ago";
        } else if (diff < 24 * HOUR_MILLIS) {
            return diff / HOUR_MILLIS + " hours ago";
        } else if (diff < 48 * HOUR_MILLIS) {
            return "yesterday";
        } else if (diff <  25 * DAY_MILLIS) {
            return "days ago";
        } else if (diff < 50 * DAY_MILLIS) {
            return "a month ago";
        } else if (diff < 24 * MONTH_MILLIS) {
            return "months ago";
        } else if (time > 0) {
            return "years ago";
        } else {
            return "never";
        }
    }
}