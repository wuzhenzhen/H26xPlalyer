package com.kgd.h26xplalyer;

import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    boolean isWaitingExit;
    /**
     * 连续点击两次返回退出app
     */
    public void exitApp() {
        if (isWaitingExit) {
            isWaitingExit = false;
            finish();
        } else {
            isWaitingExit = true;
            Handler handler = new Handler();
            handler.postDelayed(()-> {
                isWaitingExit = false;
            }, 3000);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeActivitAnim();
    }
    //销毁Activity动画
    public void closeActivitAnim(){
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }
    //打开新的Activity
    public void openActivitAnim(){
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }
}
