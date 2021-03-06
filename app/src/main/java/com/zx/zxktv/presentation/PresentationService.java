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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Presentation;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
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
import com.zx.zxktv.ui.MainActivity;
import com.zx.zxktv.ui.widget.VideoPlayListmanager;
import com.zx.zxktv.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class PresentationService extends Service implements OnFrameAvailableListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private final static String TAG = "PresentationService";
    private final static int PROGRESS_DETAL = 2000;
    private final IBinder mBinder = new LocalBinder();
    private DisplayManager mDisplayManager;
    private GiftPresentation giftDisplay;
    private MutilVideoPresentation mMutilVideoPresentation;
    private VideoPresentation mVideoPresentation;

    private ArrayList<GLPlayRenderThread> mThreadList = new ArrayList<GLPlayRenderThread>();
    private SurfaceTexture mSurfaceTexture = null;
    private MediaPlayer mMediaPlayer = null;
    private String mPath;
    private ArrayList<File> mFileList = new ArrayList<File>();
    private int mCurrentIndex = 0;
    private Song mCurSong;
    static private PresentationService sPresentationService = null;
    public static final boolean USE_TEXTURE_VIEW = false;

    private boolean isPause = false, playing = false;

    private int mAudioTrack[] = new int[16];
    private int audioTrack = 0;

    private HashMap<Integer, MediaPlayer.TrackInfo> trackInfoHashMap = new HashMap<>();
    private ArrayList<Integer> mTrackIndex = new ArrayList<>();
    private AudioManager mAudioManager;
    private PowerManager.WakeLock wl;

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

        PowerManager pm = (PowerManager) getSystemService(
                Context.POWER_SERVICE);
        wl = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK
                        | PowerManager.ON_AFTER_RELEASE | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                TAG);
        wl.acquire();

        setServiceForeground();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wl != null) {
            wl.release();
        }

        dismissGiftPresentation();
        dismissGiftPresentation();
        dismissMultiVideoPresentation();

        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //Log.i(TAG,"onFrameAvailable");
        int size = mThreadList.size();
        for (int i = 0; i < size; i++) {
            GLPlayRenderThread t = mThreadList.get(i);
            synchronized (t) {
                t.notify();
            }
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        MediaPlayer.TrackInfo[] trackInfos = mp.getTrackInfo();
        mTrackIndex.clear();

        if (trackInfos != null && trackInfos.length > 0) {
            int audioNum = 0;
            LogUtils.i("TrackInfo size =  " + trackInfos.length);
            for (int i = 0; i < trackInfos.length; i++) {
                if (trackInfos[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                    mAudioTrack[audioNum++] = i;
//                    trackInfoHashMap.put(i, trackInfos[i]);
                    mTrackIndex.add(i);
                }
            }
        }
        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i(TAG, "---onCompletion---");

        if (VideoPlayListmanager.getIntanse().getPlaySongSize() == 1) {
            playVideo(VideoPlayListmanager.getIntanse().getTop());
        } else {
            VideoPlayListmanager.getIntanse().removeTop();

            if (VideoPlayListmanager.getIntanse().getPlaySongSize() == 0) {
                mVideoPresentation.showPlaylistbottom(true);
                return;
            }

            Song song = VideoPlayListmanager.getIntanse().getTop();
            mCurSong = song;

            mVideoPresentation.updatePlayInfo(mCurSong);

        }

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    public void attachFrameLayout(FrameLayout fl) {
        Log.i(TAG, "attachFrameLayout");
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

    //=======================
    class SurfaceCallback implements SurfaceHolder.Callback {
        private GLPlayRenderThread mThread;

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "onSurfaceCreated");
            mThread = new GLPlayRenderThread(holder);

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

    //=======================
    public static PresentationService getAppInstance() {
        return sPresentationService;
    }

    synchronized public void startPlay(int texture) {
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

            mSurfaceTexture.detachFromGLContext();
        } else {
            //mSurfaceTexture.attachToGLContext(texture);
        }
    }

    public void updatePlayPreview() {
        mSurfaceTexture.updateTexImage();
    }

    public void attachPlayTexture(int texture) {
        mSurfaceTexture.attachToGLContext(texture);
    }

    public void detachPlayTexture() {
        mSurfaceTexture.detachFromGLContext();
    }

    private void setServiceForeground() {
        CharSequence text = getText(R.string.app_name);
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);


    }

    public void playVideo(String url, int index) {
        playVideo(url);
        mCurrentIndex = index;
    }

    public void playVideo(Song song) {
        playVideo(song.filePath);
        mCurrentIndex = VideoPlayListmanager.getIntanse().getSongIndex(song);
        mCurSong = song;
        mVideoPresentation.updatePlayInfo(song);
    }

    private void playVideo(String url) {
        LogUtils.i("url = " + url);
        mPath = url;
        mMediaPlayer.reset();

        try {
            mMediaPlayer.setDataSource(mPath);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.prepareAsync();
        playing = true;
    }


    public void pauseOrStart() {
        if (mMediaPlayer.isPlaying() && !isPause) {
            isPause = true;
            mMediaPlayer.pause();
        } else {
            isPause = false;
            mMediaPlayer.start();
        }
    }


    public void selectAudioTrack(int index) {
//        int i = mMediaPlayer.getSelectedTrack(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO);

//               LogUtils.i( " mAudioTrack.length = "
//                + mAudioTrack.length + " " + trackInfoHashMap.size()
//         + " select = " + i);
        if (mTrackIndex == null || index > mTrackIndex.size() - 1) {
            mMediaPlayer.selectTrack(mTrackIndex.get(0));
            return;
        }
        int trackIndex = mTrackIndex.get(index);
        mMediaPlayer.selectTrack(trackIndex);

        LogUtils.i(" index: " + index + " + trackIndex: " + trackIndex);

//        if (mMediaPlayer != null && mAudioTrack.length > 0 ) {
//            if (audioTrack > 0) {
//                audioTrack = 0;
//            } else {
//                audioTrack = 1;
//            }
//            mMediaPlayer.selectTrack(mAudioTrack[audioTrack]);
//        }
    }

    public int getAudioTrackSize() {

        return mTrackIndex != null ? mTrackIndex.size() : 0;
    }

    public int getSelectAudioTrackIndex() {
        int index = -1;
        if (mMediaPlayer != null) {
            try {
                int trackIndex = mMediaPlayer.getSelectedTrack(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO);
                index = mTrackIndex.indexOf(trackIndex);
                LogUtils.i("index = " + index);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        return index;
    }

    public void switchAccompanimentOrOriginal(boolean origianl) {
        LogUtils.i(" " + mAudioManager.getParameters("channelmask_value")
                + " origianl " + origianl);
        if (origianl) {
            mAudioManager.setParameters("channelmask_value=1");
            LogUtils.i(" select_channel=all ");
            selectAudioTrack(1);
        } else {
            mAudioManager.setParameters("channelmask_value=2");
            LogUtils.i(" select_channel=right ");
            selectAudioTrack(0);


        }
    }

    public void silentSwitchOn() {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(1.0f, 1.0f);
        }
    }

    public void silentSwitchOff() {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(0, 0);
        }
    }


    public void playForward() {
        LogUtils.i(TAG, "playForward");
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            int pos = mMediaPlayer.getCurrentPosition();
            pos += PROGRESS_DETAL;
            mMediaPlayer.seekTo(pos);
            LogUtils.i(TAG, "playForward 1");
        }

    }

    public void playBack() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            int pos = mMediaPlayer.getCurrentPosition();
            pos -= PROGRESS_DETAL;
            mMediaPlayer.seekTo(pos);
        }
    }


    public void nextVideo() {
        VideoPlayListmanager.getIntanse().removeTop();

        Song song = VideoPlayListmanager.getIntanse().getTop();
        if (null == song) {
            return;
        }

        playVideo(song);
        mCurSong = song;

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

            mMutilVideoPresentation = new MutilVideoPresentation(getApplicationContext(), displays[1], R.style.dialog);
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
                    Log.d(TAG, "Display #" + displayId + " added.");
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    Log.d(TAG, "Display #" + displayId + " changed.");
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                    Log.d(TAG, "Display #" + displayId + " removed.");
                }
            };

    private final DialogInterface.OnDismissListener mOnDismissListener =
            new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    Presentation presentation = (Presentation) dialog;
                    int displayId = presentation.getDisplay().getDisplayId();
                    Log.d(TAG, "Presentation on display #" + displayId + " was dismissed.");
                }
            };

    class TextureCallback implements TextureView.SurfaceTextureListener {

        private GLPlayRenderThread mThread;

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {
            mThread = new GLPlayRenderThread(surface);
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

    public class LocalBinder extends Binder {
        public PresentationService getService() {
            return PresentationService.this;
        }
    }
}
