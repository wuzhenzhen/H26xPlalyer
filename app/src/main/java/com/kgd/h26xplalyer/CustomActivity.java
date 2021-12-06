package com.kgd.h26xplalyer;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;

/**
 *  定制view，主要用于展示surface view 全屏与半屏切换
 *  有外层协议
 */
public class CustomActivity extends BaseActivity{
    private static final String TAG = "CustomActivity";
    private String videoPath;
    PlayH26xThread playH26xThread;
    private ConstraintLayout.LayoutParams mInitVideoViewLp;
    private CustomVideoView mVideoView;
    private int mCurrPlayPos=65;//当前视频已播放时长
    private int mVideoDuration=600;//当前视频总时长
    CustomVideoView.VideoListener listener;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom);
        videoPath = getIntent().getStringExtra("path");
        initView();
    }

    private void initView() {
        mVideoView = findViewById(R.id.surface);
        mInitVideoViewLp = (ConstraintLayout.LayoutParams) mVideoView.getLayoutParams();
        //set duration first
        if(mVideoDuration != 0) {
            mVideoView.setDuration(mVideoDuration);
        }
        //and then set curr
        if(mCurrPlayPos != 0) {
            mVideoView.setCurrPosition(mCurrPlayPos);
        }
        listener = new CustomVideoView.VideoListener() {
            @Override
            public void onVideoStarted() {
                super.onVideoStarted();
            }

            @Override
            public void onVideoStopped() {
                super.onVideoStopped();
            }

            @Override
            public void onVideoCompleted() {
            }

            @Override
            public void onZoomPressed() {
                if (!isLand()) {
                    //to full screen mode
                    showFullScreenMode();
                } else {
                    // to normal mode
                    showPortraitMode();
                }
            }

            @Override
            public void onBackPressed() {
                if (!isLand()) {
                    //stop and finish
                    finish();
                } else {
                    // to normal mode
                    showPortraitMode();
                }
            }
        };
        mVideoView.setVideoListener(listener);


        final SurfaceView surface = mVideoView.getSurfaceView();
//        final SurfaceView surface = (SurfaceView) findViewById(R.id.surface);
        final SurfaceHolder holder = surface.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                if (playH26xThread == null) {
                    playH26xThread = new PlayH26xThread(surfaceHolder.getSurface());
                }
                autoTestVideo();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

            }
        });
    }

    //模拟测试本地H264文件播放
    public void autoTestVideo(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File file = new File(videoPath);
                    if(file == null || file.isDirectory() || !file.exists()){
                        return;
                    }
                    FileInputStream inputStream = new FileInputStream(file);
                    if(inputStream != null){
                        byte[] readBytes = new byte[2048];
                        while (inputStream.read(readBytes)>0){
                            if (playH26xThread != null) {
                                playH26xThread.addVideoData(readBytes);
                                Log.d("TAG","--autoTestVideo--"+readBytes.length);
                            }
                        }
//                        long fileLength = file.length();
//                        int frameLength = 0xC30;
//                        int packageCount = (int)(fileLength/frameLength);
//                        int packageMod = (int)(fileLength % frameLength);
//                        if( packageMod != 0){
//                            packageCount ++;
//                        }
//
//                        byte[] datas;
//                        for(int i=0; i < packageCount; i++){
//                            if(i == packageCount-1){
//                                 datas = new byte[packageMod];
//                                inputStream.read(datas);
//                            }else{
//                                datas = new byte[frameLength];
//                                inputStream.read(datas);
//                            }
////                            ZLog.iii("--autoTestVideo--"+i+"/"+packageCount);
//                            onMessageResponse(datas);
//                            Thread.sleep(30);
//                        }
                        inputStream.close();
                    }
                } catch (Exception e) {
                    System.out.println("读取文件内容出错");
                    Log.e("TAG","--testFrame--e="+e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 是否横屏
     *
     * @return
     */
    private boolean isLand() {
        return getResources().getConfiguration()
                .orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private void showFullScreenMode() {
        //记录当前播放的片段
        //记录当前播放时间
        mCurrPlayPos = mVideoView.getCurrentPosition();
        mVideoDuration = mVideoView.getDuration();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        findViewById(R.id.ll_top).setVisibility(View.GONE);
        findViewById(R.id.ll_bottom).setVisibility(View.GONE);
    }

    private void showPortraitMode() {
        //记录当前播放的片段
        //记录当前播放时间
        mCurrPlayPos = mVideoView.getCurrentPosition();
        mVideoDuration = mVideoView.getDuration();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        findViewById(R.id.ll_top).setVisibility(View.VISIBLE);
        findViewById(R.id.ll_bottom).setVisibility(View.VISIBLE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //设置横屏的UI
            Log.d(TAG, "onConfigurationChanged ORIENTATION_LANDSCAPE");
            //set full screen mode
            int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE;
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            ViewGroup vp = (ViewGroup) this.findViewById(Window.ID_ANDROID_CONTENT);
            try{
                Constructor<CustomVideoView> constructor = CustomVideoView.class.getConstructor(Context.class);
                CustomVideoView customVideoView = constructor.newInstance(this.getApplicationContext());
                customVideoView.setId(R.id.zq_fullscreen_id);
                customVideoView.setVideoListener(listener);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
//                lp.bottomMargin=200;
//                lp.topMargin=200;
//                lp.leftMargin=300;
//                lp.rightMargin=300;
                vp.addView(customVideoView,lp);
                System.out.println("---");
            }catch (Exception e){

            }

        } else {
            //设置竖屏的UI
            Log.d(TAG, "onConfigurationChanged ORIENTATION_PORTRAIT");
            //exit from full screen
            getWindow().getDecorView().setSystemUiVisibility(0);
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(attrs);

//            mInitVideoViewLp.width = LinearLayout.LayoutParams.MATCH_PARENT;
//            mInitVideoViewLp.height = 0;
//            mVideoView.setLayoutParams(mInitVideoViewLp);
//            mVideoView.setZoomIco(R.drawable.ic_video_zoom_in);

            ViewGroup vp = (ViewGroup) this.findViewById(Window.ID_ANDROID_CONTENT);
            View old = vp.findViewById(R.id.zq_fullscreen_id);
            if (old != null) {
                vp.removeView(old);
            }
            System.out.println("---");
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (!isLand()) {
            //stop and finish
            finish();
        } else {
            // to normal mode
            showPortraitMode();
        }
    }
}
