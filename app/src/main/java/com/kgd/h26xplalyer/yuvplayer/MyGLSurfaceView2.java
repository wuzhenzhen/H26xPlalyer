package com.kgd.h26xplalyer.yuvplayer;

import static android.opengl.GLES20.GL_TEXTURE_2D;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 *  MyGLSurfaceView2  不处理 yuv数据的分包粘包，由PlayYuvThread 去处理
 */
public class MyGLSurfaceView2 extends GLSurfaceView implements GLSurfaceView.Renderer {
    private int mProgram;
    private int[] mTextureIds;
    private boolean isClear = true;
    private int yuvWidth = 320;
    private int yuvHeight = 160;
//    byte[] y = new byte[yuvWidth*yuvHeight];
    private ByteBuffer yBuffer;
//    CompositeByteBuf buf = Unpooled.compositeBuffer();
    protected FloatBuffer mVertexBuffer;

    public MyGLSurfaceView2(Context context) {
        super(context);
        bindGLViewRenderer();
    }

    public MyGLSurfaceView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        bindGLViewRenderer();
    }


    private void bindGLViewRenderer(){
        setEGLContextClientVersion(3);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
    private void setOpenGL() {
        //灰色背景
        String vertexSource = ShaderUtil.loadFromAssets("vertex.vsh", getResources());
        String fragmentSource = ShaderUtil.loadFromAssets("fragment1.fsh", getResources());
        mProgram = ShaderUtil.createProgram(vertexSource, fragmentSource);
        //创建纹理
        mTextureIds = new int[3];
        GLES30.glGenTextures(mTextureIds.length, mTextureIds, 0);
        for (int i = 0; i < mTextureIds.length; i++) {
            //绑定纹理
            GLES30.glBindTexture(GL_TEXTURE_2D, mTextureIds[i]);
            //设置环绕和过滤方式
            GLES30.glTexParameteri(GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
            GLES30.glTexParameteri(GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);
            GLES30.glTexParameteri(GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        }
        // OpenGL的世界坐标系是 [-1, -1, 1, 1]，纹理的坐标系为 [0, 0, 1, 1]
        float[] vertices = new float[]{
                // 前三个数字为顶点坐标(x, y, z)，后两个数字为纹理坐标(s, t)
                // 第一个三角形
                1f,  1f,  0f,       1f, 0f,
                1f,  -1f, 0f,       1f, 1f,
                -1f, -1f, 0f,       0f, 1f,
                // 第二个三角形
                1f,  1f,  0f,       1f, 0f,
                -1f, -1f, 0f,       0f, 1f,
                -1f, 1f,  0f,       0f, 0f
        };
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4); // 一个 float 是四个字节
        vbb.order(ByteOrder.nativeOrder()); // 必须要是 native order
        mVertexBuffer = vbb.asFloatBuffer();
        mVertexBuffer.put(vertices);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        setOpenGL();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // 视距区域设置使用 GLSurfaceView 的宽高
        GLES30.glViewport(0, 0, width, height);
    }
    long startTime = 0l;
    int frameCount;

    @Override
    public void onDrawFrame(GL10 gl) {
        // 该函数多次被调用的时，不要每次都new，可以设置为全局变量缓存起来
        try {
            if(isClear){
                gl.glClearColor(0, 0, 0, 1);
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
                return;
            }
            startTime = System.currentTimeMillis();

            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT); // clear color buffer
            // 1. 选择使用的程序
            GLES30.glUseProgram(mProgram);
            // 2.1 加载纹理y
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0); //激活纹理0
            GLES30.glBindTexture(GL_TEXTURE_2D, mTextureIds[0]); //绑定纹理
            GLES30.glTexImage2D(GL_TEXTURE_2D, 0, GLES30.GL_LUMINANCE, yuvWidth,
                    yuvHeight, 0, GLES30.GL_LUMINANCE, GLES30.GL_UNSIGNED_BYTE, yBuffer); // 赋值
            // 第二个参数 0是I420, 1是NV12
            GLES30.glUniform1i(0, 0); // sampler_y的location=0, 把纹理0赋值给sampler_y
////        // 2.2 加载纹理u
//        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
//        GLES30.glBindTexture(GL_TEXTURE_2D, mTextureIds[1]);
//        GLES30.glTexImage2D(GL_TEXTURE_2D, 0, GLES30.GL_LUMINANCE, yuvWidth / 2,
//                yuvHeight / 2, 0, GLES30.GL_LUMINANCE, GLES30.GL_UNSIGNED_BYTE, uBuffer);
//        GLES30.glUniform1i(1, 1); // sampler_u的location=1, 把纹理1赋值给sampler_u
//        // 2.3 加载纹理v
//        GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
//        GLES30.glBindTexture(GL_TEXTURE_2D, mTextureIds[2]);
//        GLES30.glTexImage2D(GL_TEXTURE_2D, 0, GLES30.GL_LUMINANCE, yuvWidth / 2,
//                yuvHeight / 2, 0, GLES30.GL_LUMINANCE, GLES30.GL_UNSIGNED_BYTE, vBuffer);
//        GLES30.glUniform1i(2, 2); // sampler_v的location=2, 把纹理1赋值给sampler_v
            // 3. 加载顶点数据
            mVertexBuffer.position(0);
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 5 * 4, mVertexBuffer);
            GLES30.glEnableVertexAttribArray(0);
            mVertexBuffer.position(3);
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 5 * 4, mVertexBuffer);
            GLES30.glEnableVertexAttribArray(1);
            // 4. 绘制
            // GL_POINTS       //点
            //GL_LINES        //线段
            //GL_LINE_STRIP   //多段线
            //GL_LINE_LOOP    //线圈
            //GL_TRIANGLES    //三角形
            //GL_TRIANGLE_STRIP //三角形条带
            //GL_TRIANGLE_FAN   //三角形扇
            //GL_QUADS          //四边形
            //GL_QUAD_STRIP     //四边形条带
            //GL_POLYGON        //多边形(凸)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6);
            frameCount ++;
            return;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refresh(ByteBuffer buffer, int yuvWidth, int yuvHeight){
        isClear = false;
        this.yuvWidth = yuvWidth;
        this.yuvHeight = yuvHeight;
        if (yBuffer != null) {
            yBuffer.clear();
        }
        this.yBuffer = buffer;
        requestRender();
    }

    public void clearGlSurfaceView(){
        isClear = true;
        requestRender();
    }
}
