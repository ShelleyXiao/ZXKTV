package com.zxktv.opengles;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.zxktv.util.LogUtils;

import javax.microedition.khronos.opengles.GL10;

public class TextureUtil {
    static SurfaceTexture mSurfaceTexture;
    static int textureid;

    static EglUtils mEglUtils;

    public static SurfaceTexture getInstance() {
        if (mSurfaceTexture == null) {
            LogUtils.d("SurfaceTexture shared");
            mSurfaceTexture = new SurfaceTexture(createTextureID());
//            mEglUtils = new EglUtils();
//            mEglUtils.initGL(mSurfaceTexture);

            try {
                mSurfaceTexture.detachFromGLContext();
            } catch (Exception e) {
                LogUtils.e("����detachʧ��");
            }
        }

        return mSurfaceTexture;
    }

    public static void destroy() {
        mSurfaceTexture.setOnFrameAvailableListener(null);
        mSurfaceTexture.release();

        mEglUtils.release();
    }


    public synchronized static void draw(DirectDrawer mDirectDrawer) {

        int mTextureID = mDirectDrawer.getTextureid_mediacodec();
//        LogUtils.e("start draw Frame..." + mTextureID);

        getInstance().attachToGLContext(textureid);
        getInstance().updateTexImage();

        float[] mtx = new float[16];
        getInstance().getTransformMatrix(mtx);
        mDirectDrawer.draw(mtx);

        getInstance().detachFromGLContext();

    //    mEglUtils.swap();


    }

    private static int createTextureID() {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        textureid = texture[0];

        return texture[0];
    }
}
