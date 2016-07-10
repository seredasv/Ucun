package com.nasctech.ucun.view;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.nasctech.ucun.R;

import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;

/**
 * hack view to disable home button
 */
public class HomeHackDialog extends AlertDialog {
    private Activity activity;
    private Handler windowCloseHandler = new Handler();
    private Runnable windowCloserRunnable = new Runnable() {
        @Override
        public void run() {
            ActivityManager am = (ActivityManager) activity.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            ComponentName cn = am.getRunningTasks(1).get(0).topActivity;

            if (cn != null && cn.getClassName().equals("com.android.systemui.recent.RecentsActivity")) {
                toggleRecents();
            }
        }
    };

    public HomeHackDialog(Activity activity) {
        super(activity, R.style.OverlayDialog);
        this.activity = activity;
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.type = TYPE_SYSTEM_ERROR;
        params.dimAmount = 0.0F; // transparent
        params.width = 0;
        params.height = 0;
        params.gravity = Gravity.BOTTOM;
        getWindow().setAttributes(params);
        getWindow().setFlags(FLAG_SHOW_WHEN_LOCKED | FLAG_NOT_TOUCH_MODAL, 0xffffff);
        setOwnerActivity(activity);
        setCancelable(false);
    }

    public final boolean dispatchTouchEvent(MotionEvent motionevent) {
        return true;
    }

    protected final void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        FrameLayout framelayout = new FrameLayout(getContext());
        framelayout.setBackgroundColor(0);
        setContentView(framelayout);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            activity.sendBroadcast(closeDialog);

            windowCloseHandler.post(windowCloserRunnable);
            Log.d("onFocus", "closed smth");

        }
    }

    private void toggleRecents() {
        Intent closeRecents = new Intent("com.android.systemui.recent.action.TOGGLE_RECENTS");
        closeRecents.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        ComponentName recents = new ComponentName("com.android.systemui", "com.android.systemui.recent.RecentsActivity");
        closeRecents.setComponent(recents);
        activity.startActivity(closeRecents);
    }
}
