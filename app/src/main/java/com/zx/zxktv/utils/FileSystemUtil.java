package com.zx.zxktv.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;

public class FileSystemUtil {
    private static Boolean isExternalStorageWriteable() {
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)
                && Environment.getExternalStorageDirectory().canWrite()) {// 已经插入了sd卡，并且可以读写
            mExternalStorageWriteable = true;
        }

        return mExternalStorageWriteable;
    }

    public static String getCurrentAvailableStorageDir() throws IOException {
        if (isExternalStorageWriteable()) {
            return Environment.getExternalStorageDirectory().getCanonicalPath();
        } else {
            return Environment.getDataDirectory().getCanonicalPath();
        }
    }

    public static long getAvailableStore() {

        String filePath = "/data";

        try {
            filePath = getCurrentAvailableStorageDir();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 取得sdcard文件路径
        StatFs statFs = new StatFs(filePath);

        // 获取block的SIZE
        long blocSize = statFs.getBlockSize();

        // 获取BLOCK数量
        // long totalBlocks = statFs.getBlockCount();

        // 可使用的Block的数量
        long availaBlock = statFs.getAvailableBlocks();

        // long total = totalBlocks * blocSize;
        long availableSpare = availaBlock * blocSize;

        return availableSpare;
    }

    /**
     * 删除文件夹所有内容
     */
    public static void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (File f : files) {
                deleteDir(f);
            }
            dir.delete();
        } else
            dir.delete();
    }

    public static String getFileNameWithSuffix(String pathandname) {
        int start = pathandname.lastIndexOf("/");
        if (start != -1) {
            return pathandname.substring(start + 1);
        } else {
            return null;
        }
    }

    public static String getFileName(String pathandname) {
        if (TextUtils.isEmpty(pathandname)) {
            return null;
        }
        int start = pathandname.lastIndexOf(".");
        if (start != -1) {
            return pathandname.substring(0, start);
        } else {
            return null;
        }
    }

    public static boolean isGrantExternalRW(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            activity.requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);

            return false;
        }

        return true;
    }

}
