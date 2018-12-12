package com.zx.zxktv.presentation;

import android.app.Presentation;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.zx.zxktv.R;
import com.zx.zxktv.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * User: zx
 * Date: 2018-08-21
 * Time: 09:19
 * Company: zx
 * Description: 播放多个视频源（如果只是需要播放一个源，可采用一个解码多surface渲染方案）
 * FIXME
 */

public class MutilVideoPresentation2 extends Presentation implements
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener
        , GLPlayRenderThread.renderActionListener, SurfaceTexture.OnFrameAvailableListener {

    public static final String TEST_MOVIE = "/mnt/sdcard/media/test.mp4";
    private static final int VIDEO_COUNT = 6;

    // Must be static storage so they'll survive Activity restart.
    private static boolean sVideoRunning = false;
    private static FrameLayout[] sBlob = new FrameLayout[VIDEO_COUNT];

    private MediaPlayer mMediaPlayer;
    private SurfaceTexture mSurfaceTexture = null;

    private static MutilVideoPresentation2 sVideoPresentation;

    private ArrayList<GLPlayRenderThread> mThreadList = new ArrayList<GLPlayRenderThread>();

    private Handler mHandler = new Handler();
    ;

    public MutilVideoPresentation2(Context outerContext, Display display) {
        super(outerContext, display);
    }

    public MutilVideoPresentation2(Context outerContext, Display display, int theme) {
        super(outerContext, display, theme);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.presentation_mutil_video_decode2);

        sVideoPresentation = this;

        final Window window = getWindow();
        window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams wlp = window.getAttributes();

//        wlp.width = (int) (ViewUtils.getScreenWidth(getContext()) * 0.6);
//        wlp.height = (int) (ViewUtils.getScreenHeight(getContext()) * 0.6);

        wlp.gravity = Gravity.CENTER;
        window.setAttributes(wlp);


        sBlob[0] = ((FrameLayout) findViewById(R.id.texture_view_1_0));
        sBlob[1] = ((FrameLayout) findViewById(R.id.texture_view_1_1));
        sBlob[2] = ((FrameLayout) findViewById(R.id.texture_view_2_0));
        sBlob[3] = ((FrameLayout) findViewById(R.id.texture_view_2_1));
        sBlob[4] = ((FrameLayout) findViewById(R.id.texture_view_2_2));
        sBlob[5] = ((FrameLayout) findViewById(R.id.texture_view_3_0));

        for (int i = 0; i < sBlob.length; i++) {
            FrameLayout l = sBlob[i];
            attachFrameLayout(l);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();


    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        int size = mThreadList.size();
        for (int i = 0; i < size; i++) {
            GLPlayRenderThread t = mThreadList.get(i);
            synchronized (t) {
                t.notify();
            }
        }
    }

    @Override
    public void dismiss() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        super.dismiss();
    }

    public void destroy() {
        int size = mThreadList.size();
        for (int i = 0; i < size; i++) {
            GLPlayRenderThread t = mThreadList.get(i);
            synchronized (t) {
                t.suspendRendering();
            }
        }
    }

    public void attachFrameLayout(FrameLayout fl) {

        TextureView texture = new TextureView(fl.getContext());
        texture.setSurfaceTextureListener(new VideoTextureCallback());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        fl.addView(texture, params);

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.start();
        mp.setLooping(true);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.start();
    }

    @Override
    synchronized public void startPlay(int texture) {

        File file = new File(TEST_MOVIE);
        if (!file.exists()) {
            return;
        }

        if (mSurfaceTexture == null) {

            mSurfaceTexture = new SurfaceTexture(texture);
            mSurfaceTexture.setOnFrameAvailableListener(this);
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            Surface mSurface = new Surface(mSurfaceTexture);
            mMediaPlayer.setSurface(mSurface);

            mMediaPlayer.setVolume((float) 0, (float) 0);
            LogUtils.i("************************create");

            mHandler.postAtTime(new Runnable() {
                @Override
                public void run() {
                    try {
                        mMediaPlayer.reset();
                        mMediaPlayer.setDataSource(TEST_MOVIE);
                        mMediaPlayer.prepareAsync();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 500);

            mSurfaceTexture.detachFromGLContext();
        }
    }

    @Override
    public void updatePlayPreview() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.updateTexImage();
        }
    }

    @Override
    public void attachPlayTexture(int texture) {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.attachToGLContext(texture);
        }
    }

    @Override
    public void detachPlayTexture() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.detachFromGLContext();
        }
    }

    class VideoTextureCallback implements TextureView.SurfaceTextureListener {

        private GLPlayRenderThread mThread;

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {
            mThread = new GLPlayRenderThread(sVideoPresentation, surface, sVideoPresentation);
            mThread.setRegion(width, height);
            synchronized (mThreadList) {
                mThreadList.add(mThread);
            }
            mThread.start();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mThread.suspendRendering();
            mThreadList.remove(mThread);
            return false;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            mThread.setRegion(width, height);
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // TODO Auto-generated method stub

        }

    }
}
