package com.kgd.h26xplalyer;

import android.app.Application;

import com.github.library.Builder;
import com.github.library.ZLog;

import java.io.File;

public class AppContext extends Application {

    private static AppContext instance;
    public static AppContext getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        initZlog();
    }

    public void initZlog(){
        // 初始化ZLog
        File file = new File(Constants.SD_APP_LOG_DIR);
        if (!file.exists()) {
            file.mkdir();
        }
        Builder builder = ZLog.newBuilder();
        builder.logSavePath(file);
//            builder.ZLogLogLevel(ZLog.NOT_SHOW_LOG);
//            builder.fileLogLevel(ZLog.NOT_SHOW_LOG);
        builder.fileOutFormat(ZLog.LOG_OUTPUT_FORMAT_3);
        builder.dbFlowStatistics(false);
        if(false){
            builder.logCatLogLevel(ZLog.SHOW_DEBUG_LOG|ZLog.SHOW_INFO_LOG| ZLog.SHOW_ERROR_LOG);  //控制台输出日志等级
            builder.fileLogLevel(ZLog.SHOW_DEBUG_LOG|ZLog.SHOW_INFO_LOG| ZLog.SHOW_ERROR_LOG);	//保存日志等级
        }else{
            builder.logCatLogLevel(ZLog.SHOW_INFO_LOG| ZLog.SHOW_ERROR_LOG);  //控制台输出日志等级
            builder.fileLogLevel(ZLog.SHOW_INFO_LOG| ZLog.SHOW_ERROR_LOG);	//保存日志等级
        }
//        Log.e("TAG", "#######VersionInfo#####"+MyAppUtil.getY_SFromLong(System.currentTimeMillis()));
//        ZLog.initialize(this, builder.build());
//        ZLog.iii("#############################VersionInfo###########");
//        ZLog.iii(MyAppUtil.getAppName(this) + " -- " + MyAppUtil.getVersionCode(this));
//        ZLog.iii("savLogAddr=" + file.getAbsoluteFile());
//        ZLog.iii("brand="+ Build.BRAND + "; model=" + Build.MODEL + "; release=" + Build.VERSION.RELEASE + "; SDK_INT=" + Build.VERSION.SDK_INT + "; display=" + Build.DISPLAY);
//        ZLog.iii("#############################VersionInfo#END#######");
    }
}
