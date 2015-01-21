package com.cardiomood.framework.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by antondanhsin on 06/11/14.
 */
public class GlyphTextView extends TextView {

    public GlyphTextView(Context context) {
        super(context);
        if (!isInEditMode()) {
            Typeface tf = Typeface.createFromAsset(context.getAssets(), "fonts/androidicons.ttf");
            setTypeface(tf);
        }
    }

    public GlyphTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            Typeface tf = Typeface.createFromAsset(context.getAssets(), "fonts/androidicons.ttf");
            setTypeface(tf);
        }
    }

    public GlyphTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            Typeface tf = Typeface.createFromAsset(context.getAssets(), "fonts/androidicons.ttf");
            setTypeface(tf);
        }
    }

}
