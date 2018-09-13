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

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.zx.zxktv.R;
import com.zx.zxktv.data.Song;
import com.zx.zxktv.ui.view.MarqueeTextView;
import com.zx.zxktv.ui.widget.VideoPlayListmanager;
import com.zx.zxktv.utils.FileSystemUtil;
import com.zx.zxktv.utils.LogUtils;

public class VideoPresentation extends Presentation {

    private FrameLayout mFrameLayout;

    private MarqueeTextView mtv_SongPlayInfo;
    private TextView tv_playingBottom;
    private VideoBean mCurVideo;

    private Handler mHandler = new Handler();

    public VideoPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_presentation);
        mFrameLayout = (FrameLayout) findViewById(R.id.framelayout2);
        PresentationService.getAppInstance().attachFrameLayout(mFrameLayout);

        mtv_SongPlayInfo = (MarqueeTextView) findViewById(R.id.song_info);
        tv_playingBottom = (TextView) findViewById(R.id.list_play_complete);

        String baseInfo = getResources().getString(R.string.video_play_info);
        String info = String.format(baseInfo, " ",
                " ");
        mtv_SongPlayInfo.setText(info);
    }

    public void updatePlayInfo(final Song videoBean) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                onUpdatePlayInfo(videoBean);
            }
        });
    }

    private void onUpdatePlayInfo(Song videoBean) {
        String baseInfo = getResources().getString(R.string.video_play_info);
        int index = VideoPlayListmanager.getIntanse().getSongIndex(videoBean);
        LogUtils.i(" index = " + index);
        String nextVideoInfo = getResources().getString(R.string.video_play_complete);
        if (index >= 0 && VideoPlayListmanager.getIntanse().getPlaySongSize() >= 2) {
            Song nextSong = VideoPlayListmanager.getIntanse().getSongByIndex(index + 1);
            nextVideoInfo = FileSystemUtil.getFileName(nextSong.name);
            LogUtils.i(" " + nextVideoInfo);
        }
        String info = String.format(baseInfo, FileSystemUtil.getFileName(videoBean.name),
                nextVideoInfo);
        mtv_SongPlayInfo.setText(info);
    }

    public void showPlaylistbottom(final boolean show) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                onShowPlaylistbottom(show);
            }
        });
    }

    private void onShowPlaylistbottom(boolean show) {
        tv_playingBottom.setVisibility(show ? View.VISIBLE : View.GONE);
    }

}
