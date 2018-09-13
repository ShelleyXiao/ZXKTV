package com.zx.zxktv.data;

import android.app.Activity;
import android.database.Cursor;
import android.os.Handler;

import com.zx.zxktv.data.interfaces.VideoModelContract;
import com.zx.zxktv.utils.LogUtils;

/**
 * User: ShaudXiao
 * Date: 2018-06-26
 * Time: 14:13
 * Company: zx
 * Description:
 * FIXME
 */

public class LoadVideoModelPresenterImpl implements VideoModelContract.Presenter, VideoModelContract.ModelDataCallBack {


    private VideoModelContract.View view;
    private VideoModelContract.LoadingCallBack loadingCallBack;
    private VideoModelContract.ErrorCallBack errorCallBack;


    private VideoModelContract.Model model;

    private Handler mHandler = new Handler();

    public LoadVideoModelPresenterImpl(Activity activity, VideoModelContract.View view) {
        this.view = view;
        this.model = new VideoModelImpl(activity, this);
    }

    public LoadVideoModelPresenterImpl(Activity activity, VideoModelContract.View view,
                                       VideoModelContract.ErrorCallBack errorCallBack) {
        this.view = view;
        this.model = new VideoModelImpl(activity, this);
        this.errorCallBack = errorCallBack;
    }

    public LoadVideoModelPresenterImpl(VideoModelContract.Model model, VideoModelContract.View view,
                                       VideoModelContract.LoadingCallBack loadingCallBack) {
        this.view = view;
        this.model = model;
        this.loadingCallBack = loadingCallBack;
    }

    public LoadVideoModelPresenterImpl(VideoModelContract.Model model, VideoModelContract.View view,
                                       VideoModelContract.ErrorCallBack errorCallBack) {
        this.view = view;
        this.model = model;
        this.errorCallBack = errorCallBack;
    }

    public LoadVideoModelPresenterImpl(VideoModelContract.Model model, VideoModelContract.View view,
                                       VideoModelContract.LoadingCallBack loadingCallBack,
                                       VideoModelContract.ErrorCallBack errorCallBack) {
        this.view = view;
        this.model = model;
        this.loadingCallBack = loadingCallBack;
        this.errorCallBack = errorCallBack;
    }

    @Override
    public void getData(boolean init) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (loadingCallBack != null) {
                    loadingCallBack.showLoading();
                }
            }
        });

        model.getData(init);
    }

    @Override
    public void search(String key) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (loadingCallBack != null) {
                    loadingCallBack.showLoading();
                }
            }
        });

        model.search(key);
    }

    @Override
    public void onVideoLoad(final Cursor cursor) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (null != cursor && cursor.getCount() != 0) {
                    LogUtils.i("" + cursor.getCount());
                    view.onFinish(cursor);
                } else {
                    if (errorCallBack != null) {
                        errorCallBack.dealError("empty");
                    }
                }

                if (loadingCallBack != null) {
                    loadingCallBack.hideLoading();
                }
            }
        });
    }

    @Override
    public void onVideoReset() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (loadingCallBack != null) {
                    loadingCallBack.hideLoading();
                }
            }
        });
    }

    @Override
    public void destroy() {
        model.destroy();
        loadingCallBack = null;
        errorCallBack = null;
        model = null;
    }
}
