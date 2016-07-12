package com.nasctech.ucun.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;

import com.nasctech.ucun.R;
import com.nasctech.ucun.adapters.GridAdapter;
import com.nasctech.ucun.utils.Const;
import com.nasctech.ucun.utils.DBHelper;
import com.nasctech.ucun.utils.GridClickListener;
import com.nasctech.ucun.utils.Utils;
import com.nasctech.ucun.utils.VideoPollingService;
import com.nasctech.ucun.view.FullScreenVideoView;
import com.nasctech.ucun.view.HomeHackDialog;
import com.nasctech.ucun.view.StatusBarHackView;

import java.util.List;

public class PlayerActivity extends Activity implements GridClickListener {
    //    private static final int HIDE_TIME = 5000;
    private FullScreenVideoView videoPlayer;
    private View bottomView;
    private View centerView;
    private View statusBarView;
    private String videoName;
    private String videoPath;
    private int videoId = -1;
    private int exitCounter = 0;
    private GridAdapter adapter;
    private HomeHackDialog mHomeHackDialog;
    private BroadcastReceiver newVideosReceiver;
    private ServiceConnection connection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Player", "onCreate");
        if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
            return;
        setContentView(R.layout.player);

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
    }

    private void setScreenAlwaysOn() {
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void registerVideosReceiver() {
        newVideosReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                initList();
            }
        };
        IntentFilter filter = new IntentFilter(Const.INTENT_ACTION_NAME);
        registerReceiver(newVideosReceiver, filter);
    }

    //lock home button
    private void lock() {
        if (mHomeHackDialog == null) {
            mHomeHackDialog = new HomeHackDialog(this);
            mHomeHackDialog.show();
        }
    }

    //disable back button
    @Override
    public void onBackPressed() {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Player", "onDestroy");
        if (mHomeHackDialog != null)
            mHomeHackDialog.dismiss();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Player", "onStop");
        if (newVideosReceiver != null) {
            unregisterReceiver(newVideosReceiver);
            newVideosReceiver = null;
        }
        unbindService(connection);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Player", "onPause");
        disableBlock();

        if (videoPlayer.isPlaying()) {
            pauseOrPlay(null);
        } else {
            showOrHideGrid();
            videoPlayer.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        lock();
        disableStatusBar();
        init();
        showOrHideGrid();
        Log.d("Player", "onResume");
    }

    @Override
    protected void onStart() {
        super.onStart();
        DBHelper.init(getApplicationContext());
        //start from last position after quit?
        Log.d("PLAYER", "onStart");
        registerVideosReceiver();
        bindService(new Intent(this, VideoPollingService.class), connection, Context.BIND_AUTO_CREATE);
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

    private void initList() {
        List<Integer> videos = DBHelper.getVideoByDates();
        if (videos.isEmpty()) {
            videos = DBHelper.getAllVideos();
        }
        adapter.setList(videos);
    }

    private void init() {
        videoPlayer = (FullScreenVideoView) findViewById(R.id.surfaceView);
        bottomView = findViewById(R.id.bottomView);
        GridView gridView = (GridView) findViewById(R.id.gridView);
        adapter = new GridAdapter(this);
        initList();
        gridView.setAdapter(adapter);

        setScreenAlwaysOn();

        View root = findViewById(R.id.root);

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerId(getIndex(event)) == 4) {
                    exitCounter++;
                    if (exitCounter > 6) {
                        exitCounter = 0;
                        showExitDialog();
                    }
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        showOrHideBottom();
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:

                        break;
                }
                return true;
            }
        });


        centerView = findViewById(R.id.centerView);
    }

    private int getIndex(MotionEvent event) {
        return (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
    }

    @Override
    public void onGridClick(Integer id) {
        videoName = DBHelper.getVideoNameById(id);
        videoPath = Utils.PATH + videoName;
        videoId = id;
        showOrHideGrid();
        changePlayDrawable();
        playVideo();
    }

    public void repeat(View v) {
        if (centerView.getVisibility() == View.VISIBLE) {
            showOrHideGrid();
            changePlayDrawable();
        }

        if (videoId != -1) {
            DBHelper.addReplay(videoId);
            playVideo();
        }
    }

    private void changePlayDrawable() {
        ImageView im = (ImageView) findViewById(R.id.playPauseView);
        if (videoPlayer.isPlaying())
            im.setImageResource(android.R.drawable.ic_media_play);
        else
            im.setImageResource(android.R.drawable.ic_media_pause);
    }

    public void pauseOrPlay(View v) {
        changePlayDrawable();
        if (videoPlayer.isPlaying()) {
            showOrHideGrid();
            videoPlayer.pause();
        } else {
            if (centerView.getVisibility() == View.VISIBLE) {
                showOrHideGrid();
            }
            videoPlayer.setVisibility(View.VISIBLE);
            videoPlayer.start();
        }
    }

    public void showMenu(View v) {
        finish();
        startActivity(new Intent(PlayerActivity.this, MainActivity.class));
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_quit, null);
        //disable dialog to be able to use keyboard
        if (mHomeHackDialog != null)
            mHomeHackDialog.dismiss();
        final EditText passwordText = (EditText) view.findViewById(R.id.password);
        builder.setView(view);

        builder.setTitle("CONFIRM QUIT");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (passwordText.getText().toString().equals("1919"))
                    PlayerActivity.this.finish();
                else {
                    InputMethodManager imm = (InputMethodManager) PlayerActivity.this.getSystemService(Service.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(passwordText.getWindowToken(), 0);
                    mHomeHackDialog = new HomeHackDialog(PlayerActivity.this);
                    mHomeHackDialog.show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                InputMethodManager imm = (InputMethodManager) PlayerActivity.this.getSystemService(Service.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(passwordText.getWindowToken(), 0);
                mHomeHackDialog = new HomeHackDialog(PlayerActivity.this);
                mHomeHackDialog.show();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void playVideo() {
        videoPlayer.setVideoPath(videoPath);
        videoPlayer.setVisibility(View.VISIBLE);
        videoPlayer.setOnPreparedListener(new io.vov.vitamio.MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(io.vov.vitamio.MediaPlayer mp) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                videoPlayer.setVideoWidth(mp.getVideoWidth());
                videoPlayer.setVideoHeight(mp.getVideoHeight());
                videoPlayer.start();
            }
        });

        videoPlayer.setOnCompletionListener(new io.vov.vitamio.MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(io.vov.vitamio.MediaPlayer mp) {
                showOrHideGrid();
                showOrHideBottom();
                changePlayDrawable();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        int id = prefs.getInt("place_id", 1);
                        DBHelper.addView(videoId);
                        Log.d("statistics id ", id + "");
                        Utils.sendStatistics(getApplicationContext(), id); //for video
                        DBHelper.deleteViews();
                        //clear statistics after sending
                    }
                });
                thread.start();
            }
        });
    }

    private void showOrHideGrid() {
        if (centerView.getVisibility() == View.VISIBLE) {
            centerView.clearAnimation();
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.option_leave_from_top);
            animation.setAnimationListener(new AnimationImp() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    super.onAnimationEnd(animation);
                    centerView.setVisibility(View.INVISIBLE);
                }
            });
            centerView.startAnimation(animation);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            centerView.setVisibility(View.VISIBLE);
            centerView.clearAnimation();
            Animation animation1 = AnimationUtils.loadAnimation(this, R.anim.option_entry_from_top);
            centerView.startAnimation(animation1);
        }
    }

    private void showOrHideBottom() {
        if (bottomView.getVisibility() == View.VISIBLE) {
            bottomView.clearAnimation();
            Animation animation = AnimationUtils.loadAnimation(this,
                    R.anim.option_leave_from_bottom);
            animation.setAnimationListener(new AnimationImp() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    super.onAnimationEnd(animation);
                    bottomView.setVisibility(View.INVISIBLE);
                }
            });
            bottomView.startAnimation(animation);
        } else {
            bottomView.setVisibility(View.VISIBLE);
            bottomView.clearAnimation();
            Animation animation1 = AnimationUtils.loadAnimation(this,
                    R.anim.option_entry_from_bottom);
            bottomView.startAnimation(animation1);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        GridView grid = (GridView) findViewById(R.id.gridView);
        videoPlayer.setVideoLayout(io.vov.vitamio.widget.VideoView.VIDEO_LAYOUT_STRETCH, 0);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            grid.setNumColumns(3);
            Log.e("On Config Change", "LANDSCAPE");
        } else {
            grid.setNumColumns(2);
            Log.e("On Config Change", "PORTRAIT");
        }
    }

    private class AnimationImp implements Animation.AnimationListener {

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }

    }
}
