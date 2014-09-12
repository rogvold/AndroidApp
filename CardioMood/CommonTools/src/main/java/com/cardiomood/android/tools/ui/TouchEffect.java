package com.cardiomood.android.tools.ui;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * The Class TouchEffect.
 */
public abstract class TouchEffect implements OnTouchListener {

	public static final TouchEffect FADE_ON_TOUCH = new TouchEffect() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Drawable d = v.getBackground();
                d.mutate();
                d.setAlpha(150);
                if (Build.VERSION.SDK_INT < 16) {
                    v.setBackgroundDrawable(d);
                } else {
                    v.setBackground(d);
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                Drawable d = v.getBackground();
                d.setAlpha(255);
                if (Build.VERSION.SDK_INT < 16) {
                    v.setBackgroundDrawable(d);
                } else {
                    v.setBackground(d);
                }
            }

            return false;
        }
    };
}
