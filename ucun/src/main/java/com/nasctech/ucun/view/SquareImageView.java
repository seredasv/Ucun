package com.nasctech.ucun.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SquareImageView extends ImageView {
    public SquareImageView(Context context) {
        super(context);
        setClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);
    }

    public SquareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth()); //Snap to width
    }
}
