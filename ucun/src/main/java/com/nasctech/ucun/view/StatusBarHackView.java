package com.nasctech.ucun.view;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;

//hack view for status bar
public class StatusBarHackView extends ViewGroup {

    public StatusBarHackView(Context context) {
        super(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.v("customViewGroup", "**********Intercepted");
        return true;
    }
}
