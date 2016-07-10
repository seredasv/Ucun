package com.nasctech.ucun.utils;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.loopj.android.http.FileAsyncHttpResponseHandler;

import org.apache.http.Header;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VideoPollingService extends Service {
    private static final int PERIOD = 600;
    private int placeId;
    private ScheduledExecutorService executor;
    private Runnable task;

    @Override
    public IBinder onBind(Intent intent) {
        executor.scheduleWithFixedDelay(task, 0, PERIOD, TimeUnit.SECONDS);
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        placeId = prefs.getInt(Const.PLACE_ID_KEY, -1);
        task = new Runnable() {
            @Override
            public void run() {
                checkNewVideos();
            }
        };
        executor = Executors.newSingleThreadScheduledExecutor();
    }


    private void checkNewVideos() {
        long bytes = 1024 * 1024 * 30; //if < 30mb space left, delete unused videos
        if (!Utils.isEnoughSpace(bytes)) {
            Utils.deleteUnusedVideos();
        }
        Utils.getPeriods(getApplicationContext(), placeId);

        Cursor c = DBHelper.getDownloadLinks();

        while (c.moveToNext()) {
            String link = c.getString(c.getColumnIndex(DBHelper.LINK));
            Log.d("LINK", link);
            String path = c.getString(c.getColumnIndex(DBHelper.PATH));
            final File f = new File(path);
            if (!f.exists()) {
                if (!Utils.isEnoughSpace(1024 * 1024 * 20)) {
                    //TODO:
                } else
                    HTTPUtils.getSync(link, null, new FileAsyncHttpResponseHandler(f) {
                        @Override
                        public void onSuccess(int i, Header[] headers, File response) {
                            Intent intent = new Intent(Const.INTENT_ACTION_NAME);
                            sendBroadcast(intent);
                        }

                        @Override
                        public void onFailure(int i, Header[] headers, Throwable throwable, File response) {
                            response.delete();
                        }
                    });
            }
        }
        Intent intent = new Intent(Const.INTENT_ACTION_NAME);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        executor.shutdown();
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }
}

