package com.zx.zxktv.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsoluteLayout;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.zx.zxktv.R;
import com.zx.zxktv.data.Song;
import com.zx.zxktv.ui.view.AlwaysMarqueeTextView;
import com.zx.zxktv.ui.widget.VideoPlayListmanager;
import com.zx.zxktv.utils.FileSystemUtil;
import com.zx.zxktv.utils.ViewUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: ShaudXiao
 * Date: 2018-11-12
 * Time: 13:37
 * Company: zx
 * Description:
 * FIXME
 */

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.SongItemViewHolder> {

    private List<Song> mDatas = new ArrayList<>();
    private Context mContext;
    private OnSongPreivewListener mPreivewListener;
    private OnUpdatePlayListNotifyListener mListNotifyListener;

    public SongListAdapter(Context context) {
        this.mContext = context;
    }

    public void setDatas(List<Song> songs) {
        mDatas.clear();
        mDatas.addAll(songs);
        notifyDataSetChanged();
    }

    public void setPreivewListener(SongListAdapter.OnSongPreivewListener preivewListener) {
        mPreivewListener = preivewListener;
    }

    public void setListNotifyListener(SongListAdapter.OnUpdatePlayListNotifyListener listNotifyListener) {
        mListNotifyListener = listNotifyListener;
    }

    @Override
    public SongItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_song_layout, parent, false);
        return new SongItemViewHolder(view);
    }

    public void addDatas(List<Song> datas) {
        mDatas.addAll(datas);
        notifyDataSetChanged();
    }

    public void addData(Song data) {
        addData(mDatas.size(), data);
    }

    public void addData(int position, Song data) {
        mDatas.add(position, data);
        notifyItemInserted(position);
    }

    public void removeData(int position) {
        mDatas.remove(position);
        notifyItemRemoved(position);
    }

    public void updateItem(Song song) {
//        long id = song.id;
//        mCursor.moveToPosition(0);
//        int i = 0;
//        int pos = 0;
//        while (i < mCursor.getCount()) {
//            mCursor.moveToPosition(i);
//            if (id == mCursor.getLong(mCursor.getColumnIndex(MediaStore.Files.FileColumns._ID))) {
//                pos = i;
//                break;
//            }
//            i++;
//        }
//        LogUtils.i("index: " + pos + "song: " + song + " ");
//        notifyItemChanged(pos, "1");
        //notifyItemChanged 无效 ，先采用notifyDataSetChanged
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(final SongItemViewHolder holder, int position) {
        final Song item = mDatas.get(position);
        final String title = item.name;
        holder.tvSongName.setText(FileSystemUtil.getFileName(title));

        int index = VideoPlayListmanager.getIntanse().getSongIndex(item);
        if (index != -1) {
            index++;
            holder.tvPosition.setText("(约" + index + ")");
            holder.tvPosition.setVisibility(View.VISIBLE);
        } else {
            holder.tvPosition.setVisibility(View.INVISIBLE);
        }

        holder.cbPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(v.getContext(), "item" + title + " 被点击了", Toast.LENGTH_SHORT).show();

                if (mPreivewListener != null) {
                    mPreivewListener.onPreView(item);
                }
            }
        });


        holder.cbInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(item.filePath);
                if(file.exists()) {
                    VideoPlayListmanager playListmanager = VideoPlayListmanager.getIntanse();
                    if (VideoPlayListmanager.getIntanse().getSongIndex(item) < 0) {
                        playListmanager.addSong(item);
                        int index = VideoPlayListmanager.getIntanse().getSongIndex(item);
                        index++;
                        holder.tvPosition.setText("(约" + index + ")");
                        holder.tvPosition.setVisibility(View.VISIBLE);
                        if (mListNotifyListener != null) {
                            mListNotifyListener.updateList();
                            notifyDataSetChanged();
                        }
                    }
                } else {
                    if (mListNotifyListener != null) {
                        mListNotifyListener.orderSongError(item);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    public Song getItem(int position) {
        if(position <0 || position >= mDatas.size()) {
            return null;
        }

        return mDatas.get(position);
    }

    public interface OnSongPreivewListener {
        void onPreView(Song song);
    }

    public interface OnUpdatePlayListNotifyListener {
        void updateList();
        void orderSongError(Song song);
    }

    class SongItemViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatarBg;
        ImageView ivAvatar;

        AlwaysMarqueeTextView tvSongName;
        TextView tvPosition;
        //        TextView tvPosition;
        CheckBox cbPreview;
        CheckBox cbInfo;

        public SongItemViewHolder(View itemView) {
            super(itemView);

            //没有UI 其他地方找的资源
            DisplayMetrics displayMetrics = ViewUtils.getScreenResolution((Activity) mContext);

            View mainView = new AbsoluteLayout(mContext);

            AbsListView.LayoutParams as_params = new AbsListView.LayoutParams(
                    ((int) (displayMetrics.widthPixels * (689.0f / 1920f))), (int) (displayMetrics.heightPixels * (334.0 / 1200.0f))
            );

            mainView.setLayoutParams(as_params);


            ivAvatar = new ImageView(mContext);
            ivAvatarBg = new ImageView(mContext);
            cbInfo = new CheckBox(mContext);
            tvPosition = new TextView(mContext);
            tvPosition = new TextView(mContext);
            tvSongName = new AlwaysMarqueeTextView(mContext);
            cbPreview = new CheckBox(mContext);
            cbPreview.setButtonDrawable(R.drawable.blank_button);

            AbsoluteLayout.LayoutParams al_params = new AbsoluteLayout.LayoutParams(
                    (int) (displayMetrics.widthPixels * (304.0f / 1920.0f)),
                    ((int) (displayMetrics.heightPixels * (304.0f / 1200.0f))),
                    0, (int) (displayMetrics.heightPixels * (15.0f / 1920.0f))
            );

            ivAvatarBg.setLayoutParams(al_params);
            ivAvatarBg.setImageResource(R.drawable.song_list_pic_bg);

            al_params = new AbsoluteLayout.LayoutParams(
                    (int) (displayMetrics.widthPixels * (195.0f / 1920.0f)),
                    ((int) (displayMetrics.heightPixels * (195.0f / 1200.0f))),
                    (int) (displayMetrics.widthPixels * (53.0f / 1920.0f)), (int) (displayMetrics.heightPixels * (47.0 / 1200.0f))
            );
            ivAvatar.setLayoutParams(al_params);
            ivAvatar.setImageResource(R.drawable.ldh_icon);

            al_params = new AbsoluteLayout.LayoutParams(
                    (int) (displayMetrics.widthPixels * (454.0f / 1920.0f)),
                    ((int) (displayMetrics.heightPixels * (334.0f / 1200.0f))),
                    (int) (displayMetrics.widthPixels * (200.0f / 1920.0f)), -(int) (displayMetrics.heightPixels * (7.0 / 1200.0f))
            );


            AbsoluteLayout ab_infomation = new AbsoluteLayout(mContext);
            ab_infomation.setLayoutParams(al_params);
            al_params = new AbsoluteLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0, 0);

            cbInfo.setLayoutParams(al_params);
            cbInfo.setBackground(mContext.getResources().getDrawable(R.drawable.btn_song_list_information_bg));
            cbInfo.setButtonDrawable(R.drawable.blank_button);

            al_params = new AbsoluteLayout.LayoutParams((int) (displayMetrics.widthPixels * (300 / 1920.0f)),
                    (int) (displayMetrics.widthPixels * (120.0f / 1920.0f)),
                    (int) (displayMetrics.widthPixels * (82.0f / 1920.0f)),
                    (int) (displayMetrics.heightPixels * (62.0f / 1200)));

            tvSongName.setLayoutParams(al_params);
            tvSongName.setTextSize(24);
            tvSongName.setTextColor(Color.WHITE);
            tvSongName.setSingleLine();

            al_params = new AbsoluteLayout.LayoutParams((int) (displayMetrics.widthPixels * (120.0f / 1920.0f)),
                    (int) (displayMetrics.widthPixels * (120.0f / 1920.0f)),
                    (int) (displayMetrics.widthPixels * (82.0f / 1920.0f)),
                    (int) (displayMetrics.heightPixels * (160.0f / 1200)));
            tvPosition.setLayoutParams(al_params);
            tvPosition.setTextSize(20);
            tvPosition.setAlpha(0.5f);
            tvPosition.setSingleLine();
            tvPosition.setTextColor(Color.RED);

            al_params = new AbsoluteLayout.LayoutParams((int) (displayMetrics.widthPixels * 0.044)
                    , (int) (displayMetrics.heightPixels * 0.1),
                    (int) (displayMetrics.widthPixels * (300.0f / 1920.0f))
                    , (int) (displayMetrics.heightPixels * (136.0f / 1200.0f))
            );
            cbPreview.setBackgroundResource(R.drawable.btn_song_list_preview);
            cbPreview.setLayoutParams(al_params);


            ab_infomation.addView(cbInfo);
            ab_infomation.addView(tvSongName);
//            ab_infomation.addView(tvPosition);
            ab_infomation.addView(tvPosition);
            ab_infomation.addView(cbPreview);

            tvPosition.setVisibility(View.VISIBLE);

            ((AbsoluteLayout) mainView).addView(ivAvatarBg);
            ((AbsoluteLayout) mainView).addView(ivAvatar);
            ((AbsoluteLayout) mainView).addView(ab_infomation);

            ((FrameLayout) itemView).addView(mainView);

        }
    }
}
