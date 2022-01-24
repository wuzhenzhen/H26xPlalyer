package com.kgd.h26xplalyer.yuvplayer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.InputStream;

import androidx.appcompat.app.AppCompatActivity;

import com.github.library.ZLog;
import com.kgd.h26xplalyer.R;

public class YUVTestActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private MyGLSurfaceView2 mGlSurfaceView;
    private static int yuvWidth = 500;
    private static int yuvHeight = 72;
    /**
     * 读取视频流线程
     */
    public PlayYuvThread playThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkOpenGLES30()) {
            Log.e(TAG, "con't support OpenGL ES 3.0!");
            finish();
        }
        setContentView(R.layout.activity_yuvtest);
        mGlSurfaceView = findViewById(R.id.glsurfaceview);
    }

    boolean isClickAble = true;
    public void showTwo(View view){
        if (isClickAble == false){
            Toast.makeText(this,"正在播放请稍后",Toast.LENGTH_LONG).show();
            return;
        }
        isClickAble = false;
        if (playThread == null) {
            playThread = new PlayYuvThread(mGlSurfaceView);
            playThread.setResolution(yuvWidth,yuvHeight);
            autoTestVideo();
        }
        // 绘制的width必须是8的倍数，height必须是2的倍数，如果不是则需要对齐到8的倍数，否则渲染的结果不对
    }

    private boolean checkOpenGLES30() {
        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        return (info.reqGlEsVersion >= 0x30000);
    }

    @Override
    protected void onPause() {
        mGlSurfaceView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mGlSurfaceView.onResume();
        super.onResume();
    }

    //模拟测试本地YUV文件播放
    public void autoTestVideo(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//                    while (true){
                    InputStream inputStream = getAssets().open("20211027-174338.yuv");
//                    File file = new File(Environment.getExternalStorageDirectory()+"/com.kgd.h26xPlayer/20211027-174338.yuv"); //20211027-174338.yuv,yuv500_72.yuv
//                    if(file == null || file.isDirectory() || !file.exists()){
//                        return;
//                    }
//                    FileInputStream inputStream = new FileInputStream(file);
//                    int count = 0;
                    if(inputStream != null){
                        byte[] readBytes = new byte[1000];
                        while (inputStream.read(readBytes)>0){
                            playThread.addYuvData4(readBytes);
//                            ZLog.ddd("--autoTestVideo--"+count++,TAG);
                            Thread.sleep(3);
                        }
                        inputStream.close();
                    }
                    Thread.sleep(30);
//                    }
                } catch (Exception e) {
                    System.out.println("读取文件内容出错");
                    ZLog.eee("--testFrame--e="+e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }
}