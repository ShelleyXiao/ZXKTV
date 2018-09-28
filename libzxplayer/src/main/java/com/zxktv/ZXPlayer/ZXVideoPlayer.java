package com.zxktv.ZXPlayer;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;
import android.view.Surface;

import com.zxktv.audioeffect.AudioEffect;
import com.zxktv.listener.OnCompleteListener;
import com.zxktv.listener.OnErrorListener;
import com.zxktv.listener.OnGlSurfaceViewOncreateListener;
import com.zxktv.listener.OnInfoListener;
import com.zxktv.listener.OnLoadListener;
import com.zxktv.listener.OnPreparedListener;
import com.zxktv.listener.OnStopListener;
import com.zxktv.listener.Status;
import com.zxktv.opengles.ZXGlSurfaceView;
import com.zxktv.util.LogUtils;
import com.zxktv.util.MyLog;

import java.io.IOException;
import java.nio.ByteBuffer;


public class ZXVideoPlayer {

    static {
        System.loadLibrary("avutil-55");
        System.loadLibrary("swresample-2");
        System.loadLibrary("avcodec-57");
        System.loadLibrary("avformat-57");
        System.loadLibrary("swscale-4");
        System.loadLibrary("postproc-54");
        System.loadLibrary("avfilter-6");
        System.loadLibrary("avdevice-57");
        System.loadLibrary("zxplayer");
    }

    /**
     * 播放文件路径
     */
    private String dataSource;
    /**
     * 硬解码mime
     */
    private MediaFormat mediaFormat;
    /**
     * 视频硬解码器
     */
    private MediaCodec mediaCodec;
    /**
     * 渲染surface
     */
    private Surface surface;
    /**
     * opengl surfaceview
     */
    private ZXGlSurfaceView glSurfaceView;

    private ZXGlSurfaceView glSurfaceViewExtend;
    /**
     * 视频解码器info
     */
    private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

    /**
     * 准备好时的回调
     */
    private OnPreparedListener mOnPreparedListener;
    /**
     * 错误时的回调
     */
    private OnErrorListener mOnErrorListener;
    /**
     * 加载回调
     */
    private OnLoadListener mOnLoadListener;
    /**
     * 更新时间回调
     */
    private OnInfoListener mOnInfoListener;
    /**
     * 播放完成回调
     */
    private OnCompleteListener mOnCompleteListener;
    /**
     * 停止完成回调
     */
    private OnStopListener mOnStopListener;
    /**
     * 是否已经准备好
     */
    private boolean parpared = false;
    /**
     * 时长实体类
     */
    private TimeBean mTimeBean;
    /**
     * 上一次播放时间
     */
    private int lastCurrTime = 0;

    /**
     * 是否只有音频（只播放音频流）
     */
    private boolean isOnlyMusic = false;

    private boolean isOnlySoft = false;

    private static int volumePercent = 100;
    private static float speed = 1.0f;
    private static float pitch = 1.0f;
    private static boolean initmediacodec = false;
    private static MuteEnum muteEnum = MuteEnum.MUTE_CENTER;


    public ZXVideoPlayer() {
        mTimeBean = new TimeBean();
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public void setOnlyMusic(boolean onlyMusic) {
        isOnlyMusic = onlyMusic;
    }

    public void setSurface(Surface surface) {
        this.surface = surface;
    }


    public void setZXGlSurfaceView(ZXGlSurfaceView glSurfaceView, ZXGlSurfaceView glSurfaceViewExtend) {
        this.glSurfaceView = glSurfaceView;

        this.glSurfaceViewExtend = glSurfaceViewExtend;

        glSurfaceView.setOnGlSurfaceViewOncreateListener(new OnGlSurfaceViewOncreateListener() {

            @Override
            public void onGlSurfaceViewOncreate(Surface s) {
                if (surface == null) {
                    setSurface(s);
                }

//                if (parpared && !TextUtils.isDigitsOnly(dataSource)) {
//                    nativePrepared(dataSource, isOnlyMusic);
//                }

            }
        });


    }


    /**
     * 准备
     *
     * @param url
     */
    private native void nativePrepared(String url, boolean isOnlyMusic);

    /**
     * 开始
     */
    private native void nativeStart();

    /**
     * 停止并释放资源
     */
    private native void nativeStop(boolean exit);

    /**
     * 暂停
     */
    private native void nativePause();

    /**
     * 播放 对应暂停
     */
    private native void nativeResume();

    /**
     * seek
     *
     * @param secds
     */
    private native void nativeSeek(int secds);

    /**
     * 设置音轨 根据获取的音轨数 排序
     *
     * @param index
     */
    private native void nativeSetAudioChannels(int index);

    /**
     * 设置声音音量 1-100%
     *
     * @param percent
     */
    private native void nativeSetVolume(int percent);


    /**
     * 设置静音
     *
     * @param mute
     */
    private native void nativeSetVolMute(boolean mute);

    /**
     * 设置声道静音
     *
     * @param mute (mute 0: right ; mute 1: left; mute 2 : center)
     */
    private native void nativeSetChannelMute(int mute);


    private native void nativeSetAudioEffect(AudioEffect effectParam);

    /**
     * 获取总时长
     *
     * @return
     */
    private native int nativeGetDuration();

    /**
     * 获取音轨数
     *
     * @return
     */
    private native int nativeGetAudioChannels();

    /**
     * 获取视频宽度
     *
     * @return
     */
    private native int nativeGetVideoWidth();

    /**
     * 获取视频长度
     *
     * @return
     */
    private native int nativeGetVideoHeight();

    public int getDuration() {
        return nativeGetDuration();
    }

    public int getVideoWidth() {
        return getVideoWidth();
    }

    public int getVideoHeight() {
        return getVideoHeight();
    }

    public void setAudioChannels(int index) {
        nativeSetAudioChannels(index);
    }

    public int getAudioChannels() {
        return nativeGetAudioChannels();
    }

    public void setVolume(int percent) {
        if (percent >= 0 && percent <= 100) {
            volumePercent = percent;
            nativeSetVolume(percent);
        }
    }

    public int getVolumePercent() {
        return volumePercent;
    }

    public void setVolMute(boolean mute) {
        nativeSetVolMute(mute);
    }

    public void setChannelMute(MuteEnum mute) {
        muteEnum = mute;
        nativeSetChannelMute(mute.getValue());
    }


    public void setAudioEffect(AudioEffect effect) {
        nativeSetAudioEffect(effect);
    }

    public float getPitch() {
        return pitch;
    }

    public void setOnPreparedListener(OnPreparedListener onPreparedListener) {
        this.mOnPreparedListener = onPreparedListener;
    }


    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.mOnErrorListener = onErrorListener;
    }

    public void prepared() {
        if (TextUtils.isEmpty(dataSource)) {
            onError(Status.STATUS_DATASOURCE_NULL, "datasource is null");
            return;
        }
        parpared = true;
        if (isOnlyMusic) {
            nativePrepared(dataSource, isOnlyMusic);
        } else {
            if (surface != null) {
                LogUtils.d("prepared start");
                nativePrepared(dataSource, isOnlyMusic);
            }
        }
    }

    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(dataSource)) {
                    onError(Status.STATUS_DATASOURCE_NULL, "datasource is null");
                    return;
                }
                if (!isOnlyMusic) {
                    if (surface == null) {
                        onError(Status.STATUS_SURFACE_NULL, "surface is null");
                        return;
                    }
                }

                if (mTimeBean == null) {
                    mTimeBean = new TimeBean();
                }
                LogUtils.i("start native");
                nativeStart();
            }
        }).start();
    }

    public void stop(final boolean exit) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                nativeStop(exit);
                if (exit && mediaCodec != null) {
                    try {
                        mediaCodec.flush();
                        mediaCodec.stop();
                        mediaCodec.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mediaCodec = null;
                    mediaFormat = null;
                }
                if (glSurfaceView != null) {
                    glSurfaceView.setCodecType(-1);
                    glSurfaceView.requestRender();

                    if (isOnlySoft() && glSurfaceViewExtend != null) {
                        glSurfaceViewExtend.setCodecType(-1);
                        glSurfaceViewExtend.requestRender();
                    }
                }

            }
        }).start();
    }

    public void pause() {
        nativePause();

    }

    public void resume() {
        nativeResume();
    }

    public void seek(final int secds) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                nativeSeek(secds);
                lastCurrTime = secds;
            }
        }).start();
    }

    public void setOnlySoft(boolean soft) {
        this.isOnlySoft = soft;
    }

    public boolean isOnlySoft() {
        return isOnlySoft;
    }


    private void onLoad(boolean load) {
        if (mOnLoadListener != null) {
            mOnLoadListener.onLoad(load);
        }
    }

    private void onError(int code, String msg) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(code, msg);
        }
        stop(true);
    }

    private void onParpared() {
        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared();
        }
    }

    public void mediacodecInit(int mimetype, int width, int height, byte[] csd0, byte[] csd1) {
        LogUtils.i("************* mediacodecInit");
        if (surface != null) {
            if (mediaCodec == null) {
                try {

                    String mtype = getMimeType(mimetype);
                    mediaFormat = MediaFormat.createVideoFormat(mtype, width, height);
                    LogUtils.i("mediacodecInit: " + mediaFormat.getString(MediaFormat.KEY_MIME));
//                    mediaFormat=  getMimeType_();

                    mediaFormat.setInteger(MediaFormat.KEY_WIDTH, width);
                    mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, height);
                    mediaFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, width * height);
                    mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
                    mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1));
                    LogUtils.i(mediaFormat.toString());
                    mediaCodec = MediaCodec.createDecoderByType(mtype);
                    if (surface != null) {
                        mediaCodec.configure(mediaFormat, this.surface, null, 0);
                        mediaCodec.start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                String mtype = getMimeType(mimetype);
                mediaFormat.setString(MediaFormat.KEY_MIME, mtype);
                LogUtils.i("init mediaCodec mtype: " + mtype);
//                mediaCodec.stop();
                mediaCodec.reset();

                mediaCodec.configure(mediaFormat, this.surface, null, 0);
                mediaCodec.start();

            }
        } else {
            if (mOnErrorListener != null) {
                mOnErrorListener.onError(Status.STATUS_SURFACE_NULL, "surface is null");
            }
        }
    }

    public void mediacodecDecode(byte[] bytes, int size, int pts) {
        if (bytes != null && mediaCodec != null && info != null) {
            try {
                int inputBufferIndex = mediaCodec.dequeueInputBuffer(10);
                if (inputBufferIndex >= 0) {
                    ByteBuffer byteBuffer = mediaCodec.getInputBuffers()[inputBufferIndex];
                    byteBuffer.clear();
                    byteBuffer.put(bytes);
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, size, pts, 0);
                }
                int index = mediaCodec.dequeueOutputBuffer(info, 10);
                while (index >= 0) {
                    mediaCodec.releaseOutputBuffer(index, true);
                    index = mediaCodec.dequeueOutputBuffer(info, 10);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setOnLoadListener(OnLoadListener onLoadListener) {
        this.mOnLoadListener = onLoadListener;
    }

    private String getMimeType(int type) {
        if (type == 1) {
            return "video/avc";
        } else if (type == 2) {
            return "video/hevc";
        } else if (type == 3) {
            return "video/mp4v-es";
        } else if (type == 4) {
            return "video/x-ms-wmv";
        } else if (type == 5) {
            return "video/mpeg2";
        } else if(type == 6) {
            return "video/ffmpeg";
        }

        return "";
    }

    private MediaFormat getMimeType_() {
        MediaExtractor extractor = null;
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(this.dataSource);
            int trackIndex = selectTrack(extractor);
            extractor.selectTrack(trackIndex);

            MediaFormat format = extractor.getTrackFormat(trackIndex);
            return format;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (extractor != null) {
                extractor.release();
            }
        }

        return null;

    }

    private static int selectTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                return i;
            }
        }

        return -1;
    }

    /*
     * 软解上传image frame数据
     *
     * */
    public void setFrameData(int w, int h, byte[] y, byte[] u, byte[] v) {
        if (glSurfaceView != null) {
            MyLog.d("setFrameData");
            glSurfaceView.setCodecType(0);
            glSurfaceView.setFrameData(w, h, y, u, v);

            if (isOnlySoft() && glSurfaceViewExtend != null) {
                glSurfaceViewExtend.setCodecType(0);
                glSurfaceViewExtend.setFrameData(w, h, y, u, v);
            }
        }
    }

    public void setOnInfoListener(OnInfoListener onInfoListener) {
        this.mOnInfoListener = onInfoListener;
    }

    public void setVideoInfo(int currt_secd, int total_secd) {
        if (mOnInfoListener != null && mTimeBean != null) {
            if (currt_secd < lastCurrTime) {
                currt_secd = lastCurrTime;
            }
            mTimeBean.setCurrt_secds(currt_secd);
            mTimeBean.setTotal_secds(total_secd);
            mOnInfoListener.onInfo(mTimeBean);
            lastCurrTime = currt_secd;
        }
    }

    public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
        this.mOnCompleteListener = onCompleteListener;
    }

    public void videoComplete() {
        if (mOnCompleteListener != null) {
            setVideoInfo(nativeGetDuration(), nativeGetDuration());
            mTimeBean = null;
            mOnCompleteListener.onComplete();

            if (!isOnlySoft()) {
                if (mediaCodec != null) {
                    mediaCodec.flush();
                }
            }
        }
    }


    public void setOnStopListener(OnStopListener onStopListener) {
        this.mOnStopListener = onStopListener;
    }

    public void onStopComplete() {
        if (mOnStopListener != null) {
            mOnStopListener.onStop();
        }
    }
}
