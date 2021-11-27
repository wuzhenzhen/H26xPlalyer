package com.kgd.h26xplalyer;

import static com.kgd.h26xplalyer.Constants.MIME_TYPE_264;
import static com.kgd.h26xplalyer.Constants.MIME_TYPE_265;
import static com.kgd.h26xplalyer.Constants.VIDEO_HEIGHT;
import static com.kgd.h26xplalyer.Constants.VIDEO_WIDTH;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 播放h26x线程
 * 初始化时已经调用了start();
 */
public class PlayH26xThread extends Thread {
    final String TAG = "PlayH26xRuning \t";
    public boolean isThreadRuning = false;
    private MediaCodec mCodec; //硬解码器
    String mediaType ;//解码类型
    Surface surface;//播放控件
    long lastTime;
    public long timemp;
    public long presentationTimeUs; //时间戳微秒
    public int seqno;//最后一包序号

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date date = new Date();
    //存放数据的队列
    private final LinkedBlockingQueue<FrameBuffer> mFramequeue = new LinkedBlockingQueue<>();
    byte videoOrAudio;//视频流98;音频流 97
    //音频流类型
    int streamType = AudioManager.STREAM_MUSIC;
    ////指定采样率 （MediaRecoder 的采样率通常是8000Hz AAC的通常是44100Hz。 设置采样率为44100，
    // 目前为常用的采样率，官方文档表示这个值可以兼容所有的设置）
    int sampleRateInHz = 8000;
    //配置声道
    int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
    //指定音频量化位数 ,在AudioFormaat类中指定了以下各种可能的常量。通常我们选择ENCODING_PCM_16BIT和ENCODING_PCM_8BIT
    // PCM代表的是脉冲编码调制，它实际上是原始音频样本。
    //因此可以设置每个样本的分辨率为16位或者8位，16位将占用更多的空间和处理能力,表示的音频也更加接近真实。
    int audioFormat =  AudioFormat.ENCODING_PCM_16BIT;
    //STREAM的意思是由用户在应用程序通过write方式把数据一次一次得写到audiotrack中。这个和我们在socket中发送数据一样，
    // 应用层从某个地方获取数据，例如通过编解码得到PCM数据，然后write到audiotrack。
    int mode = AudioTrack.MODE_STREAM;
    //指定缓冲区大小。 AudioTrack.getMinBufferSize方法可以获得。
    int bufferSizeInBytes;
    //2创建AudioTrack
    AudioTrack trackplayer;//
        //是否暂停
    public  boolean isPause = false;
    public long timeoutUs = 10000;
    public PlayH26xThread(Surface surface) {
        this.surface = surface;
        clearData();
        initAndroidTrack();
        start();
    }

    /**
     * 初始化音频
     */
    private void initAndroidTrack(){
//        //注意，按照数字音频的知识，这个算出来的是一秒钟buffer的大小。
        bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz,
                channelConfig,//双声道
                audioFormat);//一个采样点16比特-2个字节
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            trackplayer = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(streamType)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRateInHz)
                            .setChannelMask(channelConfig)
                            .build())
                    .setBufferSizeInBytes(bufferSizeInBytes)
                    .setTransferMode(mode)
                    .build();
        } else {
            trackplayer = new AudioTrack(streamType, sampleRateInHz,channelConfig,
                    audioFormat, bufferSizeInBytes,mode);
//            trackplayer.setStereoVolume(1f,1f);
        }
        //3.播放PCM音频流
        trackplayer.play();
        Log.d(TAG,"音频初始化ok");
    }
    final int LENGTH = 0x40000;
    public void addVideoData(byte[] buf)  {
//        if (isPause){
//            return;
//        }
        int count = buf.length % LENGTH  == 0 ? buf.length/LENGTH : buf.length/LENGTH +1;
        int surplusLength = buf.length % LENGTH;    //剩余的长度
        for (int i = 0; i < count; i++) {
            FrameBuffer frame = new FrameBuffer();
            if (buf.length <= LENGTH) {
                frame.buffer = new byte[buf.length];
                System.arraycopy(buf, 0, frame.buffer, 0, buf.length);
            }else{
                if (i == count -1){
                    //最后一个长度为余下的数据
                    frame.buffer = new byte[surplusLength];
                    System.arraycopy(buf, i * LENGTH, frame.buffer, 0, surplusLength);
                }else {
                    frame.buffer = new byte[LENGTH];
                    System.arraycopy(buf, i * LENGTH, frame.buffer, 0, LENGTH);
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
    //获取校验
    @Override
    public void start() {
        if (!isThreadRuning) {
            isThreadRuning = true;
            super.start();
        }
    }
    boolean foundFlag = false;  //针对设备端发送数据少一个字节问题
    @Override
    public void run() {
        boolean foundHeadr = false;
        int packageLength = 0;
        FrameBuffer frame;
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
                            foundFlag = false;
                            byte[] h264Data = new byte[packageLength];
                            buf.readBytes(h264Data, 0, packageLength);
                            buf.discardSomeReadBytes();
                            if(h264Data[packageLength-1] == '#'){
                                foundFlag = true;
                            }
                            if (videoOrAudio == 98) {
                                onFrame(h264Data);
                            }else if (videoOrAudio == 97){
//                        //4.写入PCM数据
                                trackplayer.write(h264Data,0, h264Data.length);
                            }
                        }else{
                            Log.d(TAG,"--PlayH26xThread##不满足条件1: buf数据小于packageLength:"+packageLength);
                            break;
                        }
                    }else{
                        Log.d(TAG,"--PlayH26xThread##不满足条件2:header: 没有发现header");
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Log.e(TAG,"PlayH26xThread:e1="+e.getLocalizedMessage());
                e.printStackTrace();
                buf.clear();
                continue;
            } catch (OutOfMemoryError oom){
                Log.e(TAG,"PlayH26xThread:e2="+oom.getLocalizedMessage());
                oom.printStackTrace();
                buf.clear();
                continue;
            }catch (Exception e) {
                Log.e(TAG,"PlayH26xThread:e3="+e.getLocalizedMessage());
                e.printStackTrace();
                buf.clear();
                continue;
            }
        }
    }

    /**
     * 解析头数据
     * @return
     * @throws Exception
     */
    //为减少卡顿，此方法应在子线程下被执行
    private void onFrame(byte[] videoData) {
        try {
            //设置解码等待时间，0为不等待，-1为一直等待，其余为时间单位
            int inputBufferIndex = mCodec.dequeueInputBuffer(timeoutUs);
            //填充数据到输入流
            if (inputBufferIndex >= 0) {
                //获取MediaCodec的输入流
                ByteBuffer inputBuffer;
                //androidAPI >= 21
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    inputBuffer = mCodec.getInputBuffer(inputBufferIndex);
                } else {
                    //兼容安卓5.0以下，如不需要可以删掉
                    ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
                    inputBuffer = inputBuffers[inputBufferIndex];
                }
                inputBuffer.clear();
                inputBuffer.put(videoData, 0, videoData.length);
                mCodec.queueInputBuffer(inputBufferIndex, 0, videoData.length, presentationTimeUs , 0);
            }else{
                Log.i(TAG,"onFrame:inputBufferIndex<0");
                return;
            }
            //解码数据到surface，实际项目中最好将以下代码放入另一个线程，不断循环解码以降低延迟
            MediaCodec.BufferInfo mediaCodecBufferInfo = new MediaCodec.BufferInfo();
            int  outputBufferIndex = mCodec.dequeueOutputBuffer(mediaCodecBufferInfo, timeoutUs);
            Log.i(TAG,"解析到的帧数据1:header:" + videoData[0]+","+ videoData[1]+"," + videoData[2]+","+videoData[3]+","+outputBufferIndex);
            while (outputBufferIndex >= 0) {
                Log.i(TAG,"解析到的帧数据2:header:" + videoData[0]+","+ videoData[1]+"," + videoData[2]+","+videoData[3]+","+outputBufferIndex);
                mCodec.releaseOutputBuffer(outputBufferIndex, true);    //释放缓冲区解码的数据到surfaceview
                outputBufferIndex = mCodec.dequeueOutputBuffer(mediaCodecBufferInfo, timeoutUs);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    // 大于0时返回数据包长度，否则表示没有找到头
    private int syncHeader(final CompositeByteBuf in) throws Exception {
        // 头数据共21个字节: 4个# + 2个字节报序号 + 8个字节时间戳 + 4字节h264长度 + 1字节标志2是h264;3是h265 + 1字节 类型 视频流98/音频流 97 + 1字节校验
        // wzz: 4字节H264长度是指 h264数据的长度,h264数据在检验码后面, 1字节检验是从 4个#到1字节类型的异或检验
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
            }else if (foundFlag && index>2){
                in.readerIndex(in.readerIndex()-1);
                Log.i(TAG,"解析包头:_序号###");
                break;  //针对个别情况下，少发一个字节的数据
            }
            index = 0;
            in.discardSomeReadBytes();
        }
        if (!in.isReadable()) {
            return 0;
        }
        //去掉了4个#的数据
        final byte[] head = new byte[17];
        // 已经连续读取到4个‘#’了
        if (!in.isReadable(head.length-1)) {
            return 0;
        }

        in.readBytes(head);
        //head[head.length-1] 是头中最后一位标示校验位
        if (getCheckXor(head, 0, head.length-1) != head[head.length-1]) {
            return syncHeader(in.resetReaderIndex());
        }
        in.discardSomeReadBytes();
        seqno = ByteUtils.pinJie2ByteToInt(head[0], head[1]);

        byte[] time = {head[2],head[3],head[4],head[5],head[6],head[7],head[8],head[9]};
        presentationTimeUs = ByteUtils.bytes2long(time);
        //时间戳 单位毫秒
        timemp = presentationTimeUs / 1000L;
        byte[] len = {head[10],head[11],head[12],head[13]};
        int  packageLen = ByteUtils.bytesUpLowToInt2(len, 0);
        date.setTime(timemp);
        lastTime = timemp;
        if (TextUtils.isEmpty(mediaType)) {
            if (2 == head[14]) {
                mediaType = MIME_TYPE_264;
            }else if (3 == head[14]){
                mediaType = MIME_TYPE_265;
            }
        }
        Log.i(TAG,"解析包头:_序号##" + seqno + "时间戳:" + timemp + " (时间差:" + (timemp - lastTime) +")" + simpleDateFormat.format(date)+ "  长度:" + packageLen+" 类型:"+mediaType+","+presentationTimeUs);
        if (mCodec == null){
            mCodec = getMediaCodec(surface,mediaType);
        }
        videoOrAudio = head[15];
        return packageLen;
    }

    private static byte getCheckXor(byte[] data, int pos, int len) {
        byte a = 0;
        for (int i = pos; i < len; i++) {
            a ^= data[i];
        }
        return a;
    }
    @Override
    public void interrupt() {
        Log.i(TAG,"PlayH26xThread:interrupt");
        super.interrupt();
        isThreadRuning = false;
    }
    /**
     *停止解码，释放解码器
     */
    public void stopRun() {
        Log.i(TAG,"PlayH26xThread:stopRun");
        interrupt();
        if (mCodec != null){
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
        if (trackplayer!= null){
//            5.停止播放,释放底层资源
            trackplayer.stop();//停止播放
            trackplayer.release();//释放底层资源
            Log.d(TAG,"音频停止,释放底层资源");
        }
        clearData();
        clearVideoFile();
    }
    public void clearData(){
        isPause = true;
        mFramequeue.clear();
    }

    public void clearSurface(){
        Log.i(TAG,"PlayH26xThread:clearSurface");
        try{
            //TODO  wzz 尝试把 mCodec 清空试试
            if (mCodec != null){
                mCodec.stop();
                mCodec.release();
                mCodec = null;
            }
            clearVideoFile();
//        clearSurface(surface);
        }catch (Exception e){

        }
    }

    //有效果，次数多的话，还是会偶现一次
    private void clearSurface(Surface surface) {
        EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] version = new int[2];
        EGL14.eglInitialize(display, version, 0, version, 1);

        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE, 0,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        EGL14.eglChooseConfig(display, attribList, 0, configs, 0, configs.length, numConfigs, 0);

        EGLConfig config = configs[0];
        EGLContext context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        }, 0);

        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(display, config, surface,
                new int[]{
                        EGL14.EGL_NONE
                }, 0);

        EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context);
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        EGL14.eglSwapBuffers(display, eglSurface);
        EGL14.eglDestroySurface(display, eglSurface);
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        EGL14.eglDestroyContext(display, context);
        EGL14.eglTerminate(display);
    }
    /**
     * 视频流大小
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

    static class FrameBuffer {
        byte[] buffer;
        int length;
    }

    /**
     * @param surface
     * @return
     * @throws IOException
     */
    public MediaCodec getMediaCodec(Surface surface,String type ){
        MediaCodec mCodec = null;
        try {
            if (TextUtils.isEmpty(type)){
                type = MIME_TYPE_264;
            }
            mCodec = MediaCodec.createDecoderByType(type);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(type,
                VIDEO_WIDTH, VIDEO_HEIGHT);

//        byte[]header_sps = AscIITools.HexToByteArr("0000000140010C01FFFF016000000300B0000003000003007BAC0900000001420101016000000300B0000003000003007BA003C08010E58DAE4932F4DC04040402000000014401C0F2F03C90");
//        0000000140010C01FFFF016000000300B0000003000003007BAC0900000001420101016000000300B0000003000003007BA003C08010E58DAE4932F4DC04040402
//        000000014401C0F2F03C90
//        mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
        ////关键帧间隔时间 单位s
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 25);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible); //MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 25);//比特率 bit单位
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);//帧率
        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR); // 调整码率的控流模式
        mediaFormat.setInteger(MediaFormat.KEY_MAX_HEIGHT, 1080);
//        mediaFormat.setInteger(MediaFormat.KEY_MAX_HEIGHT, MediaCodecInfo.VideoCapabilities.PerformancePoint.HD_200);

        mCodec.configure(mediaFormat,surface,null, 0);
        mCodec.start();
        printInfo();
        return  mCodec;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void printInfo() {
        MediaCodecList list = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos = list.getCodecInfos();
        for (MediaCodecInfo info : codecInfos) {
            StringBuffer sbEncoder = new StringBuffer();
            StringBuffer sbEncoder2 = new StringBuffer();
            if (info.isEncoder()) {
//                StringBuilder sb = new StringBuilder();
                sbEncoder.append(info.getName() + " types=");
                String[] supportedTypes = info.getSupportedTypes();
                for (String string : supportedTypes) {
                    sbEncoder.append(" " + string);
                }
                Log.i(TAG,"printInfo1=="+sbEncoder.toString());
            }else {
                sbEncoder2.append(info.getName() + " types=");
                String[] supportedTypes = info.getSupportedTypes();
                for (String string : supportedTypes) {
                    sbEncoder2.append(" " + string);
                }
                Log.i(TAG,"printInfo2=="+sbEncoder2.toString());
            }
        }
    }
}
