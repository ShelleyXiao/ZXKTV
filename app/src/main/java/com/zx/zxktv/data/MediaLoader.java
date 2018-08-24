package com.zx.zxktv.data;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

public class MediaLoader extends CursorLoader {
    private static String FILEPATH_FILTER_KEY = "/storage/emulated/0/media/";

    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");
    private static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATA,
            "duration"};

    // === params for album ALL && showSingleMediaType: false ===
    private static final String SELECTION_ALL =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";

    private static final String[] SELECTION_ALL_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    };
    // ===========================================================

    // === params for album ALL && showSingleMediaType: true ===
    private static final String SELECTION_ALL_FOR_SINGLE_MEDIA_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + " AND " + MediaStore.MediaColumns.DATA + " LIKE ?";

    private static String[] getSelectionArgsForSingleMediaType(int mediaType) {
        return new String[]{String.valueOf(mediaType)
                , String.valueOf("%" + FILEPATH_FILTER_KEY + "%")
        };
    }

    private static final String SELECTION_ALL_FOR_MEDIA_TYPE_AND_NAME =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + " AND " + MediaStore.MediaColumns.DATA + " LIKE '%" + FILEPATH_FILTER_KEY + "%'";

    private static String[] getSelectionArgsForMediaTypeAndName(int mediaType, String name) {
        return new String[]{String.valueOf(mediaType),
                "'%" + String.valueOf(name) + "%'"};
    }
    // =========================================================

    // === params for ordinary album && showSingleMediaType: false ===
    private static final String SELECTION_ALBUM =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND "
                    + " bucket_id=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";

    private static String[] getSelectionAlbumArgs(String albumId) {
        return new String[]{
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
                albumId
        };
    }
    // ===============================================================

    // === params for ordinary album && showSingleMediaType: true ===
    private static final String SELECTION_ALBUM_FOR_SINGLE_MEDIA_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND "
                    + " bucket_id=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";

    private static String[] getSelectionAlbumArgsForSingleMediaType(int mediaType, String albumId) {
        return new String[]{String.valueOf(mediaType), albumId};
    }
    // ===============================================================

    private static final String ORDER_BY = MediaStore.Images.Media.DATE_TAKEN + " DESC";

    private MediaLoader(Context context, String selection, String[] selectionArgs) {
        super(context, QUERY_URI, PROJECTION, selection, selectionArgs, ORDER_BY);
    }

    public static CursorLoader newInstance(Context context, String key) {
        String selection;
        String[] selectionArgs;

        if (TextUtils.isEmpty(key)) {
            selection = SELECTION_ALL_FOR_SINGLE_MEDIA_TYPE;
            selectionArgs = getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);

            return new MediaLoader(context, selection, selectionArgs);
        } else {
            selection = SELECTION_ALL_FOR_MEDIA_TYPE_AND_NAME;
            selectionArgs = getSelectionArgsForMediaTypeAndName(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO, key);
            return new MediaLoader(context, selection, selectionArgs);
        }
    }

    @Override
    public Cursor loadInBackground() {
        Cursor result = super.loadInBackground();

        MatrixCursor dummy = new MatrixCursor(PROJECTION);
//        dummy.addRow(new Object[]{Song.ITEM_ID_CAPTURE, Song.ITEM_DISPLAY_NAME_CAPTURE, "", 0, 0});
        return new MergeCursor(new Cursor[]{dummy, result});
    }

    @Override
    public void onContentChanged() {
        // FIXME a dirty way to fix loading multiple times
    }
}
