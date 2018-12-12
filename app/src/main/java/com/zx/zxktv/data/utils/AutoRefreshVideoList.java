package com.zx.zxktv.data.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;

import com.zx.zxktv.data.Song;
import com.zx.zxktv.lib.libijkplayer.LogUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;

/**
 * User: ShaudXiao
 * Date: 2018-11-12
 * Time: 10:50
 * Company: zx
 * Description:
 * FIXME
 */

public class AutoRefreshVideoList {

    public static final String[] VIDEO_FILE_SUFFIX = {
            "mp4", "3gp", "avi", "flv", "mkv", "rmvb", "mov", "mpg",
            "swf", "vob", "wmv"
    };

    private static String FILEPATH_FILTER_KEY = "/storage/emulated/0/media/";

    private boolean mTimerWorking = false;
    private Context mContext = null;
    private String[] mSupportSuffix = null;
    private BroadcastReceiver mBroadcastReceiver = null;
    private MediaStoreChangeObserver mMediaStoreChangeObserver = null;
    private AutoRefreshListener mAutoRefreshListener = null;
    private Timer mCheckFileTimer = null;

    public AutoRefreshVideoList(Context context, AutoRefreshListener autoRefreshListener,
                                String[] supportSuffix) throws NullPointerException {
        if (null == context || null == autoRefreshListener || null == supportSuffix) {
            throw new NullPointerException("传非空的参数进来！");
        }

        mContext = context;
        mAutoRefreshListener = autoRefreshListener;
        mSupportSuffix = supportSuffix;

        initAutoRefreshVideoList();
    }

    // 不在本界面停止后台检索
    public void onPause() {
        stopCheckFileTimer();
    }

    // 返回界面恢复后台检索
    public void onResume() {
        startCheckFileTimer();
    }

    /**
     * 注销广播
     */
    public void unregisterAutoRefresh() throws NullPointerException {
        if (null == mBroadcastReceiver || null == mMediaStoreChangeObserver || null == mContext) {
            throw new NullPointerException("没有初始化");
        }
        mContext.unregisterReceiver(mBroadcastReceiver);
        mContext.getContentResolver().unregisterContentObserver(mMediaStoreChangeObserver);
        stopCheckFileTimer();
    }

    /**
     * 得到变化的文件列表
     */
    public void getChangedFileList() {
        LogUtils.d("toast ================= getChangedFileList ");
        startCheckFileTimer();
    }

    private void initAutoRefreshVideoList() {
        startMediaFileListener();
        observerMediaStoreChange();
    }

    private void observerMediaStoreChange() {
        if (null == mMediaStoreChangeObserver) {
            mMediaStoreChangeObserver = new MediaStoreChangeObserver();
        }
        mContext.getContentResolver().registerContentObserver(MediaStore.Files.getContentUri("external"),
                false, mMediaStoreChangeObserver);
    }


    private void startMediaFileListener() {
        if (null != mBroadcastReceiver) {
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addDataScheme("file");

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
                    LogUtils.d("toast ================= ACTION_MEDIA_SCANNER_FINISHED ");
                    mTimerWorking = false;
                    startCheckFileTimer();
                } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                    LogUtils.d("toast ================= ACTION_MEDIA_MOUNTED or ACTION_MEDIA_EJECT ");
                    mTimerWorking = true;
                    mAutoRefreshListener.onVideoScan();
                } else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                    mAutoRefreshListener.onVideoScan();
                }
            }
        };
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);//注册监听函数
    }

    /**
     * 媒体数据库变更观察类
     */
    class MediaStoreChangeObserver extends ContentObserver {
        public MediaStoreChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            startCheckFileTimer();
        }
    }

    private void startCheckFileTimer() {
        if (mTimerWorking) {
            return;
        }

        mCheckFileTimer = new Timer();
        mCheckFileTimer.schedule(new CheckFileChangeTimerTask(), 1000);
        mTimerWorking = true;
    }

    private void stopCheckFileTimer() {
        if (null != mCheckFileTimer) {
            mCheckFileTimer.cancel();
            mCheckFileTimer = null;

            mTimerWorking = false;

        }
    }

    /**
     * 得到新增的文件列表
     */
    public ArrayList<Song> getChangedFileList(Context context, String[] searchFileSuffix,
                                                ArrayList<Song> existFileList) {
        LogUtils.i("getChangedFileList " + existFileList.size());
        ArrayList<Song> changedFileList = null;
        if (null == context || null == searchFileSuffix) {
            return changedFileList;
        }
        ArrayList<Song> supportFileList = getSupportFileList(context, searchFileSuffix);
        LogUtils.i("supportFileList = " + supportFileList.size());
        if(existFileList.size() > 0) {
            changedFileList = getDifferentFileList(supportFileList, existFileList);
        } else {
            changedFileList = supportFileList;
        }
        if (null == changedFileList || changedFileList.size() == 0) {
            changedFileList = null;
        }

        return changedFileList;
    }

    /**
     * 获取新增的文件列表
     */
    private ArrayList<Song> getDifferentFileList(ArrayList<Song> newFileList, ArrayList<Song> existFileList) {
        ArrayList<Song> differentFileList = null;
        if (null == newFileList || newFileList.size() == 0) {
            return differentFileList;
        }
        LogUtils.i("getDifferentFileList: " + newFileList.size());
        differentFileList = new ArrayList<Song>();
        boolean isExist = false;
        if (null == existFileList) {
            // 如果已存在文件为空，那肯定是全部加进来啦。
            for (Song newFile : newFileList) {
                differentFileList.add(newFile);
            }
        } else {
            for (Song newFile : newFileList) {
                isExist = false;
                for (Song existFilePath : existFileList) {
                    if (existFilePath.filePath.equals(newFile.filePath)) {
                        isExist = true;
                        break;
                    }
                }

                if (!isExist) {
                    differentFileList.add(newFile);
                }
            }
        }

        return differentFileList;
    }

    /**
     * 从媒体库中获取指定后缀的文件列表
     */
    public ArrayList<Song> getSupportFileList(Context context, String[] searchFileSuffix) {
        ArrayList<Song> searchFileList = null;
        if (null == context || null == searchFileSuffix || searchFileSuffix.length == 0) {
            return null;
        }
//        String searchPath = "";
//        int length = searchFileSuffix.length;
//        for (int index = 0; index < length; index++) {
//            searchPath += (MediaStore.Files.FileColumns.DATA + " LIKE '%" + searchFileSuffix[index] + "' ");
//            if ((index + 1) < length) {
//                searchPath += "or ";
//            }
//        }
//
//        searchFileList = new ArrayList<String>();
//        Uri uri = MediaStore.Files.getContentUri("external");
//        Cursor cursor = context.getContentResolver().query(
//                uri, new String[]{MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns._ID},
//                searchPath, null, null);
//
//        String filepath = null;
//        if (cursor == null) {
//            LogUtils.d("Cursor 获取失败!");
//        } else {
//            if (cursor.moveToFirst()) {
//                do {
//                    filepath = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
//                    try {
//                        searchFileList.add(new String(filepath.getBytes("UTF-8")));
//                    } catch (UnsupportedEncodingException e) {
//                        e.printStackTrace();
//                    }
//
//                } while (cursor.moveToNext());
//            }
//
//            if (!cursor.isClosed()) {
//                cursor.close();
//            }
//        }

        searchFileList = (ArrayList<Song>) searchFiles(new File(FILEPATH_FILTER_KEY),
                Arrays.asList(searchFileSuffix));

        return searchFileList;
    }

    /**
     * 得到媒体库更新的文件
     */
    class GetMediaStoreDataTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            LogUtils.i("get media store data task!!");
            ArrayList<Song> changedFileList = getChangedFileList(mContext, mSupportSuffix, mAutoRefreshListener.onGetVideoDataList());
            if (null != changedFileList && changedFileList.size() > 0) {
                mAutoRefreshListener.onVideoRefresh(changedFileList);
            }
            mTimerWorking = false;

            return null;
        }
    }

    public  List<Song> searchFiles(File folder, final List<String> keyword) {
        List<Song> result = new ArrayList<>();
        if (folder.isFile())
            return result;

        File[] subFolders = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                }
                String fileName = file.getName();
                String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
//                Log.i("debug", "file  ext: " + suffix);
                if (keyword.contains(suffix)) {
                    return true;
                }
                return false;
            }
        });

        if (subFolders != null) {
            for (File file : subFolders) {
                if (file.isFile()) {
                    String path = file.getAbsolutePath();
                    String name = file.getName();
                    Song song = Song.valueOf(path, name);
                    result.add(song);
                } else {
                    result.addAll(searchFiles(file, keyword));
                }
            }
        }

        return result;
    }

    class CheckFileChangeTimerTask extends  java.util.TimerTask {

        @Override
        public void run() {
            new GetMediaStoreDataTask().execute();
        }
    }


    public interface AutoRefreshListener {
        ArrayList<Song> onGetVideoDataList();

        void onVideoRefresh(ArrayList<Song> bookInfoList);

        void onVideoScan();
    }


}
