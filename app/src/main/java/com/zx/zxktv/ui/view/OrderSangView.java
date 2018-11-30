package com.zx.zxktv.ui.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.zx.zxktv.R;
import com.zx.zxktv.adapter.OrderSangListAdapter;
import com.zx.zxktv.data.Song;
import com.zx.zxktv.presentation.MsgEvent;
import com.zx.zxktv.ui.widget.VideoPlayListmanager;
import com.zx.zxktv.ui.widget.pagelayout.PagerGridLayoutManager;
import com.zx.zxktv.utils.LogUtils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: ShaudXiao
 * Date: 2018-06-22
 * Time: 10:21
 * Company: zx
 * Description:
 * FIXME
 */

public class OrderSangView extends FrameLayout implements PagerGridLayoutManager.PageListener
        , View.OnClickListener, OrderSangListAdapter.OnResumeSongOrderListener {


    private static final int PAGE_NUM = 8;


    private int mRows = PAGE_NUM;
    private int mColumns = 1;
    private RecyclerView mRecyclerView;
    private OrderSangListAdapter mSangListAdapter;
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

    public OrderSangView(@NonNull Context context) {
        super(context);
        init(context);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            LogUtils.i(" onWindowVisibilityChanged pageIndex = " + pageIndex);
            list_data.clear();
            list_data.addAll(VideoPlayListmanager.getIntanse().getPlaySangList());

            loadDataByIndex(pageIndex);
        }
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.popup_sang_song_fragment, this, true);

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
        list_data.addAll(playListmanager.getPlaySangList());
        loadDataByIndex(pageIndex);


    }

    @Override
    public void onPageSizeChanged(int pageSize) {
        mTotalPage = pageSize;
        String pageStr = String.format("%d/%d", mCurrentPage + 1, mTotalPage);
        tv_PageIndex.setText(pageStr);
    }

    @Override
    public void onPageSelect(int pageIndex) {
        mCurrentPage = pageIndex;
        String pageStr = String.format("%d/%d", pageIndex + 1, mTotalPage);
        tv_PageIndex.setText(pageStr);
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

    @Override
    public void onOrder(int index) {
        Song song = VideoPlayListmanager.getIntanse().getSangByIndex(index);
        if(song != null) {
            VideoPlayListmanager.getIntanse().addSong(song);
            VideoPlayListmanager.getIntanse().removeSang(song);

            Map<String, Object> extraMaps = new HashMap<>();
            extraMaps.put(MsgEvent.EXTRA_KEY_UPDATE_LIST, "next_song");
            MsgEvent event = new MsgEvent(null, null, MsgEvent.Type.UPDATELIST);
            event.setExtraMap(extraMaps);

//            EventBus.getDefault().post(event);


            list_data.remove(song);
            loadDataByIndex(pageIndex);
        }
    }

    public void updateDataList() {
        VideoPlayListmanager playListmanager = VideoPlayListmanager.getIntanse();
        list_data.clear();
        list_data.addAll(playListmanager.getPlaySangList());
        loadDataByIndex(pageIndex);
    }

    public void loadDataByIndex(int i) {
        pageNum = list_data.size() % PAGE_NUM == 0 ? list_data.size() / PAGE_NUM : list_data.size() / PAGE_NUM + 1;
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
        mSangListAdapter = new OrderSangListAdapter(getContext(), list_curr_data, this);
        mRecyclerView.setAdapter(mSangListAdapter);
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


}
