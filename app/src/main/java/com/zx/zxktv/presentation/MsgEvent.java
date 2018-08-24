package com.zx.zxktv.presentation;

import com.zx.zxktv.data.Song;

import java.util.Map;

/**
 * User: ShaudXiao
 * Date: 2018-06-28
 * Time: 15:30
 * Company: zx
 * Description:
 * FIXME
 */

public class MsgEvent {

    public static final String EXTRA_KEY_TRACK = "track";
    public static final String EXTRA_KEY_SEEK= "seek";
    public static final String EXTRA_KEY_UPDATE_LIST= "update_list";
    public static final String EXTRA_KEY_QUITE = "quite";
    public static final String EXTRA_KEY_PLAY = "play";

    public Song mSong;

    public String nextSongName;

    public Type eventType;

    public Map<String, Object> extraMap;

    public enum Type {
        SHOW, UPDATELIST, PLAY, STOP, PAUSE, RESTART, NEXT_VIDEO,  SEEK, QUITE, SET_VOL, SET_TRACK, GIFT, SYNC_VIDEO
    }

    public MsgEvent(Song song, String nextSongName, Type eventType) {
        mSong = song;
        this.nextSongName = nextSongName;
        this.eventType = eventType;
    }

    public void setExtraMap(Map<String, Object> extraMap) {
        this.extraMap = extraMap;
    }
}
