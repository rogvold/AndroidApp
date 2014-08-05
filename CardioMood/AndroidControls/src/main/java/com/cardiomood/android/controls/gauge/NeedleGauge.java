package com.cardiomood.android.controls.gauge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by danon on 11.07.2014.
 */
public class NeedleGauge extends View {

    public NeedleGauge(Context context) {
        super(context);
    }

    public NeedleGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NeedleGauge(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onDraw(Canvas canvas) {

        Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setColor(Color.RED);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(5.0f);

        Rect rect = canvas.getClipBounds();
        canvas.drawArc(new RectF(rect), -210, 240, false, arcPaint);
    }


}
