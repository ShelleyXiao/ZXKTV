package com.zx.zxktv.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.zx.zxktv.R;
import com.zx.zxktv.data.Song;
import com.zx.zxktv.ui.view.MagicTextView;
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

public class OrderSongListAdapter extends RecyclerView.Adapter<OrderSongListAdapter.SangListItemViewHolder> {

    private Context mContext;
    private boolean isFirstPage = false;
    public static List<Song> data = new ArrayList<>();

    private OnPlayListControl mOnPlayListControl;

    
    public OrderSongListAdapter(Context context, List<Song> datas, boolean isFirstPage, OnPlayListControl control) {
        mContext = context;
        this.mOnPlayListControl = control;
        this.isFirstPage = isFirstPage;
        data = datas;
    }

    public void setDatas(List<Song> datas) {
        data.clear();

        data.addAll(datas);
        notifyDataSetChanged();
    }

    @Override
    public SangListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.pop_ordered_list_item, parent, false);
        return new SangListItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SangListItemViewHolder holder, final int position) {
        holder.tv_title.setText(String.format("%d.%s", position + 1, FileSystemUtil.getFileName(data.get(position).name)));

        //set shadow text effect
        if (position == 0 && isFirstPage) {
            holder.tv_title.setTextColor(Color.rgb(255, 204, 0));
            holder.tv_state.setTextColor(Color.rgb(255, 204, 0));
            holder.tv_state.addOuterShadow(20, 3, 3, Color.argb(102, 215, 5, 0));
            holder.tv_state.addOuterShadow(20, 3, 3, Color.argb(102, 215, 5, 0));
            holder.tv_title.addOuterShadow(20, 3, 3, Color.argb(102, 215, 5, 0));
            holder.btn_delete.setVisibility(View.GONE);
            holder.btn_first.setVisibility(View.GONE);
            holder.tv_state.setVisibility(View.VISIBLE);
        }

        holder.btn_first.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (mOnPlayListControl != null) {
                        mOnPlayListControl.gotoFirstPosition(position);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        holder.btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (mOnPlayListControl != null) {
                        mOnPlayListControl.deleteItemByPosition(position);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    public interface OnPlayListControl {
        void gotoFirstPosition(int index);

        void deleteItemByPosition(int index);
    }

    class SangListItemViewHolder extends RecyclerView.ViewHolder {


        MagicTextView tv_title;
        MagicTextView tv_state;
        Button btn_first;
        Button btn_delete;

        public SangListItemViewHolder(View itemView) {
            super(itemView);

            tv_title = (MagicTextView) itemView.findViewById(R.id.tv_title);
            tv_state = (MagicTextView) itemView.findViewById(R.id.tv_state);
            btn_first = (Button) itemView.findViewById(R.id.btn_first);
            btn_delete = (Button) itemView.findViewById(R.id.btn_delete);

        }
    }
}
