package com.zx.zxktv.presentation;

import android.app.Presentation;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;

import com.zx.zxktv.R;
import com.zx.zxktv.utils.LogUtils;

import java.io.File;
import java.io.IOException;

/**
 * User: ShaudXiao
 * Date: 2018-08-21
 * Time: 09:19
 * Company: zx
 * Description:
 * FIXME
 */

public class MutilVideoPresentation extends Presentation {

    private static final String TEST_MOVIE = "/mnt/sdcard/media/test.mp4";
    private static final int VIDEO_COUNT = 6;

    // Must be static storage so they'll survive Activity restart.
    private static boolean sVideoRunning = false;
    private static VideoBlob[] sBlob = new VideoBlob[VIDEO_COUNT];


    public MutilVideoPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    public MutilVideoPresentation(Context outerContext, Display display, int theme) {
        super(outerContext, display, theme);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.presentation_mutil_video_decode);

        final Window window = getWindow();
        window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams wlp = window.getAttributes();

//        wlp.width = (int) (ViewUtils.getScreenWidth(getContext()) * 0.6);
//        wlp.height = (int) (ViewUtils.getScreenHeight(getContext()) * 0.6);

        wlp.gravity = Gravity.CENTER;
        window.setAttributes(wlp);


        sBlob[0] = new VideoBlob((TextureView) findViewById(R.id.texture_view_1_0),
                TEST_MOVIE, 0);
        sBlob[1] = new VideoBlob((TextureView) findViewById(R.id.texture_view_1_1),
                TEST_MOVIE, 1);
//        sBlob[2] = new VideoBlob((TextureView) findViewById(R.id.texture_view_1_2),
//                TEST_MOVIE, 2);

        sBlob[2] = new VideoBlob((TextureView) findViewById(R.id.texture_view_2_0),
                TEST_MOVIE, 2);
        sBlob[3] = new VideoBlob((TextureView) findViewById(R.id.texture_view_2_1),
                TEST_MOVIE, 3);

        sBlob[4] = new VideoBlob((TextureView) findViewById(R.id.texture_view_2_2),
                TEST_MOVIE, 4);

        sBlob[5] = new VideoBlob((TextureView) findViewById(R.id.texture_view_3_0),
                TEST_MOVIE, 6);
//        sBlob[7] = new VideoBlob((TextureView) findViewById(R.id.texture_view_3_1),
//                TEST_MOVIE, 7);
//        sBlob[8] = new VideoBlob((TextureView) findViewById(R.id.texture_view_3_2),
//                TEST_MOVIE, 8);

    }

    public void destroy() {
        for (int i = 0; i < VIDEO_COUNT; i++) {
            sBlob[i].stopPlayback();
            sBlob[i] = null;
        }
    }


    private static class VideoBlob implements TextureView.SurfaceTextureListener {
        private TextureView mTextureView;
        private File movieFile;

        private SurfaceTexture mSavedSurfaceTexture;
        private PlayMovieThread mPlayThread;
        private SpeedControlCallback mCallback;

        public VideoBlob(TextureView view, String filePath, int ordinal) {
            movieFile = new File(filePath);

            mCallback = new SpeedControlCallback();

            recreateView(view);
        }

        /**
         * Performs partial construction.  The VideoBlob is already created, but the Activity
         * was recreated, so we need to update our view.
         */
        public void recreateView(TextureView view) {
            mTextureView = view;
            mTextureView.setSurfaceTextureListener(this);
            if (mSavedSurfaceTexture != null) {
                view.setSurfaceTexture(mSavedSurfaceTexture);
            }
        }

        /**
         * Stop playback and shut everything down.
         */
        public void stopPlayback() {
            mPlayThread.requestStop();
            // TODO: wait for the playback thread to stop so we don't kill the Surface
            //       before the video stops

            // We don't need this any more, so null it out.  This also serves as a signal
            // to let onSurfaceTextureDestroyed() know that it can tell TextureView to
            // free the SurfaceTexture.
            mSavedSurfaceTexture = null;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {

            // If this is our first time though, we're going to use the SurfaceTexture that
            // the TextureView provided.  If not, we're going to replace the current one with
            // the original.

            if (mSavedSurfaceTexture == null) {
                mSavedSurfaceTexture = st;

                mPlayThread = new PlayMovieThread(movieFile, new Surface(st), mCallback);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {
            LogUtils.d("onSurfaceTextureSizeChanged size=" + width + "x" + height + ", st=" + st);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
            LogUtils.d("onSurfaceTextureDestroyed st=" + st);
            return (mSavedSurfaceTexture == null);
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture st) {

        }
    }

    private static class PlayMovieThread extends Thread {
        private final File mFile;
        private final Surface mSurface;
        private final SpeedControlCallback mCallback;
        private MoviePlayer mMoviePlayer;

        /**
         * Creates thread and starts execution.
         * <p>
         * The object takes ownership of the Surface, and will access it from the new thread.
         * When playback completes, the Surface will be released.
         */
        public PlayMovieThread(File file, Surface surface, SpeedControlCallback callback) {
            mFile = file;
            mSurface = surface;
            mCallback = callback;

            start();
        }

        /**
         * Asks MoviePlayer to halt playback.  Returns without waiting for playback to halt.
         * <p>
         * Call from UI thread.
         */
        public void requestStop() {
            mMoviePlayer.requestStop();
        }

        @Override
        public void run() {
            try {
                mMoviePlayer = new MoviePlayer(mFile, mSurface, mCallback);
                mMoviePlayer.setLoopMode(true);
                mMoviePlayer.play();
            } catch (IOException ioe) {
                LogUtils.e("movie playback failed" + ioe);
            } finally {
                mSurface.release();
                LogUtils.e("PlayMovieThread stopping");
            }
        }
    }

    private static class SpeedControlCallback implements MoviePlayer.FrameCallback {
        private static final String TAG = SpeedControlCallback.class.getSimpleName();
        private static final boolean CHECK_SLEEP_TIME = false;

        private static final long ONE_MILLION = 1000000L;

        private long mPrevPresentUsec;
        private long mPrevMonoUsec;
        private long mFixedFrameDurationUsec;
        private boolean mLoopReset;

        /**
         * Sets a fixed playback rate.  If set, this will ignore the presentation time stamp
         * in the video file.  Must be called before playback thread starts.
         */
        public void setFixedPlaybackRate(int fps) {
            mFixedFrameDurationUsec = ONE_MILLION / fps;
        }

        // runs on decode thread
        @Override
        public void preRender(long presentationTimeUsec) {
            // For the first frame, we grab the presentation time from the video
            // and the current monotonic clock time.  For subsequent frames, we
            // sleep for a bit to try to ensure that we're rendering frames at the
            // pace dictated by the video stream.
            //
            // If the frame rate is faster than vsync we should be dropping frames.  On
            // Android 4.4 this may not be happening.

            if (mPrevMonoUsec == 0) {
                // Latch current values, then return immediately.
                mPrevMonoUsec = System.nanoTime() / 1000;
                mPrevPresentUsec = presentationTimeUsec;
            } else {
                // Compute the desired time delta between the previous frame and this frame.
                long frameDelta;
                if (mLoopReset) {
                    // We don't get an indication of how long the last frame should appear
                    // on-screen, so we just throw a reasonable value in.  We could probably
                    // do better by using a previous frame duration or some sort of average;
                    // for now we just use 30fps.
                    mPrevPresentUsec = presentationTimeUsec - ONE_MILLION / 30;
                    mLoopReset = false;
                }
                if (mFixedFrameDurationUsec != 0) {
                    // Caller requested a fixed frame rate.  Ignore PTS.
                    frameDelta = mFixedFrameDurationUsec;
                } else {
                    frameDelta = presentationTimeUsec - mPrevPresentUsec;
                }
                if (frameDelta < 0) {
                    Log.w(TAG, "Weird, video times went backward");
                    frameDelta = 0;
                } else if (frameDelta == 0) {
                    // This suggests a possible bug in movie generation.
                    Log.i(TAG, "Warning: current frame and previous frame had same timestamp");
                } else if (frameDelta > 10 * ONE_MILLION) {
                    // Inter-frame times could be arbitrarily long.  For this player, we want
                    // to alert the developer that their movie might have issues (maybe they
                    // accidentally output timestamps in nsec rather than usec).
                    Log.i(TAG, "Inter-frame pause was " + (frameDelta / ONE_MILLION) +
                            "sec, capping at 5 sec");
                    frameDelta = 5 * ONE_MILLION;
                }

                long desiredUsec = mPrevMonoUsec + frameDelta;  // when we want to wake up
                long nowUsec = System.nanoTime() / 1000;
                while (nowUsec < (desiredUsec - 100) /*&& mState == RUNNING*/) {
                    // Sleep until it's time to wake up.  To be responsive to "stop" commands
                    // we're going to wake up every half a second even if the sleep is supposed
                    // to be longer (which should be rare).  The alternative would be
                    // to interrupt the thread, but that requires more work.
                    //
                    // The precision of the sleep call varies widely from one device to another;
                    // we may wake early or late.  Different devices will have a minimum possible
                    // sleep time. If we're within 100us of the target time, we'll probably
                    // overshoot if we try to sleep, so just go ahead and continue on.
                    long sleepTimeUsec = desiredUsec - nowUsec;
                    if (sleepTimeUsec > 500000) {
                        sleepTimeUsec = 500000;
                    }
                    try {
                        if (CHECK_SLEEP_TIME) {
                            long startNsec = System.nanoTime();
                            Thread.sleep(sleepTimeUsec / 1000, (int) (sleepTimeUsec % 1000) * 1000);
                            long actualSleepNsec = System.nanoTime() - startNsec;
                            Log.d(TAG, "sleep=" + sleepTimeUsec + " actual=" + (actualSleepNsec / 1000) +
                                    " diff=" + (Math.abs(actualSleepNsec / 1000 - sleepTimeUsec)) +
                                    " (usec)");
                        } else {
                            Thread.sleep(sleepTimeUsec / 1000, (int) (sleepTimeUsec % 1000) * 1000);
                        }
                    } catch (InterruptedException ie) {
                    }
                    nowUsec = System.nanoTime() / 1000;
                }

                // Advance times using calculated time values, not the post-sleep monotonic
                // clock time, to avoid drifting.
                mPrevMonoUsec += frameDelta;
                mPrevPresentUsec += frameDelta;
            }
        }

        // runs on decode thread
        @Override
        public void postRender() {
        }

        @Override
        public void loopReset() {
            mLoopReset = true;
        }

    }
}
