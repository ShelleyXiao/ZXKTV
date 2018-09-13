package com.zx.zxktv.adapter;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.MediaStore;
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
import com.zx.zxktv.utils.LogUtils;
import com.zx.zxktv.utils.ViewUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ShaudXiao
 * Date: 2018-06-21
 * Time: 13:52
 * Company: zx
 * Description:
 * FIXME
 */

public class SongListAdapter extends RecyclerViewCursorAdapter<SongListAdapter.SongItemViewHolder> {

    public static List<String> data = new ArrayList<>();
    private Context mContext;
    private OnSongPreivewListener mPreivewListener;
    private OnUpdatePlayListNotifyListener mListNotifyListener;


    public SongListAdapter(Context context) {
        super(null);
        this.mContext = context;
    }

    public void setPreivewListener(OnSongPreivewListener preivewListener) {
        mPreivewListener = preivewListener;
    }

    public void setListNotifyListener(OnUpdatePlayListNotifyListener listNotifyListener) {
        mListNotifyListener = listNotifyListener;
    }

    @Override
    protected void onBindViewHolder(final SongItemViewHolder holder, Cursor cursor) {
        final Song item = Song.valueOf(cursor);
        final String title = item.name;
        holder.tv_SongName.setText(FileSystemUtil.getFileName(title));

        int index = VideoPlayListmanager.getIntanse().getSongIndex(item);
        if (index != -1) {
            index++;
            holder.tv_position.setText("(约" + index + ")");
        }

        holder.cb_preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(v.getContext(), "item" + title + " 被点击了", Toast.LENGTH_SHORT).show();

                if (mPreivewListener != null) {
                    mPreivewListener.onPreView(item);
                }
            }
        });


        holder.cb_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoPlayListmanager playListmanager = VideoPlayListmanager.getIntanse();
                if (VideoPlayListmanager.getIntanse().getSongIndex(item) < 0) {
                    playListmanager.addSong(item);
                    int index = VideoPlayListmanager.getIntanse().getSongIndex(item);
                    index++;
                    holder.tv_position.setText("(约" + index + ")");
                    if (mListNotifyListener != null) {
                        mListNotifyListener.updateList();
                    }
                }
            }
        });
    }

    @Override
    protected int getItemViewType(int position, Cursor cursor) {
        return 1;
    }


    @Override
    public SongItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //    Log.i("GCS", "onCreateViewHolder");
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_song_layout, parent, false);
        return new SongItemViewHolder(view);
    }



    public Song getItem(int position) {
        if (!isDataValid(mCursor)) {
            throw new IllegalStateException("Cannot bind view holder when cursor is in invalid state.");
        }

        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("Could not move cursor to position " + position
                    + " when trying to bind view holder");
        }
        return Song.valueOf(mCursor);
    }

    public void updateItem(Song song) {
        long id = song.id;
        mCursor.moveToPosition(0);
        int i = 0;
        int pos = 0;
        while (i < mCursor.getCount()) {
            mCursor.moveToPosition(i);
            if (id == mCursor.getLong(mCursor.getColumnIndex(MediaStore.Files.FileColumns._ID))) {
                pos = i;
                break;
            }
            i++;
        }
        LogUtils.i("index: " + pos + "song: " + song + " ");
//        notifyItemChanged(pos, "1");
        notifyDataSetChanged();
    }

    public interface OnSongPreivewListener {
        void onPreView(Song song);
    }

    public interface OnUpdatePlayListNotifyListener {
        void updateList();
    }

    class SongItemViewHolder extends RecyclerView.ViewHolder {
        ImageView iv_avatar_bg;
        ImageView iv_avatar;

        AlwaysMarqueeTextView tv_SongName;
        TextView tv_position;
        //        TextView tv_position;
        CheckBox cb_preview;
        CheckBox cb_info;

        public SongItemViewHolder(View itemView) {
            super(itemView);

//            iv_avatar = (ImageView) itemView.findViewById(R.id.avatar);
//
//            tv_SongName = (TextView) itemView.findViewById(R.id.song_name);
//            tv_position = (TextView) itemView.findViewById(R.id.singer_name);
//            tv_position = (TextView) itemView.findViewById(R.id.position);
//            cb_preview = (CheckBox) itemView.findViewById(R.id.preview);
//            cb_info = (CheckBox) itemView.findViewById(R.id.song_info);
//
//            DisplayMetrics displayMetrics = ViewUtils.getScreenResolution((Activity) mContext);
//
//
//            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) iv_avatar.getLayoutParams();
//
////            params.setMargins(0,
////                    0,
////                    -(int) (displayMetrics.heightPixels * (300.0 / 1200f)), 0);
////            iv_avatar.setLayoutParams(params);
//
//            params = (FrameLayout.LayoutParams) cb_info.getLayoutParams();
//            params.setMargins(-(int) (displayMetrics.widthPixels * (140.0 / 1920f)),
//                   0,
//                    0, 0);
//
//
//            cb_info.setLayoutParams(params);
//
//            tv_position.setAlpha(0.5f);

            //没有UI 其他地方找的资源
            DisplayMetrics displayMetrics = ViewUtils.getScreenResolution((Activity) mContext);

            View mainView = new AbsoluteLayout(mContext);

            AbsListView.LayoutParams as_params = new AbsListView.LayoutParams(
                    ((int) (displayMetrics.widthPixels * (689.0f / 1920f))), (int) (displayMetrics.heightPixels * (334.0 / 1200.0f))
            );

            mainView.setLayoutParams(as_params);


            iv_avatar = new ImageView(mContext);
            iv_avatar_bg = new ImageView(mContext);
            cb_info = new CheckBox(mContext);
            tv_position = new TextView(mContext);
            tv_position = new TextView(mContext);
            tv_SongName = new AlwaysMarqueeTextView(mContext);
            cb_preview = new CheckBox(mContext);
            cb_preview.setButtonDrawable(R.drawable.blank_button);

            AbsoluteLayout.LayoutParams al_params = new AbsoluteLayout.LayoutParams(
                    (int) (displayMetrics.widthPixels * (304.0f / 1920.0f)),
                    ((int) (displayMetrics.heightPixels * (304.0f / 1200.0f))),
                    0, (int) (displayMetrics.heightPixels * (15.0f / 1920.0f))
            );

            iv_avatar_bg.setLayoutParams(al_params);
            iv_avatar_bg.setImageResource(R.drawable.song_list_pic_bg);

            al_params = new AbsoluteLayout.LayoutParams(
                    (int) (displayMetrics.widthPixels * (195.0f / 1920.0f)),
                    ((int) (displayMetrics.heightPixels * (195.0f / 1200.0f))),
                    (int) (displayMetrics.widthPixels * (53.0f / 1920.0f)), (int) (displayMetrics.heightPixels * (47.0 / 1200.0f))
            );
            iv_avatar.setLayoutParams(al_params);
            iv_avatar.setImageResource(R.drawable.ldh_icon);

            al_params = new AbsoluteLayout.LayoutParams(
                    (int) (displayMetrics.widthPixels * (454.0f / 1920.0f)),
                    ((int) (displayMetrics.heightPixels * (334.0f / 1200.0f))),
                    (int) (displayMetrics.widthPixels * (200.0f / 1920.0f)), -(int) (displayMetrics.heightPixels * (7.0 / 1200.0f))
            );


            AbsoluteLayout ab_infomation = new AbsoluteLayout(mContext);
            ab_infomation.setLayoutParams(al_params);
            al_params = new AbsoluteLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0, 0);

            cb_info.setLayoutParams(al_params);
            cb_info.setBackground(mContext.getResources().getDrawable(R.drawable.btn_song_list_information_bg));
            cb_info.setButtonDrawable(R.drawable.blank_button);

            al_params = new AbsoluteLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (displayMetrics.widthPixels * (120.0f / 1920.0f)),
                    (int) (displayMetrics.widthPixels * (82.0f / 1920.0f)),
                    (int) (displayMetrics.heightPixels * (62.0f / 1200)));
            tv_SongName.setLayoutParams(al_params);
            tv_SongName.setTextSize(24);
            tv_SongName.setTextColor(Color.WHITE);
            tv_SongName.setText("バクチ・ダンサー");
            tv_SongName.setSingleLine();

            al_params = new AbsoluteLayout.LayoutParams((int) (displayMetrics.widthPixels * (120.0f / 1920.0f)),
                    (int) (displayMetrics.widthPixels * (120.0f / 1920.0f)),
                    (int) (displayMetrics.widthPixels * (82.0f / 1920.0f)),
                    (int) (displayMetrics.heightPixels * (160.0f / 1200)));
            tv_position.setLayoutParams(al_params);
            tv_position.setTextSize(20);
            tv_position.setAlpha(0.5f);
            tv_position.setSingleLine();
            tv_position.setTextColor(Color.RED);

//            al_params = new AbsoluteLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.WRAP_CONTENT,
//                    0,
//                    (int) (displayMetrics.heightPixels * (62.0f / 1200)));
//            tv_position.setLayoutParams(al_params);
//            tv_position.setGravity(Gravity.RIGHT);
//            tv_position.setPadding(0, 0, (int) (displayMetrics.widthPixels * (82.0f / 1920.0f)), 0);
//            tv_position.setTextSize(24);
//            tv_position.setTextColor(Color.WHITE);
//            tv_position.setText("(约6)");
//            tv_position.setSingleLine();


            al_params = new AbsoluteLayout.LayoutParams((int) (displayMetrics.widthPixels * 0.044)
                    , (int) (displayMetrics.heightPixels * 0.1),
                    (int) (displayMetrics.widthPixels * (300.0f / 1920.0f))
                    , (int) (displayMetrics.heightPixels * (136.0f / 1200.0f))
            );
            cb_preview.setBackgroundResource(R.drawable.btn_song_list_preview);
            cb_preview.setLayoutParams(al_params);


            ab_infomation.addView(cb_info);
            ab_infomation.addView(tv_SongName);
//            ab_infomation.addView(tv_position);
            ab_infomation.addView(tv_position);
            ab_infomation.addView(cb_preview);

            ((AbsoluteLayout) mainView).addView(iv_avatar_bg);
            ((AbsoluteLayout) mainView).addView(iv_avatar);
            ((AbsoluteLayout) mainView).addView(ab_infomation);

            ((FrameLayout) itemView).addView(mainView);

        }
    }
}
