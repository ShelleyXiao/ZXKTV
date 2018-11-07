package com.zx.zxktv.ui.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.zx.zxktv.R;
import com.zx.zxktv.adapter.OrderSongListAdapter;
import com.zx.zxktv.adapter.animator.SlideInLeftAnimationAdapter;
import com.zx.zxktv.adapter.animator.holder.FadeInAnimator;
import com.zx.zxktv.data.Song;
import com.zx.zxktv.presentation.MsgEvent;
import com.zx.zxktv.ui.widget.VideoPlayListmanager;
import com.zx.zxktv.ui.widget.pagelayout.PagerGridLayoutManager;
import com.zx.zxktv.utils.LogUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * User: ShaudXiao
 * Date: 2018-06-22
 * Time: 10:21
 * Company: zx
 * Description:
 * FIXME
 */

public class OrderSongsView extends FrameLayout implements PagerGridLayoutManager.PageListener,
        View.OnClickListener, OrderSongListAdapter.OnPlayListControl {

    private static final int PAGE_NUM = 8;


    private int mRows = PAGE_NUM;
    private int mColumns = 1;
    private RecyclerView mRecyclerView;
    private OrderSongListAdapter mSongListAdapter;
    private PagerGridLayoutManager mLayoutManager;

    private TextView tv_PageIndex;
    private Button bt_PagePre;
    private Button bt_PageNext;

    private int mTotalPage = 0;
    private int mCurrentPage = 0;

    private List<Song> list_data = new ArrayList<>();
    private List<Song> list_curr_data = new ArrayList<>();
    private int pageIndex = 0;
    private int pageNum = 0;

    private IOnDeleteSongListener mOnDeleteSongListener;


    public OrderSongsView(@NonNull Context context) {
        super(context);
        init(context);

    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.popup_ordered_songs_frgment, this, true);


        tv_PageIndex = (TextView) findViewById(R.id.tv_pageNum);
        bt_PagePre = (Button) findViewById(R.id.btn_prePage);
        bt_PageNext = (Button) findViewById(R.id.btn_nextPage);

        bt_PagePre.setOnClickListener(this);
        bt_PageNext.setOnClickListener(this);

        mLayoutManager = new PagerGridLayoutManager(mRows, mColumns, PagerGridLayoutManager
                .VERTICAL);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(mLayoutManager);

        VideoPlayListmanager playListmanager = VideoPlayListmanager.getIntanse();
        list_data.addAll(playListmanager.getPlaySongList());

        loadDataByIndex(pageIndex);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mOnDeleteSongListener = null;
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            LogUtils.i(" onWindowVisibilityChanged pageIndex = " + pageIndex);
            list_data.clear();
            list_data.addAll(VideoPlayListmanager.getIntanse().getPlaySongList());

            loadDataByIndex(pageIndex);
        }
    }

    @Override
    public void onPageSizeChanged(int pageSize) {
        mTotalPage = pageSize;
    }

    @Override
    public void onPageSelect(int pageIndex) {
        mCurrentPage = pageIndex;
        String pageStr = String.format("%d/%d", pageIndex + 1, mTotalPage);
        tv_PageIndex.setText(pageStr);
    }

    public void updateDataList() {
        LogUtils.i(" onWindowVisibilityChanged pageIndex = " + pageIndex);
        list_data.clear();
        list_data.addAll(VideoPlayListmanager.getIntanse().getPlaySongList());

        loadDataByIndex(pageIndex);

    }

    public void loadDataByIndex(int i) {
        pageNum = list_data.size() % PAGE_NUM == 0 ?
                list_data.size() / PAGE_NUM : list_data.size() / PAGE_NUM + 1;
        if (i >= pageNum) {
            bt_PageNext.setClickable(false);
            return;
        } else {
            bt_PageNext.setClickable(true);
        }

        if (i < 0) {
            bt_PagePre.setClickable(false);
            return;
        } else {
            bt_PagePre.setClickable(true);
        }
        try {
            list_curr_data = list_data.subList(PAGE_NUM * i, PAGE_NUM * i + PAGE_NUM);
        } catch (Exception e) {
            list_curr_data = list_data.subList(PAGE_NUM * i, list_data.size());
        }
        tv_PageIndex.setText(String.format("%d/%d", i + 1, pageNum));
        pageIndex = i;
        mSongListAdapter = new OrderSongListAdapter(getContext(), list_curr_data, i == 0 ? true : false, this);

        mRecyclerView.setItemAnimator(new FadeInAnimator());
        SlideInLeftAnimationAdapter alphaAdapter = new SlideInLeftAnimationAdapter(mSongListAdapter);
        alphaAdapter.setFirstOnly(true);
        alphaAdapter.setDuration(500);
        alphaAdapter.setInterpolator(new OvershootInterpolator(.5f));
        mRecyclerView.setAdapter(alphaAdapter);
    }

    public void onNextPageClick() {
        if (pageIndex + 1 > pageNum) {
            return;
        } else {
            loadDataByIndex(pageIndex + 1);
        }
    }

    public void onPrePageClick() {
        if (pageIndex - 1 < 0) {
            return;
        } else {
            loadDataByIndex(pageIndex - 1);
        }
    }

    public void onMessCardClick() {
        if (list_data != null && list_data.size() > 0) {
            Collections.shuffle(list_data);
            loadDataByIndex(0);
        }
    }

    @Override
    public void gotoFirstPosition(int i) {
        Song song = list_data.get(i);
        list_data.remove(i);
        list_data.add(1, song);
        loadDataByIndex(pageIndex);
        VideoPlayListmanager playListmanager = VideoPlayListmanager.getIntanse();
        playListmanager.setTop(song);


    }

    public void setOnDeleteSongListener(IOnDeleteSongListener onDeleteSongListener) {
        mOnDeleteSongListener = onDeleteSongListener;
    }

    @Override
    public void deleteItemByPosition(int i) {
        Song song = list_data.remove(i);
        loadDataByIndex(pageIndex);
        VideoPlayListmanager playListmanager = VideoPlayListmanager.getIntanse();
        playListmanager.removeSong(song);
        if (mOnDeleteSongListener != null) {
            mOnDeleteSongListener.deleteSong(song);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_prePage:
                onPrePageClick();
                break;
            case R.id.btn_nextPage:
                onNextPageClick();
                break;
            default:
                break;
        }
    }

    public void onEvent(MsgEvent event) {
//        if (event.eventType == MsgEvent.Type.SYNC_VIDEO) {
//            list_data.clear();
//            list_data.addAll(VideoPlayListmanager.getIntanse().getPlaySongList());
//            loadDataByIndex(pageIndex);
//        }
        if (event.eventType == MsgEvent.Type.UPDATELIST) {
            if (event.extraMap != null && !event.extraMap.isEmpty()) {
                String extraValue = (String) event.extraMap.get(MsgEvent.EXTRA_KEY_SEEK);
                if (extraValue.equals("update_top")) {
                    list_data.clear();
                    list_data.addAll(VideoPlayListmanager.getIntanse().getPlaySongList());
                    loadDataByIndex(pageIndex);
                }
            }
        }

    }

    public interface IOnDeleteSongListener {
        void deleteSong(Song song);
    }

}
