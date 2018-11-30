package com.zx.zxktv.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.zx.zxktv.R;
import com.zx.zxktv.adapter.SongListAdapter;
import com.zx.zxktv.data.LoadVideoModelPresenterImpl;
import com.zx.zxktv.data.Song;
import com.zx.zxktv.data.interfaces.VideoModelContract;
import com.zx.zxktv.data.utils.AutoRefreshVideoList;
import com.zx.zxktv.presentation.PresentationService;
import com.zx.zxktv.ui.fragment.SongPreviewWin;
import com.zx.zxktv.ui.view.LongTouchButton;
import com.zx.zxktv.ui.view.MagicTextView;
import com.zx.zxktv.ui.view.OrderSangView;
import com.zx.zxktv.ui.view.OrderSongsView;
import com.zx.zxktv.ui.view.RepeatingButton;
import com.zx.zxktv.ui.widget.BoxedVertical;
import com.zx.zxktv.ui.widget.VerticalProgressBar;
import com.zx.zxktv.ui.widget.VideoPlayListmanager;
import com.zx.zxktv.ui.widget.pagelayout.PagerGridLayoutManager;
import com.zx.zxktv.ui.widget.pagelayout.PagerGridSnapHelper;
import com.zx.zxktv.utils.AudioMngHelper;
import com.zx.zxktv.utils.LogUtils;
import com.zx.zxktv.utils.rxbus.RxBus;
import com.zx.zxktv.utils.rxbus.RxConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class MainActivity extends BaseActivity implements View.OnClickListener
        , VideoModelContract.View, SongListAdapter.OnUpdatePlayListNotifyListener,
        VideoModelContract.ErrorCallBack, AutoRefreshVideoList.AutoRefreshListener {

    public static final String MEDIA_PATH = Environment.getExternalStorageDirectory() + "/media/";

    private static final int CONSTANT_LONG_REPEAT_TIME = 100;
    private final static int CONSTANT_OFFSET_DISTANCE = 20;
    private final static int MUSICE_VOL_STEP = 5;
    private final static int PROGRESS_DETAL = 12000;

    private RecyclerView rvSongList;
    private TextView tvPageIndex;
    private ImageView ivPagePre;
    private ImageView ivPageNext;

    private View btnOrdered;
    private Button btnVolume;
    private Button btnEffect;
    private CheckBox cbOrignal;
    private Button btnRestart;
    private CheckBox cbSilent;
    private MagicTextView tvTips;
    private MagicTextView tvNum;

    private Button btnSearchClose;

    private LongTouchButton btnBack;
    private LongTouchButton btnForward;

    private Button btnNextVideo;
    private CheckBox cbPlay;


    private View v_ll_poporder_bg;
    private FrameLayout flSongsBg;

    private PopupWindow popupOrdered;
    private PopupWindow popupVolume;
    private PopupWindow popupEffect;
    private RadioButton rbOrdered;

    private VerticalProgressBar vp_popEffect_pitch_1;
    private VerticalProgressBar vp_popEffect_pitch_2;

    private RadioGroup rg_ordered;
    private TabHost tabHost_popup;

    private OrderSangView mOrderSangView;
    private OrderSongsView mOrderSongsView;

    private EditText mSearchEdit;

    private RelativeLayout rl_videoControl;
    private FrameLayout fl_videoViewSmall;
    private FrameLayout fl_videoViewPrimary;

    private View rl_listView;
    private TextView tv_ErrorInfo;

    private int mRows = 3;
    private int mColumns = 3;
    private SongListAdapter mSongAdapter;
    private PagerGridLayoutManager mLayoutManager;

    private int mPageSum;
    private int mCurrentPageIndex;

    private AutoRefreshVideoList mRefreshVideoList;


    private LoadVideoModelPresenterImpl mVideoModelPresenter;

    private List<Song> mSongDatas = new ArrayList<>();

    private AudioMngHelper mAudioMngHelper;

    private boolean mSearchMode = false;

    private FrameLayout mFrameLayout;
    private PresentationService mPresentationService;

    private Cursor mSongCursor;

    private Disposable mDisposable;

    private boolean isFirstSelected = false;

    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        initView();

        mVideoModelPresenter = new LoadVideoModelPresenterImpl(this, this, this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 100);
        } else {
            mVideoModelPresenter.getData(true);
        }

        Intent it = new Intent(MainActivity.this, PresentationService.class);
        MainActivity.this.startService(it);
        isConnected = bindService(it, mServiceConnection, Context.BIND_AUTO_CREATE
                | Context.BIND_IMPORTANT | Context.BIND_ADJUST_WITH_ACTIVITY);

        subscribeEvent();

        mRefreshVideoList = new AutoRefreshVideoList(this, this,
                AutoRefreshVideoList.VIDEO_FILE_SUFFIX);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPresentationService = ((PresentationService.LocalBinder) service).getService();
            mPresentationService.attachFrameLayout(fl_videoViewSmall);
            mPresentationService.attachFrameLayout(fl_videoViewPrimary);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPresentationService = null;
        }

    };

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onResume() {
        super.onResume();
        mRefreshVideoList.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    @Override
    public void onPause() {
        super.onPause();

        destoryPopupWindow();
        mRefreshVideoList.onPause();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
        mVideoModelPresenter.destroy();

        mRefreshVideoList.unregisterAutoRefresh();

        if (mPresentationService != null && isConnected) {
            unbindService(mServiceConnection);
        }
    }

    @Override
    public void onBackPressed() {
        if (mSearchMode) {
            mSearchMode = false;
            mVideoModelPresenter.getData(false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mVideoModelPresenter.getData(true);
                } else {
                    Toast.makeText(this, R.string.grant_label, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onFinish(Cursor cursor) {
        LogUtils.i("cursor " + cursor);
        if (cursor != null /*&& !mSearchMode*/) {
            mSongCursor = cursor;
            if (cursor.moveToFirst()) {
                do {
                    Song song = Song.valueOf(cursor);
                    File file = new File(song.filePath);
                    if (file.exists()) {
                        mSongDatas.add(song);
                    }
                } while (cursor.moveToNext());
            }

            mSongAdapter.setDatas(mSongDatas);

            if (!mSearchMode) {
                VideoPlayListmanager.getIntanse().clearList();
            }
        }
        tv_ErrorInfo.setVisibility(View.INVISIBLE);
        rl_listView.setVisibility(View.VISIBLE);
    }

    @Override
    public void dealError(String message) {
        LogUtils.i("dealError");

        tv_ErrorInfo.setVisibility(View.VISIBLE);
        rl_listView.setVisibility(View.INVISIBLE);
        tvPageIndex.setText("0/0");
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (popupOrdered != null && popupOrdered.isShowing()) {
            return false;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void updateList() {
        int size = VideoPlayListmanager.getIntanse().getPlaySongSize();
        tvNum.setText(String.valueOf(size));
        if (VideoPlayListmanager.getIntanse().getPlaySongSize() == 1) {

            final Song song = VideoPlayListmanager.getIntanse().getTop();
            LogUtils.i("song: " + song);
            if (song != null && !isFirstSelected) {
                mPresentationService.playVideo(song);
                isFirstSelected = true;
            }
        } else {
            mPresentationService.updateDisplayInfo();
        }

        cbPlay.setEnabled(true);
    }

    @Override
    public void orderSongError(Song song) {
        Toast.makeText(this, song.name + " ," + getString(R.string.order_faild),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public ArrayList<Song> onGetVideoDataList() {
        return (ArrayList<Song>) mSongDatas;
    }

    @Override
    public void onVideoRefresh(final ArrayList<Song> videoList) {
        LogUtils.i("****onVideoRefresh*********** " + videoList.size());

        mSongDatas.addAll(videoList);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSongAdapter.addDatas(videoList);
            }
        });

    }

    @Override
    public void onVideoScan() {

    }

    private void initView() {
        rl_videoControl = (RelativeLayout) findViewById(R.id.video_control);
        rl_listView = findViewById(R.id.list);
        tv_ErrorInfo = (TextView) findViewById(R.id.empty_data);

        fl_videoViewSmall = (FrameLayout) findViewById(R.id.videoView_container);
        fl_videoViewPrimary = (FrameLayout) findViewById(R.id.primary_video_view);

        rvSongList = (RecyclerView) findViewById(R.id.song_list);
        tvPageIndex = (TextView) findViewById(R.id.page_index);
        ivPagePre = (ImageView) findViewById(R.id.page_pre);
        ivPageNext = (ImageView) findViewById(R.id.page_next);

        btnVolume = (Button) findViewById(R.id.btn_volume);
        btnRestart = (Button) findViewById(R.id.btn_resing);
        btnEffect = (Button) findViewById(R.id.btn_effect);

        cbOrignal = (CheckBox) findViewById(R.id.cb_origin);
        cbSilent = (CheckBox) findViewById(R.id.cb_quite);
//        cbOrignal.setChecked(true);
//        cbSilent.setChecked(true);

        cbPlay = (CheckBox) findViewById(R.id.cb_play);
        cbPlay.setEnabled(false);

        ivPagePre.setOnClickListener(this);
        ivPageNext.setOnClickListener(this);
        btnVolume.setOnClickListener(this);
        btnRestart.setOnClickListener(this);
        btnEffect.setOnClickListener(this);

        fl_videoViewSmall.setOnClickListener(this);
        fl_videoViewPrimary.setOnClickListener(this);

        btnOrdered = findViewById(R.id.rl_ordered);
        tvNum = (MagicTextView) findViewById(R.id.tv_order_num);
        tvTips = (MagicTextView) findViewById(R.id.tv_order_tips);

        tvNum.setText("0");

        btnOrdered.setOnClickListener(this);

        btnBack = (LongTouchButton) findViewById(R.id.btn_back);
        btnForward = (LongTouchButton) findViewById(R.id.btn_forward);

        btnBack.setOnCumTouchListener(mCumTouchListener);
        btnForward.setOnCumTouchListener(mCumTouchListener);

        btnNextVideo = (Button) findViewById(R.id.btn_next);
        btnNextVideo.setOnClickListener(this);

        mSearchEdit = (EditText) findViewById(R.id.et_search);
        mSearchEdit.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        btnSearchClose = (Button) findViewById(R.id.btn_close_search);
        btnSearchClose.setOnClickListener(this);

        mLayoutManager = new PagerGridLayoutManager(mRows, mColumns, PagerGridLayoutManager
                .HORIZONTAL);

        rvSongList.setLayoutManager(mLayoutManager);

        // 设置滚动辅助工具
        PagerGridSnapHelper pageSnapHelper = new PagerGridSnapHelper();
        pageSnapHelper.attachToRecyclerView(rvSongList);
        mLayoutManager.setPageListener(new MainPageChangeListener());

        // 使用原生的 Adapter 即可
        mSongAdapter = new SongListAdapter(this);
        mSongAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                int count = mSongAdapter.getItemCount();
            }
        });

        mSongAdapter.setPreivewListener(new SongListAdapter.OnSongPreivewListener() {
            @Override
            public void onPreView(Song song) {
                new SongPreviewWin(song).show(getFragmentManager(), "priview");
            }
        });

        mSongAdapter.setListNotifyListener(this);
        rvSongList.setAdapter(mSongAdapter);
        rvSongList.setItemAnimator(null);
//        rvSongList.setHasFixedSize(true);


        cbSilent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mPresentationService.silentSwitchOn();
                } else {
                    mPresentationService.silentSwitchOff();
                }
            }
        });

        cbOrignal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //伴唱
                    mPresentationService.switchAccompanimentOrOriginal(false);
                } else {//原唱
                    mPresentationService.switchAccompanimentOrOriginal(true);

                }

            }
        });

        cbPlay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (isChecked) {
//
//                    if (mPresentationService.isPlaying()) {
//                        mPresentationService.pauseOrStart();
//                    } else {
////                        Song song = (Song) mSongAdapter.getItem(0);
////                        playVideo(song.filePath);
//                    }
//                } else {
//                    if (mPresentationService.isPlaying()) {
//                        mPresentationService.pauseOrStart();
//                    }
//                }

                mPresentationService.pauseOrStart();
            }
        });

        mSearchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String searchKey = mSearchEdit.getText().toString().trim();
                    LogUtils.i(searchKey);
                    if (!TextUtils.isEmpty(searchKey)) {
                        mVideoModelPresenter.search(searchKey);

                        InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                        im.hideSoftInputFromWindow(getCurrentFocus()
//                                .getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        if (im != null) {
                            im.hideSoftInputFromWindow(mSearchEdit.getWindowToken(), 0);
                        }
                        mSearchEdit.clearFocus();
                        mSearchMode = true;
                        btnSearchClose.setVisibility(View.VISIBLE);
                    }
                }
                return true;
            }
        });
    }

    private void subscribeEvent() {
        if (mDisposable != null) {
            mDisposable.dispose();
        }
        RxBus.getDefault()
                .toObservableWithCode(RxConstants.UPDATE_SELECT_SONG_CODE, String.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposable = d;
                    }

                    @Override
                    public void onNext(String value) {
                        if (value.equals(RxConstants.EXTRA_KEY_UPDATE_SELECT)) {
                            int size = VideoPlayListmanager.getIntanse().getPlaySongSize();
                            tvNum.setText(String.valueOf(size));
                            mSongAdapter.notifyDataSetChanged();
                            if (size == 1 && !isFirstSelected) {
                                final Song song = VideoPlayListmanager.getIntanse().getTop();
                                LogUtils.i("song: " + song);
                                if (song != null) {
                                    mPresentationService.playVideo(song);
                                    isFirstSelected = true;
                                }
                            } else {
                                mPresentationService.updateDisplayInfo();
                            }

                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        subscribeEvent();
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        RxBus.getDefault()
                .toObservableWithCode(RxConstants.SYNC_ORIGINAL_UI_CODE, Bundle.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new Observer<Bundle>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mDisposable = d;
                    }

                    @Override
                    public void onNext(Bundle value) {
                        if (value != null) {
                            final boolean check = value.getBoolean("check");
                            if (check) {
                                //伴唱
                                cbOrignal.setChecked(false);
                                LogUtils.i("update cb " + check);
                            } else {
                                cbOrignal.setChecked(true);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        subscribeEvent();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void onOrderedClick(View v) {
        if (popupVolume != null && popupVolume.isShowing()) {
            popupVolume.dismiss();
        }
        if (popupEffect != null && popupEffect.isShowing()) {
            popupEffect.dismiss();
        }
        if (popupOrdered != null && popupOrdered.isShowing()) {
            popupOrdered.dismiss();
            return;
        }
        try {
            initPopUpWindowsOrdered();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onVolumeClick(View v) {
        if (popupOrdered != null && popupOrdered.isShowing()) {
            popupOrdered.dismiss();
        }
        if (popupEffect != null && popupEffect.isShowing()) {
            popupEffect.dismiss();
        }
        if (popupVolume != null && popupVolume.isShowing()) {
            popupVolume.dismiss();
            return;
        }


        try {
            initPopUpWindowsVolume();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onEffectClick() {
        if (popupOrdered != null && popupOrdered.isShowing()) {
            popupOrdered.dismiss();
        }
        if (popupEffect != null && popupEffect.isShowing()) {
            popupEffect.dismiss();
        }
        if (popupVolume != null && popupVolume.isShowing()) {
            popupVolume.dismiss();
            return;
        }


        try {
            initPopUpWindowsEffect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPopUpWindowsOrdered() throws Exception {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View mView = inflater.inflate(R.layout.popup_ordersongs, null);

        v_ll_poporder_bg = mView.findViewById(R.id.order_bg);
        flSongsBg = mView.findViewById(R.id.ll_bg);

        Button btn_close = (Button) mView.findViewById(R.id.btn_close);
        btn_close.setOnClickListener(new CloseListener());

        rg_ordered = (RadioGroup) mView.findViewById(R.id.rg_menu);
        rbOrdered = (RadioButton) mView.findViewById(R.id.rb_order);
        rbOrdered.setChecked(true);
        rg_ordered.setOnCheckedChangeListener(new OrderedMenuCheckListener());

        mOrderSangView = new OrderSangView(this);
        mOrderSongsView = new OrderSongsView(this);

        flSongsBg.addView(mOrderSangView);
        flSongsBg.addView(mOrderSongsView);

        mOrderSangView.setVisibility(View.GONE);

        mOrderSongsView.setOnDeleteSongListener(new OrderSongsView.IOnDeleteSongListener() {

            @Override
            public void deleteSong(Song song) {

//                LogUtils.i(" " + rvSongList.hasFixedSize()
//                        + rvSongList.isAttachedToWindow());
                mSongAdapter.updateItem(song);
            }
        });

        int width = (int) (dm.widthPixels * (360.0 / 1008));
        popupOrdered = new PopupWindow(mView, width, (int) (width * (460.0f / 360.0f)));
        popupOrdered.setAnimationStyle(R.style.PopUpWindowAnimation);
        popupOrdered.showAsDropDown(btnOrdered, 0, 50);

        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.song_bg);
        Drawable drawable = new BitmapDrawable(getResources(), bmp);
        popupOrdered.setBackgroundDrawable(drawable);
        popupOrdered.setFocusable(true);
        popupOrdered.setOutsideTouchable(false);

        popupOrdered.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                try {
                    hideShadowText();
                    mOrderSangView = null;
                    mOrderSongsView = null;
                    popupOrdered = null;
                    updateList();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        showShadowText();

//        VideoPlayListmanager.getIntanse().addNotifyListen(new VideoPlayListmanager.INotifyPropertyChanged() {
//            @Override
//            public void update(final int size) {
//
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        tvNum.setText(size + "");
//                    }
//                });
//            }
//        });
    }

    private void initPopUpWindowsVolume() {

        if (mAudioMngHelper == null) {
            mAudioMngHelper = new AudioMngHelper(this);
            mAudioMngHelper.setVoiceStep100(MUSICE_VOL_STEP);
        }

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View mView = inflater.inflate(R.layout.popup_control_volume, null);
        int width = (int) (dm.widthPixels * (320.0 / 1080));
        Button btn_close = (Button) mView.findViewById(R.id.btn_close);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupVolume.dismiss();
            }
        });

        BoxedVertical music = (BoxedVertical) mView.findViewById(R.id.music_progress);
        music.setValue(mAudioMngHelper.get100CurrentVolume());
        LogUtils.i(mAudioMngHelper.get100CurrentVolume());
        music.setOnBoxedPointsChangeListener(new BoxedVertical.OnValuesChangeListener() {
            @Override
            public void onPointsChanged(BoxedVertical boxedPoints, int points) {
                mAudioMngHelper.setVoice100(points);
            }

            @Override
            public void onStartTrackingTouch(BoxedVertical boxedPoints) {

            }

            @Override
            public void onStopTrackingTouch(BoxedVertical boxedPoints) {

            }
        });

        //btn_popVolume_microRaise=mView.findViewById(R.id.)
//        popupVolume = new PopupWindow(mView, width, (int) (width * (360.0 / 370.0)));
        popupVolume = new PopupWindow(mView, width, width);
        popupVolume.setAnimationStyle(R.style.PopUpWindowVolumeAnimation);
        popupVolume.setOutsideTouchable(true);
        popupVolume.showAsDropDown(btnVolume, -5, -5);
    }

    private void initPopUpWindowsEffect() throws Exception {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View mView = inflater.inflate(R.layout.popup_control_effect, null);
        int width = (int) (dm.widthPixels * (540.0 / 1008));

        vp_popEffect_pitch_1 = (VerticalProgressBar) mView.findViewById(R.id.vp_pitch_1);
        vp_popEffect_pitch_2 = (VerticalProgressBar) mView.findViewById(R.id.vp_pitch_2);
        vp_popEffect_pitch_1.setCurrMode(VerticalProgressBar.MODE_BOTTOM);
        vp_popEffect_pitch_2.setCurrMode(VerticalProgressBar.MODE_TOP);
        vp_popEffect_pitch_1.setMax(100);
        vp_popEffect_pitch_2.setMax(100);


        float pitch = mPresentationService.getPitchShiftLevel();
        LogUtils.i("pitch = " + pitch);
        if (pitch > 0) {
            int val = (int) ((pitch / 3) * 100);
            LogUtils.i("val = " + val);
            vp_popEffect_pitch_1.setProgress(val);
            vp_popEffect_pitch_2.setProgress(0);
        } else if (pitch < 0) {
            int val = (int) ((-pitch / 3) * 100f);
            LogUtils.i("val = " + val);
            vp_popEffect_pitch_1.setProgress(0);
            vp_popEffect_pitch_2.setProgress(val);
        } else {
            vp_popEffect_pitch_1.setProgress(0);
            vp_popEffect_pitch_2.setProgress(0);
        }

        RepeatingButton btn_pitch_raise = (RepeatingButton) mView.findViewById(R.id.btn_pitch_raise);
        RepeatingButton btn_pitch_reduce = (RepeatingButton) mView.findViewById(R.id.btn_pitch_reduce);
        Button btn_pitch_origin = (Button) mView.findViewById(R.id.btn_pitch_origin);

        btn_pitch_origin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vp_popEffect_pitch_1.setProgress(0);
                vp_popEffect_pitch_2.setProgress(0);

                mPresentationService.setPitch(0);

            }
        });

        btn_pitch_raise.setRepeatListener(new RepeatingButton.RepeatListener() {
            @Override
            public void onRepeat(View v, long duration, int repeatcount) {
                int curr_progress_1 = vp_popEffect_pitch_1.getProgress();
                int curr_progress_2 = vp_popEffect_pitch_2.getProgress();

                if (curr_progress_1 == 0 && curr_progress_2 > 0) {
                    vp_popEffect_pitch_2.setProgress(vp_popEffect_pitch_2.getProgress() - 10);

                    float val = vp_popEffect_pitch_2.getProgress() * 0.01f * (-3);
                    mPresentationService.setPitch(val);
                } else if (curr_progress_2 == 0 && curr_progress_1 <= 90) {
                    vp_popEffect_pitch_1.setProgress(vp_popEffect_pitch_1.getProgress() + 10);

                    float val = vp_popEffect_pitch_1.getProgress() * 0.01f * 3;
                    mPresentationService.setPitch(val);
                }

            }
        }, CONSTANT_LONG_REPEAT_TIME);

        btn_pitch_reduce.setRepeatListener(new RepeatingButton.RepeatListener() {
            @Override
            public void onRepeat(View v, long duration, int repeatcount) {
                int curr_progress_1 = vp_popEffect_pitch_1.getProgress();
                int curr_progress_2 = vp_popEffect_pitch_2.getProgress();

                if (curr_progress_2 == 0 && curr_progress_1 > 0) {
                    vp_popEffect_pitch_1.setProgress(vp_popEffect_pitch_1.getProgress() - 10);

                    float val = vp_popEffect_pitch_1.getProgress() * 0.01f * 3;
                    mPresentationService.setPitch(val);

                } else if (curr_progress_1 == 0 && curr_progress_2 <= 90) {
                    vp_popEffect_pitch_2.setProgress(vp_popEffect_pitch_2.getProgress() + 10);

                    float val = vp_popEffect_pitch_2.getProgress() * 0.01f * (-3);
                    mPresentationService.setPitch(val);
                }
            }
        }, CONSTANT_LONG_REPEAT_TIME);

        btn_pitch_reduce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int curr_progress_1 = vp_popEffect_pitch_1.getProgress();
                int curr_progress_2 = vp_popEffect_pitch_2.getProgress();

                if (curr_progress_2 == 0 && curr_progress_1 > 0) {
                    vp_popEffect_pitch_1.setProgress(vp_popEffect_pitch_1.getProgress() - 10);
                    float val = vp_popEffect_pitch_1.getProgress() * 0.01f * 3;
                    mPresentationService.setPitch(val);

                } else if (curr_progress_1 == 0 && curr_progress_2 <= 90) {
                    vp_popEffect_pitch_2.setProgress(vp_popEffect_pitch_2.getProgress() + 10);

                    float val = vp_popEffect_pitch_2.getProgress() * 0.01f * (-3);
                    mPresentationService.setPitch(val);
                }
            }
        });

        btn_pitch_raise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int curr_progress_1 = vp_popEffect_pitch_1.getProgress();
                int curr_progress_2 = vp_popEffect_pitch_2.getProgress();

                if (curr_progress_1 == 0 && curr_progress_2 > 0) {
                    vp_popEffect_pitch_2.setProgress(vp_popEffect_pitch_2.getProgress() - 10);

                    float val = vp_popEffect_pitch_2.getProgress() * 0.01f * (-3);
                    mPresentationService.setPitch(val);

                } else if (curr_progress_2 == 0 && curr_progress_1 <= 90) {
                    vp_popEffect_pitch_1.setProgress(vp_popEffect_pitch_1.getProgress() + 10);

                    float val = vp_popEffect_pitch_1.getProgress() * 0.01f * 3;
                    mPresentationService.setPitch(val);
                }
            }
        });

        Button btn_close = (Button) mView.findViewById(R.id.btn_close);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (popupEffect != null && popupEffect.isShowing()) {
                        popupEffect.dismiss();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Button btn_gift = (Button) mView.findViewById(R.id.btn_gift);
        btn_gift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresentationService.showGiftPresentation();
            }
        });

        Button btn_multi = (Button) mView.findViewById(R.id.btn_multi_video);
        btn_multi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mPresentationService.isMultiVideoShow()) {
                    mPresentationService.showMultiVideoPresentation();
                } else {
                    mPresentationService.dismissMultiVideoPresentation();
                }
            }
        });


        popupEffect = new PopupWindow(mView, 600, 610);
        popupEffect.setAnimationStyle(R.style.PopUpWindowEffectAnimation);
        popupEffect.setOutsideTouchable(true);
        popupEffect.showAsDropDown(btnEffect, 0, 5);
    }

//    private void initPopUpWindowsEffect() throws Exception {
//        DisplayMetrics dm = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(dm);
//        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View mView = inflater.inflate(R.layout.popup_control_effect, null);
//        int width = (int) (dm.widthPixels * (540.0 / 1008));
//
//        VerticalSeekBar sb_pitchShift = (VerticalSeekBar) mView.findViewById(R.id.pitch_levelseek_bar);
//
//        int pitchShiftLevel = mPresentationService.getPitchShiftLevel();
//
//        int val = pitchShiftLevel / 3 / 2 * sb_pitchShift.getMax() + 50;
//        sb_pitchShift.setProgress(val);
//
//        sb_pitchShift.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                float pro = seekBar.getProgress();
//                float num = seekBar.getMax();
//                float result = ((pro - 50) / num) * 2;
//                int pitchShiftLevel = (int) (result * 3);
//                mPresentationService.setPitch(pitchShiftLevel);
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });
//
//        Button btn_close = (Button) mView.findViewById(R.id.btn_close);
//        btn_close.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                try {
//                    if (popupEffect != null && popupEffect.isShowing()) {
//                        popupEffect.dismiss();
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        Button btn_gift = (Button) mView.findViewById(R.id.btn_gift);
//        btn_gift.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                mPresentationService.showGiftPresentation();
//
//
//            }
//        });
//
//        Button btn_multi = (Button) mView.findViewById(R.id.btn_multi_video);
//        btn_multi.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (!mPresentationService.isMultiVideoShow()) {
//                    mPresentationService.showMultiVideoPresentation();
//                } else {
//                    mPresentationService.dismissMultiVideoPresentation();
//                }
//            }
//        });
//
//
//        popupEffect = new PopupWindow(mView, 600, 610);
//        popupEffect.setAnimationStyle(R.style.PopUpWindowEffectAnimation);
//        popupEffect.setOutsideTouchable(true);
//        popupEffect.showAsDropDown(btn_effect, 0, 5);
//    }

    private void destoryPopupWindow() {
        if (popupVolume != null && popupVolume.isShowing()) {
            popupVolume.dismiss();
        }
        if (popupEffect != null && popupEffect.isShowing()) {
            popupEffect.dismiss();
        }
        if (popupOrdered != null && popupOrdered.isShowing()) {
            popupOrdered.dismiss();
        }
    }


    private void showShadowText() throws Exception {
        tvNum.setShadowLayer(20, 3, 0, Color.rgb(255, 144, 0));
        tvNum.addOuterShadow(20, 3, 0, Color.rgb(255, 144, 0));
        tvTips.addOuterShadow(20, 3, 0, Color.rgb(255, 255, 255));
        tvTips.setShadowLayer(20, 3, 0, Color.rgb(255, 255, 255));
        tvNum.addOuterShadow(20, 3, 0, Color.rgb(255, 144, 0));
        tvTips.addOuterShadow(20, 3, 0, Color.rgb(255, 255, 255));
        tvTips.setTextColor(Color.argb(255, 255, 255, 255));
        tvNum.setTextColor(Color.argb(255, 255, 144, 0));
    }

    private void hideShadowText() throws Exception {
        tvNum.setTextColor(Color.argb(155, 255, 144, 0));
        tvTips.setTextColor(Color.argb(155, 255, 255, 255));
        tvNum.setShadowLayer(20, 3, 0, Color.argb(0, 0, 0, 0));
        tvTips.setShadowLayer(20, 3, 0, Color.argb(0, 0, 0, 0));
        tvNum.clearOuterShadows();
        tvTips.clearOuterShadows();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_ordered:
                onOrderedClick(v);
                break;
            case R.id.page_next:
                mLayoutManager.nextPage();
                break;
            case R.id.page_pre:
                mLayoutManager.prePage();
                break;
            case R.id.btn_volume:
                onVolumeClick(v);
                break;
            case R.id.btn_next:
                if (VideoPlayListmanager.getIntanse().getPlaySongSize() <= 1) {
                    Toast.makeText(this, R.string.video_play_complete, Toast.LENGTH_SHORT).show();
                    return;
                }

                mPresentationService.nextVideo();
                int size = VideoPlayListmanager.getIntanse().getPlaySongSize();
                tvNum.setText(String.valueOf(size));

                cbOrignal.setChecked(true);
                mSongAdapter.notifyDataSetChanged();
                break;
            case R.id.btn_resing:
                mPresentationService.songResing();
                break;
            case R.id.btn_effect:
                onEffectClick();
                break;
            case R.id.btn_close_search:
                mVideoModelPresenter.getData(false);
                mSearchMode = false;
                mSearchEdit.setText("");
                btnSearchClose.setVisibility(View.INVISIBLE);
                rl_listView.setVisibility(View.VISIBLE);
                break;

            case R.id.videoView_container:
                fl_videoViewPrimary.setVisibility(View.VISIBLE);
                rl_videoControl.setVisibility(View.GONE);
                fl_videoViewSmall.setVisibility(View.GONE);
                break;
            case R.id.primary_video_view:
                fl_videoViewPrimary.setVisibility(View.GONE);
                rl_videoControl.setVisibility(View.VISIBLE);
                fl_videoViewSmall.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void playSyncVideo(Song song) {

        mPresentationService.playVideo(song);

        cbPlay.setChecked(false);
    }

    private void playVideo(String url) {
        mPresentationService.playVideo(url, 0);
    }

    private class MainPageChangeListener implements PagerGridLayoutManager
            .PageListener {

        @Override
        public void onPageSizeChanged(int pageSize) {
            mPageSum = pageSize;
            String pageStr = String.format("%d/%d", mCurrentPageIndex + 1, mPageSum);
            tvPageIndex.setText(pageStr);
        }

        @Override
        public void onPageSelect(int pageIndex) {
            mCurrentPageIndex = pageIndex;
            String pageStr = String.format("%d/%d", pageIndex + 1, mPageSum);
            tvPageIndex.setText(pageStr);
        }
    }

    private LongTouchButton.CumTouchListener mCumTouchListener = new LongTouchButton.CumTouchListener() {

        @Override
        public void onLongTouchUp(View v) {


        }

        @Override
        public void onLongTouchDown(View v) {

            if (v.getId() == R.id.btn_back) {
                mPresentationService.seekBack();
            } else if (v.getId() == R.id.btn_forward) {
                mPresentationService.seekForward();
            }
        }

        @Override
        public void onShortTouch(View v) {
            if (v.getId() == R.id.btn_back) {
                mPresentationService.seekBack();
            } else if (v.getId() == R.id.btn_forward) {
                mPresentationService.seekForward();
            }
        }

    };

    private class OrderedMenuCheckListener implements RadioGroup.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {

            switch (i) {
                case R.id.rb_order:
                    v_ll_poporder_bg.setBackgroundResource(R.drawable.song_bg);
                    mOrderSangView.setVisibility(View.GONE);
                    mOrderSongsView.setVisibility(View.VISIBLE);
                    mOrderSongsView.updateDataList();
                    break;
                case R.id.rb_rank:
                    v_ll_poporder_bg.setBackgroundResource(R.drawable.sing_bg);
                    mOrderSangView.setVisibility(View.VISIBLE);
                    mOrderSongsView.setVisibility(View.GONE);
                    mOrderSangView.updateDataList();
                    break;
                default:
                    break;
            }
        }
    }


    private class CloseListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            if (popupOrdered != null && popupOrdered.isShowing()) {
                popupOrdered.dismiss();
                return;
            }

        }
    }

}
