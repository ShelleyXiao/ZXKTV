package com.zx.zxktv.lib.libijkplayer;

import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Surface;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;

public class XMediaPlayer implements IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener,
        IMediaPlayer.OnBufferingUpdateListener, IMediaPlayer.OnSeekCompleteListener, IMediaPlayer.OnErrorListener,
        IMediaPlayer.OnVideoSizeChangedListener, IMediaPlayer.OnInfoListener, IXMediaPlayer {

    private static final String TAG = XMediaPlayer.class.getSimpleName();

    private IjkMediaPlayer mMediaPlayer;
    private Surface mSurface;
    private XMediaPlayerListener mMediaPlayerListener;

    private int mCurrentState = STATE_IDLE; // 当前状态
    private long mCurrentPosition; // 记录当前的播放进度值

    protected int mVideoWidth;
    protected int mVideoHeight;
    protected int mVideoSarNum;
    protected int mVideoSarDen;

    public XMediaPlayer() {
        mMediaPlayer = createPlayer();
        LogUtils.i("XMediaPlayer-->init");
    }

    private IjkMediaPlayer createPlayer() {
        IjkMediaPlayer ijkMediaPlayer = new IjkMediaPlayer();
        IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT);

        //open mediacodec
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec_mpeg4", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);

        //accurate seek
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);

        //开启openssl es 切换音轨会有延迟
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", 10000000);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 1);

        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 2);

        ijkMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        ijkMediaPlayer.setScreenOnWhilePlaying(true);
        ijkMediaPlayer.setOnNativeInvokeListener(new IjkMediaPlayer.OnNativeInvokeListener() {
            @Override
            public boolean onNativeInvoke(int i, Bundle bundle) {
                return true;
            }
        });

        ijkMediaPlayer.setOnPreparedListener(this);
        ijkMediaPlayer.setOnVideoSizeChangedListener(this);
        ijkMediaPlayer.setOnInfoListener(this);
        ijkMediaPlayer.setOnBufferingUpdateListener(this);
        ijkMediaPlayer.setOnSeekCompleteListener(this);
        ijkMediaPlayer.setOnCompletionListener(this);
        ijkMediaPlayer.setOnErrorListener(this);

        // 开启调试的LOG
        IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT);

        return ijkMediaPlayer;
    }

    @Override
    public void setMediaPlayerListener(XMediaPlayerListener mediaPlayerListener) {
        mMediaPlayerListener = mediaPlayerListener;
    }

    @Override
    public void setVideoPath(String path) {
        if(TextUtils.isEmpty(path)) {
            return;
        }

        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.setDataSource(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setSurface(Surface surface) {
        if (mMediaPlayer != null) {
            if (surface != null && surface.isValid()) {
                mSurface = surface;
                mMediaPlayer.setSurface(surface);
            }
        }
    }

    @Override
    public Surface getSurface() {
        return mSurface;
    }

    @Override
    public void setLooping(boolean looping) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setLooping(looping);
        }
    }

    @Override
    public int getCurrentState() {
        return mCurrentState;
    }

    @Override
    public boolean isPlaying() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getDuration();
        }

        return -1;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null
                && mCurrentState != STATE_ERROR // 没有发生错误
                && mCurrentState != STATE_IDLE // 不是处于空闲状态
                && mCurrentState != STATE_PREPARING); // 不是准备中
    }

    @Override
    public void setNeedMute(boolean needMute) {
        if (mMediaPlayer != null) {
            if (needMute) {
                mMediaPlayer.setVolume(0, 0);
            } else {
                mMediaPlayer.setVolume(1, 1);
            }
        }
    }

    @Override
    public void seekTo(long pos) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(pos);
        }
    }

    @Override
    public void setSpeed(float speed) {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.setSpeed(speed);
            }
        }
    }



    @Override
    public void play() {
        if (mMediaPlayer == null) {
            return;
        }

        if (mCurrentState == STATE_PREPARING
                || mCurrentState == STATE_PREPARED
                || mCurrentState == STATE_PLAYING) {
            return;
        }

        if(mCurrentState == STATE_IDLE) {
            // 空闲状态、闲置的
            mCurrentPosition = 0;
            mCurrentState = STATE_PREPARING;
            mMediaPlayer.prepareAsync();
        } else if (mCurrentState == STATE_PAUSED) {
            resume();
        } else if (mCurrentState == STATE_COMPLETED) {
            start();
        }
    }

    @Override
    public void start() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
            mCurrentPosition = 0;
        }
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mCurrentPosition = getCurrentPosition();
                LogUtils.i("pause() mCurrentPosition = " + mCurrentPosition);
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
    }

    @Override
    public void resume() {
        if (mCurrentState == STATE_PAUSED) {
            LogUtils.i("resume() mCurrentPosition = " + mCurrentPosition);
            if (mCurrentPosition > 0) {
                seekTo(mCurrentPosition);
            }
            start();
        }
    }

    @Override
    public void stop() {
        if (mCurrentState == STATE_PREPARED
                || mCurrentState == STATE_PLAYING
                || mCurrentState == STATE_PAUSED
                || mCurrentState == STATE_COMPLETED) {
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                LogUtils.i("XMediaPlayer-->stop()");
            }
        }
    }

    @Override
    public void reset() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            LogUtils.i("XMediaPlayer-->reset()");
            mCurrentState = STATE_IDLE;
            mCurrentPosition = 0;
        }
    }

    @Override
    public void releaseSurface() {
        if (mMediaPlayer != null) {
            LogUtils.i("XMediaPlayer-->releaseSurface");
            mMediaPlayer.setSurface(null);
        }

        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
    }

    @Override
    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            LogUtils.i("XMediaPlayer-->release");
        }
    }

    @Override
    public int getVideoWidth() {
        return mVideoWidth;
    }
    @Override
    public int getVideoHeight() {
        return mVideoHeight;
    }
    @Override
    public int getVideoSarNum() {
        return mVideoSarNum;
    }
    @Override
    public int getVideoSarDen() {
        return mVideoSarDen;
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        mCurrentState = STATE_PREPARED;
        if (mMediaPlayerListener != null) {
            mMediaPlayerListener.onPrepared(this);
        }
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDe) {
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();
        mVideoSarNum = mp.getVideoSarNum();
        mVideoSarDen = mp.getVideoSarDen();

        if (mMediaPlayerListener != null) {
            mMediaPlayerListener.onVideoSizeChanged(this, width, height, sarNum, sarDe);
        }
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int i) {
        if (mMediaPlayerListener != null) {
            mMediaPlayerListener.onBufferingUpdate(i);
        }
    }

    @Override
    public void onSeekComplete(IMediaPlayer iMediaPlayer) {
        if (mMediaPlayerListener != null) {
            mMediaPlayerListener.onSeekComplete();
        }
    }

    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {
        mCurrentState = STATE_COMPLETED;
        if (mMediaPlayerListener != null) {
            mMediaPlayerListener.onCompletion();
        }
    }

    @Override
    public boolean onError(IMediaPlayer iMediaPlayer, int framework_err, int impl_err) {
        LogUtils.i("Error: " + framework_err + "," + impl_err);

        mCurrentState = STATE_ERROR;
        if (mMediaPlayerListener != null) {
            mMediaPlayerListener.onError(framework_err, impl_err);
        }
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer iMediaPlayer, int what, int extra) {
        if (mMediaPlayerListener != null) {
            mMediaPlayerListener.onInfo(what, extra);
        }

//        LogUtils.i("TTT", "what="+what+"\n extra=" + extra);


//        int MEDIA_INFO_VIDEO_RENDERING_START = 3;//视频准备渲染
//        int MEDIA_INFO_BUFFERING_START = 701;//开始缓冲
//        int MEDIA_INFO_BUFFERING_END = 702;//缓冲结束
//        int MEDIA_INFO_VIDEO_ROTATION_CHANGED = 10001;//视频选择信息
//        int MEDIA_ERROR_SERVER_DIED = 100;//视频中断，一般是视频源异常或者不支持的视频类型。
//        int MEDIA_ERROR_IJK_PLAYER = -10000,//一般是视频源有问题或者数据格式不支持，比如音频不是AAC之类的
//        int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;//数据错误没有有效的回收


//        int MEDIA_INFO_UNKNOWN = 1;//未知信息
//        int MEDIA_INFO_STARTED_AS_NEXT = 2;//播放下一条
//        int MEDIA_INFO_VIDEO_RENDERING_START = 3;//视频开始整备中
//        int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;//视频日志跟踪
//        int MEDIA_INFO_BUFFERING_START = 701;//开始缓冲中
//        int MEDIA_INFO_BUFFERING_END = 702;//缓冲结束
//        int MEDIA_INFO_NETWORK_BANDWIDTH = 703;//网络带宽，网速方面
//        int MEDIA_INFO_BAD_INTERLEAVING = 800;//
//        int MEDIA_INFO_NOT_SEEKABLE = 801;//不可设置播放位置，直播方面
//        int MEDIA_INFO_METADATA_UPDATE = 802;//
//        int MEDIA_INFO_TIMED_TEXT_ERROR = 900;
//        int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;//不支持字幕
//        int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;//字幕超时
//
//        int MEDIA_INFO_VIDEO_INTERRUPT= -10000;//数据连接中断
//        int MEDIA_INFO_VIDEO_ROTATION_CHANGED = 10001;//视频方向改变
//        int MEDIA_INFO_AUDIO_RENDERING_START = 10002;//音频开始整备中
//
//        int MEDIA_ERROR_UNKNOWN = 1;//未知错误
//        int MEDIA_ERROR_SERVER_DIED = 100;//服务挂掉
//        int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;//数据错误没有有效的回收
//        int MEDIA_ERROR_IO = -1004;//IO错误
//        int MEDIA_ERROR_MALFORMED = -1007;
//        int MEDIA_ERROR_UNSUPPORTED = -1010;//数据不支持
//        int MEDIA_ERROR_TIMED_OUT = -110;//数据超时



        switch (what) {
            case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                LogUtils.d( "MEDIA_INFO_VIDEO_TRACK_LAGGING:");
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                LogUtils.d( "MEDIA_INFO_VIDEO_RENDERING_START:");
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                LogUtils.d( "MEDIA_INFO_BUFFERING_START:");
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                // 缓冲结束，开始播放，可以隐藏掉加载进度条

                LogUtils.d( "MEDIA_INFO_BUFFERING_END:");
                break;
            case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                LogUtils.d( "MEDIA_INFO_NETWORK_BANDWIDTH: " + extra);
                break;
            case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                LogUtils.d( "MEDIA_INFO_BAD_INTERLEAVING:");
                break;
            case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                LogUtils.d( "MEDIA_INFO_NOT_SEEKABLE:");
                break;
            case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                LogUtils.d( "MEDIA_INFO_METADATA_UPDATE:");
                break;
            case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                LogUtils.d( "MEDIA_INFO_UNSUPPORTED_SUBTITLE:");
                break;
            case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                LogUtils.d( "MEDIA_INFO_SUBTITLE_TIMED_OUT:");
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                LogUtils.d( "MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + extra);

//                mVideoRotationDegree = arg2;
//                LogUtils.d( "MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + arg2);
//                if (mRenderView != null)
//                    mRenderView.setVideoRotation(arg2);

                break;
            case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                LogUtils.d( "MEDIA_INFO_AUDIO_RENDERING_START:");
                break;
        }
        return true;
    }

    @Override
    public ITrackInfo[] getTrackInfo() {
        return mMediaPlayer.getTrackInfo();
    }

    @Override
    public int getSelectedTrack(int trackType) {
        return mMediaPlayer.getSelectedTrack(trackType);
    }

    @Override
    public void selectTrack(int stream) {
         mMediaPlayer.selectTrack(stream);
    }

    @Override
    public void deselectTrack(int stream) {
        mMediaPlayer.deselectTrack(stream);
    }

    public int getAudioTrakNums() {
        if(mMediaPlayer != null) {
            return mMediaPlayer.getAudioTrackNum();
        }

        return 0;
    }

    public void switchAudioTrakcIndex(int trackNums, int trackIndex) {
        if(mMediaPlayer != null) {
            mMediaPlayer.switchAudioTrack(trackNums, trackIndex);
        }
    }

    public void setPitch(float pitch) {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.setPitch(pitch);
            }
        }
    }


}
