package com.zx.zxktv.data.interfaces;

import android.database.Cursor;

/**
 * User: ShaudXiao
 * Date: 2018-06-26
 * Time: 13:59
 * Company: zx
 * Description:
 * FIXME
 */

public interface VideoModelContract {


    interface Model {
        void getData(boolean init);
        void search(String key);
    }

    interface View {
        void onFinish(Cursor data);
    }

    interface Presenter {
        void getData(boolean init);
        void search(String key);
    }

    /**
     * model数据回调
     */
    interface ModelDataCallBack {

        void onVideoLoad(Cursor cursor);

        void onVideoReset();
    }


    /**
     * 加载中回调
     */
    interface LoadingCallBack {
        void showLoading();

        void hideLoading();
    }

    /**
     * 错误回调
     */
    interface ErrorCallBack {
        void dealError(String message);
    }


}
