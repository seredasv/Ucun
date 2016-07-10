package com.nasctech.ucun.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;

public class KeyLocker {
    private OverlayDialog overlayDialog;

    public void lock(Activity activity) {
        if (overlayDialog == null) {
            overlayDialog = new OverlayDialog(activity);
            overlayDialog.show();
        }
    }

    public void unlock() {
        if (overlayDialog != null) {
            overlayDialog.dismiss();
            overlayDialog = null;
        }
    }

    private static class OverlayDialog extends AlertDialog {

        public OverlayDialog(Activity activity) {
            super(activity);
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
    }
}
