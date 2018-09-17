package com.zx.zxktv.data;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;

import com.zx.zxktv.data.interfaces.VideoModelContract;
import com.zx.zxktv.utils.LogUtils;

import java.lang.ref.WeakReference;

/**
 * User: ShaudXiao
 * Date: 2018-06-26
 * Time: 14:15
 * Company: zx
 * Description:
 * FIXME
 */

public class VideoModelImpl implements VideoModelContract.Model,
        LoaderManager.LoaderCallbacks<Cursor> {

    private final static int LOADER_ID = 1000;

    private LoaderManager mLoaderManager;
    private VideoModelContract.ModelDataCallBack mCallbacks;
    private WeakReference<Context> mContext;
    private String keySearch;

    public VideoModelImpl(Context context, final VideoModelContract.ModelDataCallBack callBack) {
        this.mContext = new WeakReference<>(context);
        this.mCallbacks = callBack;
        mLoaderManager = ((Activity) context).getLoaderManager();
    }

    @Override
    public void getData(boolean init) {
        if (init) {
            mLoaderManager.initLoader(LOADER_ID, null, this);
        } else {
            mLoaderManager.restartLoader(LOADER_ID, null, this);
        }
    }

    @Override
    public void search(String key) {
        keySearch = key;
        mLoaderManager.restartLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Context context = mContext.get();
        if (context == null) {
            return null;
        }

        if (!TextUtils.isEmpty(keySearch)) {
            LogUtils.i("keySearch " + keySearch);
            return MediaLoader.newInstance(context, keySearch);
        } else {

            return MediaLoader.newInstance(context, "");
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Context context = mContext.get();
        if (context == null) {
            return;
        }
        keySearch = null;
        mCallbacks.onVideoLoad(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Context context = mContext.get();
        if (context == null) {
            return;
        }

        mCallbacks.onVideoReset();
    }

    @Override
    public void destroy() {
        mLoaderManager.destroyLoader(LOADER_ID);
        mCallbacks = null;
    }
}
