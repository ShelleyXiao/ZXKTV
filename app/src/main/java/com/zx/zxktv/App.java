package com.zx.zxktv;

import android.app.Application;

import com.zx.zxktv.ui.widget.VideoPlayListmanager;

/**
 * User: ShaudXiao
 * Date: 2018-06-20
 * Time: 14:33
 * Company: zx
 * Description: 简单DEMO
 * FIXME
 */

public class App extends Application {

    public static final int PLAN_ID_EXO = 1;

    private static App instance;

    public static App get(){
        return instance;
    }

    public VideoPlayListmanager mVideoPlayListmanager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        //播放器目前只是简单DEMO，后续要基于FFMEPG去做定制开发，
        // 解码应用自己去做，方便以后添加音视频特效
//        PlayerConfig.setUseDefaultNetworkEventProducer(false);
//
//        PlayerLibrary.init(this);
//
//        PlayerConfig.addDecoderPlan(new DecoderPlan(PLAN_ID_EXO, ExoMediaPlayer.class.getName(), "ExoPlayer"));

//        PlayerConfig.setDefaultPlanId(PLAN_ID_EXO);

        mVideoPlayListmanager = VideoPlayListmanager.getIntanse();
    }


}
