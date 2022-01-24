package com.kgd.h26xplalyer.yuvplayer;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;

import com.github.library.ZLog;
import com.kgd.h26xplalyer.AppContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

public class PlayYuvThread extends Thread{
    final String TAG = "PlayYuvThread";
    public boolean isThreadRuning = false;
    MyGLSurfaceView2 myGLSurfaceView;
    //500*72,320*160,160*100
    private int yuvWidth = 320;
    private int yuvHeight = 160;
    int LENGTH = yuvWidth*yuvHeight ;
    byte[] y = new byte[LENGTH];
    //yuv有效数据队列
    private final LinkedBlockingQueue<YUVDataBuf> mFramequeue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<byte[]> mFramequeueByte = new LinkedBlockingQueue<>();

    public static int playMode = 3; // 1 为解析裸流；  2 为解析带包头的流; 3解析裸流
    private boolean isClear = false;

    public PlayYuvThread(MyGLSurfaceView2 glSurfaceView) {
        this.myGLSurfaceView = glSurfaceView;
        frameFlagRun2 = 0;
        start();
    }

    public void setResolution(int width,int height){
        this.yuvWidth = width;
        this.yuvHeight = height;
        LENGTH = yuvWidth * yuvHeight;
        y = new byte[LENGTH];
    }

    @Override
    public void start() {
        if (!isThreadRuning) {
            isThreadRuning = true;
            super.start();
        }
    }

    @Override
    public void interrupt() {
        ZLog.iii(TAG+":interrupt");
        super.interrupt();
        isThreadRuning = false;
    }

    @Override
    public void run() {
        if(!isClear){
            clearYuvJpg();
            isClear = true;
        }
        if(playMode == 1){
            run1();
        }else if (playMode == 2){
//            run2();
            run3();
        }else if (playMode == 3){
            run4();
        }
    }

    /**
     *停止解码，释放解码器
     */
    public void stopRun() {
        ZLog.iii("PlayH26xThread:stopRun");
        interrupt();
        clearVideoFile();
        clearGlSurface();
        frame3.length = 0;
    }

    // 清空画面，显示黑屏
    private void clearGlSurface(){
        if(myGLSurfaceView != null){
            myGLSurfaceView.clearGlSurfaceView();
        }
    }
    /**
     * 清除保存的视频流文件
     * @return
     */
    public static void clearVideoFile(){
//        try {
//            OutputStream outputStream = new FileOutputStream(VIDEO_FILE_NAME);//AppContext.getInstance().openFileOutput(VIDEO_FILE_NAME,Context.MODE_PRIVATE);
//            outputStream.write(0);
//            outputStream.flush();
//            outputStream.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }finally {
//        }
    }

    int flag = 0;
    int count = 0; //一帧含有几包数据
    int frameFlag = 0;  //第几帧数据
    YUVDataBuf frame3 = new YUVDataBuf();
    public void addYuvData3(byte[] buf){
        try{
            flag++;
            count ++;
            ZLog.ddd(TAG+"NettyManagerTrafficFlow视频数据长度:1=="+flag+","+buf.length+","+count);
            if(frame3.length == 0){
                frame3.buffer = new byte[LENGTH];
            }
            if(buf.length >= LENGTH || buf.length + frame3.length >= LENGTH){
                int length2 = LENGTH - frame3.length;
                System.arraycopy(buf,0,frame3.buffer,frame3.length,length2);
                length2 = buf.length - length2;
                frame3.length = LENGTH;
                mFramequeue.put(frame3);
                count = 0;
                frameFlag ++;
                ZLog.ddd(TAG+"NettyManagerTrafficFlow视频数据长度:112=="+frameFlag+","+flag+","+frame3.length+","+frame3.buffer.length+","+count);
                frame3.length = 0;
                if(length2 > 0){
                    frame3.buffer = new byte[LENGTH];
                    ZLog.ddd(TAG+"NettyManagerTrafficFlow视频数据长度:113=="+flag+","+frame3.length+","+frame3.buffer.length+","+length2);
                    System.arraycopy(buf,buf.length-length2,frame3.buffer,frame3.length,length2);
                    frame3.length += length2;
                    count ++;
                }
            }else{
                System.arraycopy(buf,0,frame3.buffer,frame3.length,buf.length);
                frame3.length += buf.length;
            }
        }catch ( Exception e){
            e.printStackTrace();
        }
    }

    private static int frameFlagRun2 = 0;
    public void run1(){
        YUVDataBuf frame;
        CompositeByteBuf buf = Unpooled.compositeBuffer();
        while (isThreadRuning) {
            try{
                frame = mFramequeue.take();
                if (frame == null || frame.buffer == null) {
                    break;
                }
                buf.writeBytes(frame.buffer, 0, frame.length);
                ZLog.ddd(TAG+"NettyManagerTrafficFlow视频数据长度:114#="+frameFlagRun2);
                if (buf.capacity() >= LENGTH && (buf.isReadable(LENGTH))) {
                    buf.readBytes(y, 0, LENGTH);
                    buf.discardSomeReadBytes();
                    frameFlagRun2 ++;
                    if(frameFlagRun2 == 1 || frameFlagRun2 % 10 == 0){
                        saveYUV2Bitmap(y,yuvWidth,yuvHeight,frameFlagRun2);
                    }
                    ZLog.ddd(TAG+"NettyManagerTrafficFlow视频数据长度:114=="+frameFlagRun2);
                    myGLSurfaceView.refresh(ByteBuffer.wrap(y),yuvWidth,yuvHeight);
                }
            }catch (Exception e){
                ZLog.eee(TAG+"==e=="+e.getLocalizedMessage());
            }
        }
    }

    class YUVDataBuf {
        byte[] buffer;
        int length = 0;
    }
//--------------------2---------------
    public void addYuvData4(byte[] buf){
        try {
            mFramequeueByte.put(buf);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run4(){
        CompositeByteBuf buf = Unpooled.compositeBuffer();
        while (isThreadRuning) {
            try{
                byte[] frame = mFramequeueByte.take();
                if (frame == null || frame.length <= 0) {
                    break;
                }
                buf.writeBytes(frame, 0, frame.length);
                ZLog.ddd(TAG+"NettyManagerTrafficFlow视频数据长度:114#="+frameFlagRun2);
                if (buf.capacity() >= LENGTH && (buf.isReadable(LENGTH))) {
                    buf.readBytes(y, 0, LENGTH);
                    buf.discardSomeReadBytes();
                    frameFlagRun2 ++;
                    if(frameFlagRun2 == 1 || frameFlagRun2 % 10 == 0){
                        saveYUV2Bitmap(y,yuvWidth,yuvHeight,frameFlagRun2);
                    }
                    ZLog.ddd(TAG+"NettyManagerTrafficFlow视频数据长度:114=="+frameFlagRun2);
                    myGLSurfaceView.refresh(ByteBuffer.wrap(y),yuvWidth,yuvHeight);
                }
            }catch (Exception e){
                ZLog.eee(TAG+"==e=="+e.getLocalizedMessage());
            }
        }
    }


    //----------------处理协议---------处理协议-----------------------------
    final int BYTE_LENGTH = 0x40000;
    YUVDataBuf playBuf = new YUVDataBuf();
    public void addYuvData(byte[] buf)  {
        ZLog.ddd("NettyManagerTrafficFlow视频数据长度:"+buf.length+","+buf[0]+","+buf[1]+","+buf[2]+","+buf[3]);
        int count = buf.length % BYTE_LENGTH  == 0 ? buf.length/BYTE_LENGTH : buf.length/BYTE_LENGTH +1;
        int surplusLength = buf.length % BYTE_LENGTH;    //剩余的长度
        for (int i = 0; i < count; i++) {
            YUVDataBuf frame = new YUVDataBuf();
            if (buf.length <= BYTE_LENGTH) {
                frame.buffer = new byte[buf.length];
                System.arraycopy(buf, 0, frame.buffer, 0, buf.length);
            }else{
                if (i == count -1){
                    //最后一个长度为余下的数据
                    frame.buffer = new byte[surplusLength];
                    System.arraycopy(buf, i * BYTE_LENGTH, frame.buffer, 0, surplusLength);
                }else {
                    frame.buffer = new byte[BYTE_LENGTH];
                    System.arraycopy(buf, i * BYTE_LENGTH, frame.buffer, 0, BYTE_LENGTH);
                }
            }
            frame.length = buf.length;
            try {
                mFramequeue.put(frame);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *  连续读取 width*height 个字节给 glsurfaceview 播放，
     *  缺馅： 可能存在一帧出错后续都出错
     */
    public void run2(){
        int packageLength = 0;
        boolean foundHeadr = false;
        YUVDataBuf frame = new YUVDataBuf();
        CompositeByteBuf buf = Unpooled.compositeBuffer();
        while (isThreadRuning) {
            try {
                frame = mFramequeue.take();
                if (frame == null || frame.buffer == null) {
                    break;
                }
                buf.writeBytes(frame.buffer, 0, frame.length);

                while (true){
                    if (!foundHeadr) {
                        foundHeadr = (packageLength = syncHeader(buf)) > 0;
                    }
                    if (foundHeadr) {
                        if (buf.isReadable(packageLength)) {
                            foundHeadr = false;
                            byte[] yuvData = new byte[packageLength];
                            buf.readBytes(yuvData, 0, packageLength);
                            buf.discardSomeReadBytes();
                            if(playBuf.length < LENGTH){
                                if(playBuf.buffer == null){
                                    playBuf.buffer = new byte[LENGTH];
                                }
                                System.arraycopy(yuvData,0,playBuf.buffer,playBuf.length,yuvData.length);
                                playBuf.length += yuvData.length;
                                if(playBuf.length >= LENGTH){
                                    myGLSurfaceView.refresh(ByteBuffer.wrap(playBuf.buffer),yuvWidth,yuvHeight);
                                    ZLog.ddd("--PlayYuvThread##:"+packageLength+","+playBuf.length);
                                    playBuf.length = 0;
                                }
                            }
                        }else{
                            ZLog.ddd("--PlayYuvThread##不满足条件1: buf数据小于packageLength:"+packageLength);
                            break;
                        }
                    }else{
                        ZLog.ddd("--PlayYuvThread##不满足条件2:header: 没有发现header");
                        break;
                    }
                }

            }catch (Exception e){
                ZLog.eee("--PlayYuvThread##e=="+e.getLocalizedMessage());
            }
        }
    }

    /**
     * 依据协议读取 该帧的所有数据，如果此帧数据长度不为 width*height 则丢弃 --此种处理更优一点
     * 数据10个字节： 4个####+2个字节长度+2个字节帧号+1个字节当前帧总包数+1个字节包序号  -- 长度是包含(包头，帧号，帧包数，帧包序号)
     * 依据帧号，当前帧包数，当前帧包序号 读取完整一帧，不完整的丢弃
     */
    public void run3(){
        int packageLength = 0;
        boolean foundHeadr = false;
        YUVDataBuf frame = new YUVDataBuf();
        int playFrameNo = -1;   //当前接收的帧号
        CompositeByteBuf buf = Unpooled.compositeBuffer();
        List<YuvFrameBean> yuvFrameBeanList = new ArrayList<>(); //当前帧的数据
        while (isThreadRuning) {
            try {
                frame = mFramequeue.take();
                if (frame == null || frame.buffer == null) {
                    break;
                }
                buf.writeBytes(frame.buffer, 0, frame.length);

                while (true){
                    if (!foundHeadr) {
                        YuvFrameBean frameBean = syncHeader2(buf);
                        if(frameBean != null){
                            foundHeadr = true;
                            packageLength = frameBean.packageLength - 10;
                            if(playFrameNo != frameBean.frameNo){
                                if(frameBean.framePackageCount == frameBean.framePackageNo
                                        && yuvFrameBeanList != null && yuvFrameBeanList.size()>0){
                                    for(YuvFrameBean bean: yuvFrameBeanList){
                                        // 正常数据
                                        if(playBuf.buffer == null){
                                            playBuf.buffer = new byte[LENGTH];
                                        }
                                        System.arraycopy(bean.data,0,playBuf.buffer,playBuf.length,bean.data.length);
                                        playBuf.length += bean.data.length;
                                    }
                                    if(playBuf.length == LENGTH){
                                        myGLSurfaceView.refresh(ByteBuffer.wrap(playBuf.buffer),yuvWidth,yuvHeight);
                                        ZLog.iii(TAG+"==fullData=="+packageLength+","+playBuf.length);
                                        playBuf.length = 0;
                                    }else{
                                        ZLog.eee(TAG+"=IllegalData="+playFrameNo+"!="+frameBean.frameNo+","+playBuf.length+"!="+LENGTH);
                                    }
                                }else{
                                    ZLog.eee(TAG+"=IllegalData="+playFrameNo+"!="+frameBean.frameNo);
                                }
                                if(playFrameNo != -1){
                                    yuvFrameBeanList.clear();
                                }
                                playFrameNo = frameBean.frameNo;
                            }
                            if (buf.isReadable(packageLength)) {
                                foundHeadr = false;
                                byte[] yuvData = new byte[packageLength];
                                buf.readBytes(yuvData, 0, packageLength);
                                buf.discardSomeReadBytes();
                                frameBean.data = yuvData;
                                yuvFrameBeanList.add(frameBean);
                            }else{
                                ZLog.ddd("--PlayYuvThread##不满足条件1: buf数据小于packageLength:"+packageLength);
                                break;
                            }
                        }else{
                            foundHeadr = false;
                            ZLog.ddd("--PlayYuvThread##不满足条件2:header: 没有发现header");
                            break;
                        }
                    }else{
                        ZLog.ddd("--PlayYuvThread##不满足条件3:header: 没有发现header");
                        break;
                    }
                }

            }catch (Exception e){
                ZLog.eee("--PlayYuvThread##e=="+e.getLocalizedMessage());
            }
        }
    }

    int packageLength; //2个字节长度
    int frameNo;    //2个字节帧号
    int framePackageCount; //1个字节当前帧总包数
    int framePackageNo; //1个字节包序号


    /**
     * 依据协议 查找是否有包头，返回该包数据长度
     * 大于0时返回数据包长度，否则表示没有找到头
     * @param in
     * @return
     * @throws Exception
     */
    private int syncHeader(final CompositeByteBuf in) throws Exception {
        // 头数据共21个字节: 4个# + 2个字节报序号 + 8个字节时间戳 + 4字节h264长度 + 1字节标志2是h264;3是h265 + 1字节 类型 视频流98/音频流 97 + 1字节校验  --H264，H265
        // 头数据10个字节： 4个####+2个字节长度+2个字节帧号+1个字节当前帧总包数+1个字节包序号  -- 长度是包含(包头，帧号，帧包数，帧包序号)
        int index = 0;
        while (in.isReadable()) {
            byte c = in.readByte();
            if (c == '#') {
                if (index == 0) {
                    // 这里mark一下，等到下面对头部校验失败后重新reset到这里
                    in.markReaderIndex();
                }
                ++index;
                if (index > 3) {
                    break;
                }
                continue;
            }
            index = 0;
            in.discardSomeReadBytes();
        }
        if (!in.isReadable()) {
            return 0;
        }
        //去掉了4个#的数据
        final byte[] head = new byte[6];
        // 已经连续读取到4个‘#’了
        if (!in.isReadable(head.length-1)) {
            return 0;
        }

        in.readBytes(head);
        in.discardSomeReadBytes();
        packageLength = ByteUtils.pinJie2ByteToInt(head[1], head[0]);
        frameNo = ByteUtils.pinJie2ByteToInt(head[3], head[2]);
        framePackageCount = head[4];
        framePackageNo = head[5];

        ZLog.iii("解析包头:_序号##包长度=" + packageLength + ",帧号:" + frameNo + ",当前帧总包数:" +framePackageCount +"当前帧总包序号" + framePackageNo);
        return packageLength-10;
    }

    /**
     *  依据协议 查找是否有包头，返回完整协议头数据
     *  大于0时返回数据包长度，否则表示没有找到头
     *  数据10个字节： 4个####+2个字节长度+2个字节帧号+1个字节当前帧总包数+1个字节包序号  -- 长度是包含(包头，帧号，帧包数，帧包序号)
     * @param in
     * @return
     * @throws Exception
     */
    private YuvFrameBean syncHeader2(final CompositeByteBuf in) throws Exception {
        // 头数据共21个字节: 4个# + 2个字节报序号 + 8个字节时间戳 + 4字节h264长度 + 1字节标志2是h264;3是h265 + 1字节 类型 视频流98/音频流 97 + 1字节校验
        // 头数据10个字节： 4个####+2个字节长度+2个字节帧号+1个字节当前帧总包数+1个字节包序号  -- 长度是包含(包头，帧号，帧包数，帧包序号)
        int index = 0;
        while (in.isReadable()) {
            byte c = in.readByte();
            if (c == '#') {
                if (index == 0) {
                    // 这里mark一下，等到下面对头部校验失败后重新reset到这里
                    in.markReaderIndex();
                }
                ++index;
                if (index > 3) {
                    break;
                }
                continue;
            }
            index = 0;
            in.discardSomeReadBytes();
        }
        if (!in.isReadable()) {
            return null;
        }
        //去掉了4个#的数据
        final byte[] head = new byte[6];
        // 已经连续读取到4个‘#’了
        if (!in.isReadable(head.length-1)) {
            return null;
        }

        in.readBytes(head);
        in.discardSomeReadBytes();
        YuvFrameBean frameBean = new YuvFrameBean();
        frameBean.packageLength = ByteUtils.pinJie2ByteToInt(head[1], head[0]);
        frameBean.frameNo = ByteUtils.pinJie2ByteToInt(head[3], head[2]);
        frameBean.framePackageCount = head[4];
        frameBean.framePackageNo = head[5];

        ZLog.iii("解析包头:_序号##包长度=" + packageLength + ",帧号:" + frameNo + ",当前帧总包数:" +framePackageCount +"当前帧总包序号" + framePackageNo);
        return frameBean;
    }

    // 将YUV 数据保存图片
    private void saveYUV2Bitmap(byte[] data, int width, int height, int frameFlag) {
        int PREVIEW_WIDTH1 = width, PREVIEW_HEIGHT1 = height;
        int WIDTH = width, HEIGHT = height;
        ByteArrayOutputStream stream = null;
        try {
            //only support ImageFormat.NV21 and ImageFormat.YUY2 for now
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, PREVIEW_WIDTH1, PREVIEW_HEIGHT1,null);  // NV21, 不行

            if (yuvImage != null) {
                stream = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, WIDTH, HEIGHT), 100, stream);
                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                String time = String.valueOf(System.currentTimeMillis());
                String mPictureName = Environment.getExternalStorageDirectory()+"/com.kgd.h26xplayer/"+frameFlag+"_"+time+".jpg";
                File saveFile = new File(mPictureName);
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(saveFile);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 暂时只保存最近三天的log
    private void clearYuvJpg() {
        try{
            ZLog.iii("clearYuvJpg==");
            File Sfile = new File(Environment.getExternalStorageDirectory() + "/" + AppContext.getInstance().getPackageName());
            if (!Sfile.exists())
                return;
            File[] fs = Sfile.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    if (pathname.getName().endsWith(".jpg")) { //yuv.jpg 文件
                        return true;
                    }
                    return false;
                }
            });
            for (int i = fs.length - 1; i > -1; i--) {
                ZLog.iii("--delete--clearYuvJpg=" + fs[i].getName() + "; lastModified="
                        + new Date(fs[i].lastModified()));
                fs[i].delete();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    class YuvFrameBean{
        int packageLength; //2个字节长度
        int frameNo;    //2个字节帧号
        int framePackageCount; //1个字节当前帧总包数
        int framePackageNo; //1个字节包序号
        byte[] data;    //数据
    }
}

