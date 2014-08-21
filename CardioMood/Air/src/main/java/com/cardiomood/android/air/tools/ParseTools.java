package com.cardiomood.android.air.tools;

import android.text.TextUtils;

import com.parse.ParseUser;

/**
 * Created by danon on 14.08.2014.
 */
public abstract class ParseTools {

    public static String getUserFullName(ParseUser pu) {
        String fullName = pu.has("lastName") ? pu.getString("lastName") : "";
        if (!TextUtils.isEmpty(fullName))
            fullName += " ";
        if (pu.has("firstName"))
            fullName += pu.getString("firstName");
        return fullName;
    }

}
