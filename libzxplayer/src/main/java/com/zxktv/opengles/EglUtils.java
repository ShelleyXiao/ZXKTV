package com.zxktv.opengles;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLUtils;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import static javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT;

/**
 * User: ShaudXiao
 * Date: 2018-08-23
 * Time: 17:04
 * Company: zx
 * Description:
 * FIXME
 */

public class EglUtils {

    /*For EGL Setup*/
    private EGL10 mEgl;
    private EGLDisplay mEglDisplay;
    private EGLConfig mEglConfig;
    private EGLContext mEglContext;
    private EGLSurface mEglSurface;

    public void initGL(SurfaceTexture surface) {
        /*Get EGL handle*/
        mEgl = (EGL10) EGLContext.getEGL();

        /*Get EGL display*/
        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        if (EGL10.EGL_NO_DISPLAY == mEglDisplay) {
            throw new RuntimeException("eglGetDisplay failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }

        /*Initialize & Version*/
        int versions[] = new int[2];
        if (!mEgl.eglInitialize(mEglDisplay, versions)) {
            throw new RuntimeException("eglInitialize failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }

        /*Configuration*/
        int configsCount[] = new int[1];
        EGLConfig configs[] = new EGLConfig[1];
        int configSpec[] = new int[]{
                EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE
        };

        mEgl.eglChooseConfig(mEglDisplay, configSpec, configs, 1, configsCount);
        if (configsCount[0] <= 0) {
            throw new RuntimeException("eglChooseConfig failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
        mEglConfig = configs[0];

        /*Create Context*/
        int contextSpec[] = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        };
        mEglContext = mEgl.eglCreateContext(mEglDisplay, mEglConfig, EGL_NO_CONTEXT, contextSpec);

        if (EGL_NO_CONTEXT == mEglContext) {
            throw new RuntimeException("eglCreateContext failed: " + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }

        /*Create window surface*/
        mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay, mEglConfig, surface, null);

        if (null == mEglSurface || EGL10.EGL_NO_SURFACE == mEglSurface) {
            throw new RuntimeException("eglCreateWindowSurface failed" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }

        /*Make current*/
        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw new RuntimeException("eglMakeCurrent failed:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
    }

    public EGLContext getContext() {
        return mEglContext;
    }

    public void swap() {
        mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);
    }

    public void release() {
        if (mEglSurface != EGL10.EGL_NO_SURFACE) {
            mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
            mEglSurface = EGL10.EGL_NO_SURFACE;
        }
        if (mEglContext != EGL_NO_CONTEXT) {
            mEgl.eglDestroyContext(mEglDisplay, mEglContext);
            mEglContext = EGL10.EGL_NO_CONTEXT;
        }
        if (mEglDisplay != EGL10.EGL_NO_DISPLAY) {
            mEgl.eglTerminate(mEglDisplay);
            mEglDisplay = EGL10.EGL_NO_DISPLAY;
        }
    }

}
