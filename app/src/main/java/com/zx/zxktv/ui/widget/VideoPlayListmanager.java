package com.zx.zxktv.ui.widget;


import com.zx.zxktv.data.Song;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * User: ShaudXiao
 * Date: 2018-06-27
 * Time: 13:50
 * Company: zx
 * Description:
 * FIXME
 */

public class VideoPlayListmanager {

    private static VideoPlayListmanager sIntanse;

    private LinkedList<Song> mSongLinkedList;
    private LinkedList<Song> mSangLinkedList;

    private List<INotifyPropertyChanged> mListens;

    private VideoPlayListmanager() {
        mSongLinkedList = new LinkedList<>();
        mSangLinkedList = new LinkedList<>();
        mListens = new ArrayList<>();
    }

    public static VideoPlayListmanager getIntanse() {
        if (null == sIntanse) {
            sIntanse = new VideoPlayListmanager();
        }

        return sIntanse;
    }

    public void addNotifyListen(INotifyPropertyChanged li) {
        mListens.add(li);
    }


    public void destroyListen() {
        for (INotifyPropertyChanged l : mListens) {
            l = null;
        }
    }

    private void notifyListen() {
        for (INotifyPropertyChanged l : mListens) {
            int size = mSongLinkedList.size();
            l.update(size);
        }
    }

    public void clearList() {
        mSongLinkedList.clear();
        mSangLinkedList.clear();

        notifyListen();
    }

    public boolean addSong(Song song) {
        if (mSongLinkedList.indexOf(song) == -1) {
            return mSongLinkedList.add(song);
        }

        notifyListen();

        return false;
    }

    public boolean removeSong(Song song) {
        if (mSongLinkedList.indexOf(song) != -1) {
            mSangLinkedList.add(song);
            return mSongLinkedList.remove(song);
        }

        notifyListen();

        return false;
    }

    public void removeTop() {
        if (!mSongLinkedList.isEmpty()) {
            Song song = mSongLinkedList.pop();
            mSangLinkedList.add(song);
        }

        notifyListen();
    }

    public Song getTop() {
        return mSongLinkedList.peekFirst();
    }

    public Song getSongByIndex(int index) {
        if (mSongLinkedList.size() > 1) {
            return mSongLinkedList.get(1);
        }

        return null;
    }

    public int getSongIndex(Song song) {
        return mSongLinkedList.indexOf(song);
    }

    public void setTop(Song song) {
        int position = mSongLinkedList.indexOf(song);
        if (position <= 0) {
            return;
        }

        mSongLinkedList.remove(song);
        mSongLinkedList.add(1, song);
    }

    public List<Song> getPlaySongList() {
        ArrayList<Song> playList = new ArrayList<>(mSongLinkedList);
        return playList;
    }

    public int getPlaySongSize() {
        return mSongLinkedList.size();
    }

    public Song getSangByIndex(int index) {
        return mSangLinkedList.get(index);
    }

    public List<Song> getPlaySangList() {
        ArrayList<Song> playList = new ArrayList<>(mSangLinkedList);
        return playList;
    }

    public boolean removeSang(Song song) {
        if (mSangLinkedList.indexOf(song) != -1) {
            return mSangLinkedList.remove(song);
        }

        return false;
    }

    public static interface INotifyPropertyChanged {
        void update(int size);
    }
}
