package com.nasctech.ucun.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.nasctech.ucun.R;
import com.nasctech.ucun.utils.Const;
import com.nasctech.ucun.utils.DBHelper;
import com.nasctech.ucun.utils.HTTPUtils;
import com.nasctech.ucun.utils.Utils;
import com.nasctech.ucun.view.HomeHackDialog;
import com.nasctech.ucun.view.StatusBarHackView;
import com.squareup.picasso.Picasso;

import org.apache.http.Header;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static final int COUNT_TO_CLOSE = 5;
    private static final int COUNT_OPEN_LOGIN_ACTIVITY = 5;
    private static final String BACKGROUND_IMAGE_FILE_NAME = "/background.png";
    private static final String VIDEO = "/animation.mp4";
    private static final long INTERVAL = 3 * 60 * 1000;
    private int countLeftBtn = 0, countRightBtn = 0;
    private Button btnLeft, btnRight;
    private ImageButton btnStartPlayer;
    private String path;
    private ImageView background;
    private VideoView videoView;
    private View statusBarView;
    private HomeHackDialog mHomeHackDialog;
    private Loader loader;
    private SharedPreferences prefs;
    private Timer timer;

    @Override
    public void onBackPressed() {
        if (timer != null) {
            timer.cancel();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        DBHelper.init(getApplicationContext());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        findView();
        init();

        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countLeftBtn++;
                if (countLeftBtn == COUNT_TO_CLOSE) {
                    finish();
                    if (timer != null) {
                        timer.cancel();
                    }
                }
            }
        });

        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countRightBtn++;
                if (countRightBtn == COUNT_OPEN_LOGIN_ACTIVITY) {
                    finish();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    if (timer != null) {
                        timer.cancel();
                    }
                }
            }
        });
        btnStartPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                String login = prefs.getString(Const.LOGIN_KEY, "");
                String password = prefs.getString(Const.PASS_KEY, "");

                if (!login.isEmpty() && !password.isEmpty()) {
                    finish();
                    startActivity(new Intent(MainActivity.this, PlayerActivity.class));
                    if (timer != null) {
                        timer.cancel();
                    }
                } else {
                    finish();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    if (timer != null) {
                        timer.cancel();
                    }
                }
            }
        });

        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    String login = prefs.getString(Const.LOGIN_KEY, "");
                    String password = prefs.getString(Const.PASS_KEY, "");
                    final int placeID = prefs.getInt(Const.PLACE_ID_KEY, -1);

                    if (!login.isEmpty() && !password.isEmpty() && placeID != -1) {
                        finish();
                        startActivity(new Intent(MainActivity.this, PlayerActivity.class));
                    }
                }
            }, INTERVAL);
        }

        Utils.setPaths(this);
        DBHelper.init(getApplicationContext());
        deleteVideoOnFirstLaunch();
    }

    //disable popup windows (on power key button)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
            Log.d("onFocus", "closed smth");
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                String login = prefs.getString(Const.LOGIN_KEY, "");
                String password = prefs.getString(Const.PASS_KEY, "");
                final int placeID = prefs.getInt(Const.PLACE_ID_KEY, -1);

                if (!login.isEmpty() && !password.isEmpty() && placeID != -1) {
                    finish();
                    startActivity(new Intent(MainActivity.this, PlayerActivity.class));
                }
            }
        }, INTERVAL);
    }

    private void findView() {
        btnLeft = (Button) findViewById(R.id.btn_left);
        btnRight = (Button) findViewById(R.id.btn_right);
        btnStartPlayer = (ImageButton) findViewById(R.id.btn_start_menu);
        videoView = (VideoView) findViewById(R.id.video_view);
        background = (ImageView) findViewById(R.id.background);
    }

    private void init() {
        countLeftBtn = 0;
        countRightBtn = 0;
        path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Ucun";

        makeDefaultDir();

        disableStatusBar();
        setScreenAlwaysOn();

        loadImage(background, R.drawable.background, path + BACKGROUND_IMAGE_FILE_NAME);

        loadVideo(path + VIDEO);
    }

    private void makeDefaultDir() {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    private void loadVideo(String path) {
        if (isFileExists(path)) {
            videoView.setVideoPath(path);
        } else {
            videoView.setVideoPath("android.resource://com.nasctech.ucun/" + R.raw.animation);
        }

        videoView.requestFocus();
        videoView.start();

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                videoView.start();
            }
        });
    }

    private void setScreenAlwaysOn() {
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void loadImage(ImageView imageView, int resource, String path) {
        if (isFileExists(path)) {
            Picasso.with(MainActivity.this).load(new File(path)).placeholder(resource).into(imageView);
        } else {
            Picasso.with(MainActivity.this).load("android.resource://com.nasctech.ucun/" + resource)
                    .placeholder(resource).into(imageView);
        }
    }

    private boolean isFileExists(String path) {
        return new File(path).exists();
    }

    @Override
    protected void onResume() {
        super.onResume();
        lock();
        init();

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disableBlock();
    }

    private void disableStatusBar() {
        WindowManager manager = ((WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE));

        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
        localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        localLayoutParams.gravity = Gravity.TOP;
        localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                // this is to enable the notification to receive touch events
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                // Draws over status bar
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        localLayoutParams.height = (int) (50 * getResources()
                .getDisplayMetrics().scaledDensity);
        localLayoutParams.format = PixelFormat.TRANSPARENT;

        StatusBarHackView view = new StatusBarHackView(this);
        statusBarView = view;
        manager.addView(view, localLayoutParams);
    }

    private void disableBlock() {
        WindowManager manager = ((WindowManager) getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE));
        if (statusBarView != null && statusBarView.isShown())
            manager.removeView(statusBarView);
        if (mHomeHackDialog != null) {
            mHomeHackDialog.dismiss();
            mHomeHackDialog = null;
        }
    }

    private void lock() {
        if (mHomeHackDialog == null) {
            mHomeHackDialog = new HomeHackDialog(this);
            mHomeHackDialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        disableBlock();

        if (timer != null) {
            timer.cancel();
        }
    }

    private void deleteVideoOnFirstLaunch() {
        if (DBHelper.getAllVideos().isEmpty()) {
            File dir = new File(Utils.PATH);
            File[] files = dir.listFiles();
            for (File file : files)
                file.delete();
        }
    }

    public void login(View view) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String login = prefs.getString(Const.LOGIN_KEY, "");
        String password = prefs.getString(Const.PASS_KEY, "");
        String url = Utils.SERVER_URL + "/api/get_client_token.json";
        final int placeID = prefs.getInt(Const.PLACE_ID_KEY, -1);

        HTTPUtils.post(url, createParams(login, password), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                if (statusCode == 200) {
                    if (placeID != -1) {
                        loader = new Loader();
                        loader.execute(placeID);
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Toast.makeText(MainActivity.this, "Connection problems", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject object) {
                super.onFailure(statusCode, headers, throwable, object);
                Toast.makeText(MainActivity.this, "Connection problems", Toast.LENGTH_SHORT).show();
                throwable.printStackTrace();
            }
        });
    }

    private Map<String, String> createParams(String login, String pass) {
        Map<String, String> map = new HashMap<>();
        map.put("email", login);
        map.put("password", pass);
        return map;
    }

    private class Loader extends AsyncTask<Integer, Integer, String> {
        @Override
        protected String doInBackground(Integer... params) {
            download(params);
            return null;
        }

        private void download(Integer... params) {
            long bytes = 1024 * 1024 * 30; //if < 30mb space left, delete unused videos
            if (!Utils.isEnoughSpace(bytes)) {
                Utils.deleteUnusedVideos();
            }
            Utils.getPeriods(getApplicationContext(), params[0]);

            Cursor cursor = DBHelper.getDownloadLinks();

            while (cursor.moveToNext()) {
                String link = cursor.getString(cursor.getColumnIndex(DBHelper.LINK));
                String path = cursor.getString(cursor.getColumnIndex(DBHelper.PATH));
                final File file = new File(path);
                if (!file.exists()) {
                    if (!Utils.isEnoughSpace(1024 * 1024 * 20)) {
                        //TODO do something if no space
                    } else
                        HTTPUtils.getSync(link, null, new FileAsyncHttpResponseHandler(file) {
                            @Override
                            public void onSuccess(int i, Header[] headers, File response) {
                            }

                            @Override
                            public void onFailure(int i, Header[] headers, Throwable throwable, File response) {
                                cancel(true);
                                response.delete();
                            }
                        });
                }
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            loader = null;
        }

        @Override
        protected void onCancelled(String s) {
            super.onCancelled(s);
            Toast.makeText(MainActivity.this, "Connection problems, please try again", Toast.LENGTH_SHORT).show();
        }
    }
}
