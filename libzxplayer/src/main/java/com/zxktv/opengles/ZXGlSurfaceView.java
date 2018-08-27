package com.zxktv.opengles;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.zxktv.listener.OnGlSurfaceViewOncreateListener;
import com.zxktv.listener.OnRenderRefreshListener;


public class ZXGlSurfaceView extends GLSurfaceView{

    private GlViewRender mGlViewRender;
    private OnGlSurfaceViewOncreateListener onGlSurfaceViewOncreateListener;

    public ZXGlSurfaceView(Context context) {
        this(context, null);
    }

    public ZXGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGlViewRender = new GlViewRender(context);
        //设置egl版本为2.0
        setEGLContextClientVersion(2);
        //设置render
        setRenderer(mGlViewRender);
        //设置为手动刷新模式
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGlViewRender.setOnRenderRefreshListener(new OnRenderRefreshListener() {
            @Override
            public void onRefresh() {
                requestRender();
            }
        });
    }

    public void setOnGlSurfaceViewOncreateListener(OnGlSurfaceViewOncreateListener onGlSurfaceViewOncreateListener) {
        if(mGlViewRender != null) {
            mGlViewRender.setOnGlSurfaceViewOncreateListener(onGlSurfaceViewOncreateListener);
        }
    }

    public void setCodecType(int type) {
        if(mGlViewRender != null) {
            mGlViewRender.setCodecType(type);
        }
    }


    public void setFrameData(int w, int h, byte[] y, byte[] u, byte[] v) {
        if(mGlViewRender != null) {
            mGlViewRender.setFrameData(w, h, y, u, v);
            requestRender();
        }
    }
}
