package com.zx.zxktv.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.zx.zxktv.R;
import com.zx.zxktv.data.Song;
import com.zx.zxktv.utils.FileSystemUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ShaudXiao
 * Date: 2018-06-25
 * Time: 15:39
 * Company: zx
 * Description:
 * FIXME
 */

public class OrderSangListAdapter extends RecyclerView.Adapter<OrderSangListAdapter.SangListItemViewHolder> {

    private Context mContext;

    private OnResumeSongOrderListener mSongOrderListener;

    public  List<Song> data = new ArrayList<>();




    public OrderSangListAdapter(Context context, List<Song> datas, OnResumeSongOrderListener listener) {
        mContext = context;
        this.data = datas;
        this.mSongOrderListener = listener;
    }

    @Override
    public SangListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.pop_sang_list_item, parent, false);
        return new SangListItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SangListItemViewHolder holder, final int position) {

        holder.tv_title.setText(String.format("%d.%s", position + 1, FileSystemUtil.getFileName(data.get(position).name)));
        holder.bt_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSongOrderListener != null) {
                    mSongOrderListener.onOrder(position);
                }
            }
        });

    }

    public interface OnResumeSongOrderListener {
        void onOrder(int index);
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    class SangListItemViewHolder extends RecyclerView.ViewHolder {


        TextView tv_title;
        Button bt_again;

        public SangListItemViewHolder(View itemView) {
            super(itemView);

            tv_title = (TextView) itemView.findViewById(R.id.tv_title);
            bt_again = (Button) itemView.findViewById(R.id.btn_again);

        }
    }
}
