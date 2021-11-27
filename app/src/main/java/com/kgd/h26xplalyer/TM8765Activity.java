package com.kgd.h26xplalyer;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;

/**
 * TM8765 设备，数据里包含头信息
 */
public class TM8765Activity extends BaseActivity{

    private String videoPath;
    PlayH26xThread playH26xThread;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h26x);
        videoPath = getIntent().getStringExtra("path");
        initView();
    }

    private void initView() {
        final SurfaceView surface = (SurfaceView) findViewById(R.id.surface);
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
}
