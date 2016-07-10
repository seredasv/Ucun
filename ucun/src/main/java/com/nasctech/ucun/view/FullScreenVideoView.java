package com.nasctech.ucun.view;

import android.content.Context;
import android.util.AttributeSet;

public class FullScreenVideoView extends io.vov.vitamio.widget.VideoView {

    private int videoWidth;
    private int videoHeight;

    public FullScreenVideoView(Context context) {
        super(context);
    }

    public FullScreenVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FullScreenVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width, height;
        width = getDefaultSize(videoWidth, widthMeasureSpec);
        height = getDefaultSize(videoHeight, heightMeasureSpec);

        if (videoWidth > 0 && videoHeight > 0) {
            if (videoWidth * height > width * videoHeight) {
                height = width * videoHeight / videoWidth;
            } else if (videoWidth * height < width * videoHeight) {
                width = height * videoWidth / videoHeight;
            }
        }
        setMeasuredDimension(width, height);
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }
}

