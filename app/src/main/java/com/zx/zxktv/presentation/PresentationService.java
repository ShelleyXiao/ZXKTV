/*
 * Copyright 2011 WonderMedia Technologies, Inc. All Rights Reserved.
 *
 * This PROPRIETARY SOFTWARE is the property of WonderMedia Technologies, Inc.
 * and may contain trade secrets and/or other confidential information of
 * WonderMedia Technologies, Inc. This file shall not be disclosed to any third party,
 * in whole or in part, without prior written consent of WonderMedia.
 *
 * THIS PROPRIETARY SOFTWARE AND ANY RELATED DOCUMENTATION ARE PROVIDED AS IS,
 * WITH ALL FAULTS, AND WITHOUT WARRANTY OF ANY KIND EITHER EXPRESS OR IMPLIED,
 * AND WonderMedia TECHNOLOGIES, INC. DISCLAIMS ALL EXPRESS OR IMPLIED WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT.
 */

package com.zx.zxktv.presentation;

import android.annotation.SuppressLint;
import android.app.Presentation;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;

import com.zx.zxktv.R;
import com.zx.zxktv.data.Song;
import com.zx.zxktv.lib.libijkplayer.IXMediaPlayer;
import com.zx.zxktv.lib.libijkplayer.XMediaPlayer;
import com.zx.zxktv.lib.libijkplayer.XMediaPlayerListener;
import com.zx.zxktv.ui.widget.VideoPlayListmanager;
import com.zx.zxktv.utils.LogUtils;
import com.zx.zxktv.utils.SharePrefUtil;
import com.zx.zxktv.utils.rxbus.RxBus;
import com.zx.zxktv.utils.rxbus.RxConstants;

import java.util.ArrayList;

import io.reactivex.disposables.Disposable;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;
import tv.danmaku.ijk.media.player.misc.IjkTrackInfo;
import tv.danmaku.ijk.media.player.misc.TrackItem;

import static tv.danmaku.ijk.media.player.IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START;

public class PresentationService extends Service implements OnFrameAvailableListener,
        XMediaPlayerListener, GLPlayRenderThread.renderActionListener {


    private final static String TAG = "PresentationService";
    private final static int PROGRESS_DETAL = 1000;
    private final static int PROGRESS_BACK_DETAIL = 1000 * 2;
    private final IBinder mBinder = new LocalBinder();
    private DisplayManager mDisplayManager;
    private GiftPresentation giftDisplay;
    private MutilVideoPresentation2 mMutilVideoPresentation;
    private VideoPresentation mVideoPresentation;

    private ArrayList<GLPlayRenderThread> mThreadList = new ArrayList<GLPlayRenderThread>();
    private SurfaceTexture mSurfaceTexture = null;
    private String mPath;
    private Song mCurSong;
    static private PresentationService sPresentationService = null;
    public static final boolean USE_TEXTURE_VIEW = true;

    private boolean isPause = false, playing = false;

    private AudioManager mAudioManager;

    private int mTotalAudioTracks = 0;
    private ArrayList<TrackItem> mTrackItems = new ArrayList<>();
    private int mCurrentAudioTrackIndex = 2; //默认伴唱
    private boolean isOriginal = false;

    //[-3, 3] 0代表正常不变调
    private float pitchShiftLevel = 0;
    private float accompanyPitch = (float) Math.pow(1.059463094359295, pitchShiftLevel);

    private Disposable mDisposable;

    private XMediaPlayer mIjkPlayer;

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                mIjkPlayer = new XMediaPlayer();
                if (mSurfaceTexture != null && !mSurfaceTexture.isReleased()) {
                    LogUtils.i("replay &*************");
                    mIjkPlayer.setSurface(new Surface(mSurfaceTexture));
                    mIjkPlayer.setVideoPath(mCurSong.filePath);
                    mIjkPlayer.setMediaPlayerListener(PresentationService.this);
                    mIjkPlayer.play();
                    playing = true;
                }

            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        sPresentationService = this;

        mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        mDisplayManager.registerDisplayListener(mDisplayListener, null);
        Display[] displays = mDisplayManager.getDisplays(null);
        if (displays.length > 1) {
            mVideoPresentation = new VideoPresentation(this, displays[1]);

            mVideoPresentation.getWindow().setType(LayoutParams.TYPE_SYSTEM_ALERT);
            mVideoPresentation.getWindow().setFlags(LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    LayoutParams.FLAG_HARDWARE_ACCELERATED);
            mVideoPresentation.show();
            mVideoPresentation.setOnDismissListener(mOnDismissListener);

        }

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


//        setServiceForeground();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mIjkPlayer.releaseSurface();
        mIjkPlayer.release();

        if (mDisposable != null) {
            mDisposable.dispose();
        }

        dismissKTVPresentation();
        dismissGiftPresentation();
        dismissMultiVideoPresentation();

    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
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

    public void setPitch(float pitchShiftLevel) {
        LogUtils.i("************** pitchShiftLevel = " + pitchShiftLevel);
        this.pitchShiftLevel = pitchShiftLevel;
        accompanyPitch = (float) Math.pow(1.059463094359295, pitchShiftLevel);
        mIjkPlayer.setPitch(accompanyPitch);
    }

    public float getPitchShiftLevel() {
        return this.pitchShiftLevel;
    }


    @Override
    public void onPrepared(IXMediaPlayer mp) {
        ITrackInfo[] trackInfos = mIjkPlayer.getTrackInfo();
        mTrackItems.clear();
        for (int i = 0; i < trackInfos.length; i++) {
            TrackItem item = new TrackItem(i, trackInfos[i]);
            mTrackItems.add(item);
        }
        mIjkPlayer.start();
        //获取原唱音轨索引
        mCurrentAudioTrackIndex = mIjkPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO);

        LogUtils.i(" track size = " + mTrackItems.size()
                + " audio track index: " + mCurrentAudioTrackIndex);
    }

    @Override
    public void onBufferingUpdate(int percent) {

    }

    @Override
    public void onSeekComplete() {

    }

    @Override
    public void onError(int what, int extra) {
        nextVideo();
    }

    @Override
    public void onInfo(int what, int extra) {
        if (what == MEDIA_INFO_AUDIO_RENDERING_START) {
            mTotalAudioTracks = mIjkPlayer.getAudioTrakNums();
            if (mTotalAudioTracks > 1) {
                mCurrentAudioTrackIndex++;
//                mIjkPlayer.switchAudioTrakcIndex(mTotalAudioTracks, mCurrentTrackIndex);
                mIjkPlayer.selectTrack(mCurrentAudioTrackIndex);
                LogUtils.i(" " + mCurrentAudioTrackIndex);
            } else {
                mAudioManager.setParameters("channelmask_value=2");
            }
            isOriginal = false;
            Bundle extraData = new Bundle();
            extraData.putBoolean("check", isOriginal);
            RxBus.getDefault().postWithCode(RxConstants.SYNC_ORIGINAL_UI_CODE, extraData);

            setPitch(0.0f);
        }
    }

    @Override
    public void onVideoSizeChanged(IXMediaPlayer mp, int width, int height, int sarNum, int sarDe) {

    }

    @Override
    public void onCompletion() {
        LogUtils.i("---onCompletion---");

        if (VideoPlayListmanager.getIntanse().getPlaySongSize() == 0) {
            mVideoPresentation.showPlaylistbottom(true);
            return;
        }

        Song song = VideoPlayListmanager.getIntanse().getTop();
        mCurSong = song;
        nextVideo();
        RxBus.getDefault().postWithCode(RxConstants.UPDATE_SELECT_SONG_CODE, RxConstants.EXTRA_KEY_UPDATE_SELECT);
    }


    public void attachFrameLayout(FrameLayout fl) {
        if (USE_TEXTURE_VIEW) {

            TextureView texture = new TextureView(fl.getContext());
            texture.setSurfaceTextureListener(new TextureCallback());
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            fl.addView(texture, params);
        } else {
            SurfaceView surfaceView = new SurfaceView(fl.getContext());
            surfaceView.getHolder().addCallback(new SurfaceCallback());
            surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            //surfaceView.setZOrderMediaOverlay(true);

            fl.addView(surfaceView, params);

        }
    }

    public static PresentationService getAppInstance() {
        return sPresentationService;
    }

    @Override
    synchronized public void startPlay(int texture) {
        if (mSurfaceTexture == null) {
            mSurfaceTexture = new SurfaceTexture(texture);
            mSurfaceTexture.setOnFrameAvailableListener(this);
            mIjkPlayer = new XMediaPlayer();
            mIjkPlayer.setSurface(new Surface(mSurfaceTexture));
            mSurfaceTexture.detachFromGLContext();
            mIjkPlayer.setMediaPlayerListener(this);
        }
    }

    @Override
    public void updatePlayPreview() {
        mSurfaceTexture.updateTexImage();
    }

    @Override
    public void attachPlayTexture(int texture) {
        mSurfaceTexture.attachToGLContext(texture);
    }

    @Override
    public void detachPlayTexture() {
        mSurfaceTexture.detachFromGLContext();
    }

    public void playVideo(String url, int index) {
        playVideo(url);
    }

    public void playVideo(Song song) {
        playVideo(song.filePath);
        mCurSong = song;

        mVideoPresentation.updatePlayInfo(mCurSong);
    }

    private void playVideo(String url) {
        LogUtils.i("url = " + url);
        mPath = url;
        mIjkPlayer.setVideoPath(url);
        mIjkPlayer.play();
    }

    public void songResing() {
//        mIjkPlayer.seekTo(0);

        mIjkPlayer.stop();
        mIjkPlayer.releaseSurface();
        mIjkPlayer.release();

        mVideoPresentation.updatePlayInfo(mCurSong);
        Message message = Message.obtain();
        message.what = 1;
        handler.sendMessageAtTime(message, 500);
    }

    public void pauseOrStart() {
        if (!isPause) {
            isPause = true;
            mIjkPlayer.pause();
        } else {
            isPause = false;
            mIjkPlayer.start();
        }
    }

    public void switchAccompanimentOrOriginal(boolean origianl) {
        if (origianl) {
            if (mTotalAudioTracks <= 1) {
                mAudioManager.setParameters("channelmask_value=1");
                LogUtils.i(" sorigianl true ");
                SharePrefUtil.saveString(getApplicationContext(), "channelmask",
                        "channelmask_value=1");
            } else {
                if (mIjkPlayer != null) {
                    mCurrentAudioTrackIndex--;
                    int audioTrackIndex = mIjkPlayer.getSelectedTrack(IjkTrackInfo.MEDIA_TRACK_TYPE_AUDIO);
                    if (audioTrackIndex != mCurrentAudioTrackIndex) {
                        mIjkPlayer.selectTrack(mCurrentAudioTrackIndex);
                    }
                }
            }
        } else {
            if (mTotalAudioTracks <= 1) {
                mAudioManager.setParameters("channelmask_value=2");
                LogUtils.i(" sorigianl false ");
                SharePrefUtil.saveString(getApplicationContext(), "channelmask",
                        "channelmask_value=2");
            } else {
                if (mIjkPlayer != null) {
                    mCurrentAudioTrackIndex++;
//                LogUtils.i(mIjkPlayer.getSelectedTrack(IjkTrackInfo.MEDIA_TRACK_TYPE_AUDIO));
                    int audioTrackIndex = mIjkPlayer.getSelectedTrack(IjkTrackInfo.MEDIA_TRACK_TYPE_AUDIO);
                    if (audioTrackIndex != mCurrentAudioTrackIndex) {
                        mIjkPlayer.selectTrack(mCurrentAudioTrackIndex);
                    }
                }
            }
        }
    }

    public void silentSwitchOn() {
        mIjkPlayer.setNeedMute(false);
    }

    public void silentSwitchOff() {
        mIjkPlayer.setNeedMute(true);
    }


    public void seekForward() {

        long progress = mIjkPlayer.getCurrentPosition();
        progress += PROGRESS_DETAL;
//        long pos = mIjkPlayer.getDuration() * progress / 100;
        if (progress <= mIjkPlayer.getDuration()) {
            mIjkPlayer.seekTo(progress);
        }

    }

    public void seekBack() {
        long progress = mIjkPlayer.getCurrentPosition();
        progress -= PROGRESS_BACK_DETAIL;
        if (progress >= 0) {
            mIjkPlayer.seekTo(progress);
        }
    }


    public void nextVideo() {
        if (VideoPlayListmanager.getIntanse().getPlaySongSize() > 1) {
            VideoPlayListmanager.getIntanse().removeTop();
        }
        Song song = VideoPlayListmanager.getIntanse().getTop();
        if (null == song) {
            return;
        }

        mCurSong = song;
        mIjkPlayer.stop();
        mIjkPlayer.releaseSurface();
        mIjkPlayer.release();

        mVideoPresentation.updatePlayInfo(mCurSong);
        Message message = Message.obtain();
        message.what = 1;
        handler.sendMessageAtTime(message, 500);
    }

    public void updateDisplayInfo() {
        Song song = VideoPlayListmanager.getIntanse().getTop();
        if (null == song) {
            return;
        }

        mVideoPresentation.updatePlayInfo(mCurSong);
    }

    public boolean isPlaying() {
        return playing;
    }

    public void showGiftPresentation() {
        if (null == giftDisplay) {

            DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
            Display[] displays = displayManager.getDisplays(null);

            giftDisplay = new GiftPresentation(getApplicationContext(), displays[1], R.style.dialog);
            giftDisplay.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }

        if (!giftDisplay.isShowing()) {
            LogUtils.i("show gift");
            giftDisplay.show();
            giftDisplay.startAnimation();
        }
    }

    public void showMultiVideoPresentation() {
        if (null == mMutilVideoPresentation) {

            DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
            Display[] displays = displayManager.getDisplays(null);

            mMutilVideoPresentation = new MutilVideoPresentation2(getApplicationContext(), displays[1], R.style.dialog);
            mMutilVideoPresentation.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }

        if (!mMutilVideoPresentation.isShowing()) {
            LogUtils.i("show gift");
            mMutilVideoPresentation.show();
        }
    }

    private void dismissKTVPresentation() {
        if (mVideoPresentation != null && mVideoPresentation.isShowing()) {
            mVideoPresentation.dismiss();
        }
    }

    private void dismissGiftPresentation() {
        if (giftDisplay != null && giftDisplay.isShowing()) {
            giftDisplay.dismiss();
        }
    }

    public void dismissMultiVideoPresentation() {
        if (mMutilVideoPresentation != null && mMutilVideoPresentation.isShowing()) {
            mMutilVideoPresentation.destroy();
            mMutilVideoPresentation.dismiss();
        }
    }

    public boolean isMultiVideoShow() {
        return mMutilVideoPresentation != null ? mMutilVideoPresentation.isShowing() : false;
    }

    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                    LogUtils.i("Display #" + displayId + " added.");
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    LogUtils.i("Display #" + displayId + " changed.");
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                    LogUtils.i("Display #" + displayId + " removed.");
                }
            };

    private final DialogInterface.OnDismissListener mOnDismissListener =
            new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    Presentation presentation = (Presentation) dialog;
                    int displayId = presentation.getDisplay().getDisplayId();
                    LogUtils.i("Presentation on display #" + displayId + " was dismissed.");
                }
            };

    class TextureCallback implements TextureView.SurfaceTextureListener {

        private GLPlayRenderThread mThread;

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {
            mThread = new GLPlayRenderThread(sPresentationService, surface, sPresentationService);
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

    //=======================
    class SurfaceCallback implements SurfaceHolder.Callback {
        private GLPlayRenderThread mThread;

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "onSurfaceCreated");
            mThread = new GLPlayRenderThread(sPresentationService, holder, sPresentationService);

            Log.i(TAG, "width = " + holder.getSurfaceFrame().width());
            Log.i(TAG, "height = " + holder.getSurfaceFrame().height());
            mThread.setRegion(holder.getSurfaceFrame().width(), holder.getSurfaceFrame().height());
            synchronized (mThreadList) {
                mThreadList.add(mThread);
            }
            mThread.start();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            Log.i(TAG, "surfaceChanged");
            mThread.setRegion(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mThreadList.remove(mThread);
        }
    }

    public class LocalBinder extends Binder {
        public PresentationService getService() {
            return PresentationService.this;
        }
    }
}
