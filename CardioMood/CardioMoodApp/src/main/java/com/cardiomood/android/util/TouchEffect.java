package com.cardiomood.android.util;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

// TODO: Auto-generated Javadoc

/**
 * The Class TouchEffect is a Base Touch listener. It can be attached as touch
 * listener for any view that as some valid background drawable or color. It
 * simply set alpha value for background to create a Touch like effect on View
 */
public class TouchEffect implements OnTouchListener
{

	/* (non-Javadoc)
	 * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event)
	{

		if (event.getAction() == MotionEvent.ACTION_DOWN)
		{
			Drawable d = v.getBackground();
			d.mutate();
			d.setAlpha(150);
			if (Build.VERSION.SDK_INT < 16) {
                v.setBackgroundDrawable(d);
            } else {
                v.setBackground(d);
            }
		}
		else if (event.getAction() == MotionEvent.ACTION_UP
				|| event.getAction() == MotionEvent.ACTION_CANCEL)
		{
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

}
