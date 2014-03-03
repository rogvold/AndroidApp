package com.cardiomood.android.progress;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by danon on 01.03.14.
 */
public class CircularProgressBar extends View {

    public static final float DEFAULT_MAX = 100;
    public static final float DEFAULT_MIN = 0;
    public static final float DEFAULT_LINE_WIDTH = 60;
    public static final int DEFAULT_COLOR = Color.RED;

    private float max = DEFAULT_MAX;
    private float progress = 0;
    private float min = DEFAULT_MIN;
    private float lineWidth = DEFAULT_LINE_WIDTH;
    private int color = DEFAULT_COLOR;

    private Paint mPaint;


    public CircularProgressBar(Context context) {
        super(context);
        init();
    }

    public CircularProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CircularProgressBar,
                0, 0);

        try {
            // read attributes
            setMax(attributes.getFloat(R.styleable.CircularProgressBar_max, (float) DEFAULT_MAX));
            setMin(attributes.getFloat(R.styleable.CircularProgressBar_min, (float) DEFAULT_MIN));
            setProgress(attributes.getFloat(R.styleable.CircularProgressBar_progress, 0));
            setLineWidth(attributes.getFloat(R.styleable.CircularProgressBar_lineWidth, DEFAULT_LINE_WIDTH));
            setColor(attributes.getColor(R.styleable.CircularProgressBar_color, DEFAULT_COLOR));
        } finally {
            attributes.recycle();
        }

        init();
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        if (max < min) {
            throw new IllegalArgumentException("Illegal value: max < min");
        }

        this.max = max;
        invalidate();
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        if (progress > max || progress < min) {
            throw new IllegalArgumentException("Illegal value: progress is outside range [min, max]");
        }
        this.progress = progress;
        invalidate();
    }

    public float getMin() {
        return min;
    }

    public void setMin(float min) {
        if (max < min) {
            throw new IllegalArgumentException("Illegal value: min > max");
        }
        this.min = min;
        invalidate();
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        if (lineWidth < 0) {
            throw new IllegalArgumentException("Illegal value: lineWidth < 0");
        }
        this.lineWidth = lineWidth;
        invalidate();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Clear canvas
        canvas.drawColor(Color.TRANSPARENT);

        RectF oval = getOval(canvas, 1);

        // background circle
        mPaint.setStrokeWidth(lineWidth);
        mPaint.setColor(Color.LTGRAY);
        canvas.drawArc(oval, 0, 360, false, mPaint);

        // current progress arc
        float angle = 360 * Math.abs((progress)/(max - min));
        mPaint.setColor(color);
        canvas.drawArc(oval, -90, angle, false, mPaint);
    }

    private RectF getOval(Canvas canvas, float factor) {
        RectF oval;
        final int canvasWidth = canvas.getWidth() - getPaddingLeft() - getPaddingRight() - (int) getLineWidth();
        final int canvasHeight = canvas.getHeight() - getPaddingTop() - getPaddingBottom() - (int) getLineWidth();

        if (canvasHeight >= canvasWidth) {
            oval = new RectF(0, 0, canvasWidth*factor, canvasWidth*factor);
        } else {
            oval = new RectF(0, 0, canvasHeight*factor, canvasHeight*factor);
        }

        oval.offset((canvasWidth-oval.width())/2 + getPaddingLeft()+getLineWidth()/2, (canvasHeight-oval.height())/2 + getPaddingTop()+getLineWidth()/2);

        return oval;
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    public static interface LabelConverter {
        String getLabelFor(float progress, float max);
    }
}
