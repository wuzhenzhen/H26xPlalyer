package com.kgd.h26xplalyer;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


public class H26XActivity extends AppCompatActivity {

    private H264DeCodePlay h264DeCodePlay;
    private H265DeCodePlay h265DeCodePlay;
    private String videoPath;
    private String h26x;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h26x);
        h26x = getIntent().getStringExtra("h26x");
        videoPath = getIntent().getStringExtra("path");
        initView();
    }



    private void initView() {
        final SurfaceView surface = (SurfaceView) findViewById(R.id.surface);
        final SurfaceHolder holder = surface.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                if("h264".equals(h26x)){
                    h264DeCodePlay = new H264DeCodePlay(videoPath, holder.getSurface());
                    h264DeCodePlay.decodePlay();
                }else if ("h265".equals(h26x)){
                    h265DeCodePlay = new H265DeCodePlay(videoPath, holder.getSurface());
                    h265DeCodePlay.decodePlay();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

            }
        });
    }
    @Override
    public void onBackPressed() {
        this.finish();
    }
}
