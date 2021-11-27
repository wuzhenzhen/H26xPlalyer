package com.kgd.h26xplalyer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.File;
import java.io.FilenameFilter;

public class MainActivity extends BaseActivity {

    private static final String TAG = "H26xPlayer";
    private static final int REQUEST_CODE = 1024;
    public static final String MOVIES_DIR = Environment.getExternalStorageDirectory().toString() + "/com.tiamaes.tm8765";
    private String[] permiss = {"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
    private String[] fileList;


    private ListView lvFile;
    private ArrayAdapter arrayAdapter;
    private RadioButton rb_h264,rb_h265,rb_codeStream1,rb_codeStream2;
    private RadioGroup rg_h26x,rg_codeStream;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermiss();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
            }
        }
        initView();
    }
    private void checkPermiss() {
        int code = ActivityCompat.checkSelfPermission(this, permiss[0]);
        if (code != PackageManager.PERMISSION_GRANTED) {
            // 没有写的权限，去申请写的权限
            ActivityCompat.requestPermissions(this, permiss, 11);
        }
    }

    private void initView(){
//        private RadioButton rb_h264,rb_h265,rb_codeStream1,rb_codeStream2;
//        private RadioGroup rg_h26x,rg_codeStream;
        rb_h264 = findViewById(R.id.rb_h264);
        rb_h265 = findViewById(R.id.rb_h265);
        rb_codeStream1 = findViewById(R.id.rb_codeStream1);
        rb_codeStream2 = findViewById(R.id.rb_codeStream2);
        rg_h26x = findViewById(R.id.rg_h26x);
        rg_codeStream = findViewById(R.id.rg_codeStream);

        lvFile = findViewById(R.id.lvVideo);
        loadMediaFile();
    }
    public void loadMediaFile() {
        File file = new File(MOVIES_DIR);
        fileList = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".h264") || name.endsWith("h265")||name.endsWith("hevc")) {
                    return true;
                }
                return false;
            }
        });
        if(fileList == null || fileList.length<=0){
            return;
        }
        arrayAdapter = new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1,fileList);
        lvFile.setAdapter(arrayAdapter);
        lvFile.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String path = MOVIES_DIR + "/" + fileList[position];
                Log.d(TAG, "startPlayVideo: path = " + path);

                if(rb_codeStream1.isChecked()){ //裸码流
                    String H26x = "h264";
                    if(rb_h264.isChecked()){
                        H26x = "h264";
                    }else if (rb_h265.isChecked()){
                        H26x = "h265";
                    }
                    Intent intent = new Intent(MainActivity.this,H26XActivity.class);
                    intent.putExtra("path",path);
                    intent.putExtra("h26x",H26x);
                    startActivity(intent);
                }else if (rb_codeStream2.isChecked()){  //8765设备发过来的原始文件 包含协议头
                    Intent intent = new Intent(MainActivity.this,TM8765Activity.class);
                    intent.putExtra("path",path);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
            } else {
                System.out.println("存储权限获取失败");
            }
            loadMediaFile();
        }
    }

    @Override
    public void onBackPressed() {
        exitApp();
    }
}