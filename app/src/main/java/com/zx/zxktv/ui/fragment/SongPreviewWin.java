package com.zx.zxktv.ui.fragment;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.zx.zxktv.R;
import com.zx.zxktv.data.Song;
import com.zx.zxktv.presentation.GiftPresentation;
import com.zx.zxktv.ui.widget.VideoPlayListmanager;
import com.zx.zxktv.utils.LogUtils;
import com.zx.zxktv.utils.ViewUtils;
import com.zx.zxktv.utils.rxbus.RxBus;
import com.zx.zxktv.utils.rxbus.RxConstants;

import java.io.IOException;


/**
 * User: zx
 * Date: 2018-06-22
 * Time: 10:50
 * Company: zx
 * Description:
 * FIXME
 */

@SuppressLint("ValidFragment")
public class SongPreviewWin extends DialogFragment implements View.OnClickListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private Song mCurrentSong;

    private Button btn_order;
    private Button btn_close;

    private DisplayManager mDisplayManager;
    private GiftPresentation mFlowerPresentation;

    private MediaPlayer mPreMediaPlayer;

    private TextureView mTextureView;
    private Surface mSurface;


    public SongPreviewWin() {
    }


    public SongPreviewWin(Song song) {
        mCurrentSong = song;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.song_preview_win_layout, container, true);

        final Window window = getDialog().getWindow();
        window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.width = (int) (ViewUtils.getScreenWidth(getActivity()) * 0.6);
        wlp.height = (int) (ViewUtils.getScreenHeight(getActivity()) * 0.8);
        window.setAttributes(wlp);

        v.findViewById(R.id.btn_close).setOnClickListener(this);
        v.findViewById(R.id.btn_order).setOnClickListener(this);

        mTextureView = (TextureView) v.findViewById(R.id.video_textrue);

        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mSurface = new Surface(surface);
                mPreMediaPlayer = new MediaPlayer();
                mPreMediaPlayer.setOnErrorListener(SongPreviewWin.this);
                mPreMediaPlayer.setOnPreparedListener(SongPreviewWin.this);
                mPreMediaPlayer.setOnCompletionListener(SongPreviewWin.this);

                mPreMediaPlayer.setVolume((float) 0, (float) 0);
                mPreMediaPlayer.setSurface(mSurface);
                LogUtils.i("************************create");

                new Handler().postAtTime(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mPreMediaPlayer.reset();
                            mPreMediaPlayer.setDataSource(mCurrentSong.filePath);
                            mPreMediaPlayer.prepareAsync();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 500);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                surface = null;
                mSurface = null;

                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
        return v;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        mVideoView.setOnPlayerEventListener(this);


    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();


    }

    @Override
    public void onStop() {
        super.onStop();
    }


    @Override
    public void onDestroy() {
        mPreMediaPlayer.stop();
        mPreMediaPlayer.release();
        mPreMediaPlayer = null;

        super.onDestroy();


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_close:
                dismiss();
                break;
            case R.id.btn_order:
                VideoPlayListmanager.getIntanse().addSong(mCurrentSong);
                RxBus.getDefault().postWithCode(RxConstants.UPDATE_SELECT_SONG_CODE, RxConstants.EXTRA_KEY_UPDATE_SELECT);
                break;
            default:
                break;
        }
    }


    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }
}
