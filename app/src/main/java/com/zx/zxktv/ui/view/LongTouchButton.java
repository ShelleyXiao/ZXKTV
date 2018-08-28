package com.zx.zxktv.ui.view;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * User: ShaudXiao
 * Date: 2018-06-07
 * Time: 10:38
 * Company: zx
 * Description:
 * FIXME
 */

public class LongTouchButton extends AppCompatButton {

    private static final int TIME = 5000;

    private static final int TIME_CHECK = 100;
    private static final int TIME_LONGTOUCH = 1000;
    private static final int TIME_INTERRUPT_TO_START = 10000;

    /**
     * 璁板綍褰撳墠鑷畾涔塀tn鏄惁鎸変笅
     */
    private boolean isStillClick = false;

    private CumTouchListener mListener;

    private boolean mIsInterrupt = false;


    public LongTouchButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            long startTime = System.currentTimeMillis();
            isStillClick = true;
            new LongTouchTask().execute(startTime);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            isStillClick = false;
        }
        return super.onTouchEvent(event);
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class LongTouchTask extends AsyncTask<Long, Integer, Boolean> {
        private long start;
        private boolean isLongTouch = false;
        private boolean needCheckLongTouch = true;
        private long startLongTouch;

        @Override
        protected Boolean doInBackground(Long... params) {
            start = params[0];

            while (isStillClick) {
                sleep(TIME_CHECK);
//                LogUtils.i("needCheckLongTouch " + needCheckLongTouch + "dddddddddddddd start: " + start
//                        + " : " + (System.currentTimeMillis() - start));
                if ( System.currentTimeMillis() - start >= TIME_LONGTOUCH) {
                    publishProgress(0);

                    startLongTouch = System.currentTimeMillis();
                    isLongTouch = true;
                    needCheckLongTouch = false;
                }

                if (isLongTouch && mIsInterrupt && System.currentTimeMillis() - startLongTouch >= TIME_INTERRUPT_TO_START) {
                    return isLongTouch;
                }

            }


            return isLongTouch;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mListener.onLongTouchUp(LongTouchButton.this);
            } else {
                mListener.onShortTouch(LongTouchButton.this);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
//            LogUtils.i("onProgressUpdate start");
            if (values[0] == 0) {
                mListener.onLongTouchDown(LongTouchButton.this);
            }
        }

    }


    public void setOnCumTouchListener(CumTouchListener listener) {
        setOnCumTouchListener(listener, false);
    }


    public void setOnCumTouchListener(CumTouchListener listener, boolean interrupt) {
        mListener = listener;
        mIsInterrupt = interrupt;
    }


    public interface CumTouchListener {


        void onLongTouchUp(View v);

        void onLongTouchDown(View v);


        void onShortTouch(View v);
    }

    public static CumTouchListener CumTouchListener() {
        return null;
    }
}

