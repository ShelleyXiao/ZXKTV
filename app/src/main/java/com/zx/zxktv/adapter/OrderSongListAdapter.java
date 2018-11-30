package com.zx.zxktv.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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
        holder.tvSongIndex.setText(position + 1 + ".");
        holder.tvTitle.setText(String.format("%s", FileSystemUtil.getFileName(data.get(position).name)));

        //set shadow text effect
        if (position == 0 && isFirstPage) {
            holder.tvTitle.setTextColor(Color.rgb(255, 204, 0));
            holder.tvState.setTextColor(Color.rgb(255, 204, 0));
            holder.tvState.addOuterShadow(20, 3, 3, Color.argb(102, 215, 5, 0));
            holder.tvState.addOuterShadow(20, 3, 3, Color.argb(102, 215, 5, 0));
            holder.tvTitle.addOuterShadow(20, 3, 3, Color.argb(102, 215, 5, 0));
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnFirst.setVisibility(View.GONE);
            holder.tvState.setVisibility(View.VISIBLE);
        }

        holder.btnFirst.setOnClickListener(new View.OnClickListener() {
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

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
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


        MagicTextView tvTitle;
        MagicTextView tvState;
        Button btnFirst;
        Button btnDelete;
        TextView tvSongIndex;

        public SangListItemViewHolder(View itemView) {
            super(itemView);
            tvSongIndex = (TextView) itemView.findViewById(R.id.tv_song_index);
            tvTitle = (MagicTextView) itemView.findViewById(R.id.tv_title);
            tvState = (MagicTextView) itemView.findViewById(R.id.tv_state);
            btnFirst = (Button) itemView.findViewById(R.id.btn_first);
            btnDelete = (Button) itemView.findViewById(R.id.btn_delete);
            tvTitle.setSelected(true);
        }
    }
}
