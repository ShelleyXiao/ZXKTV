package com.zx.zxktv.ui.widget;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * User: ShaudXiao
 * Date: 2018-12-03
 * Time: 15:45
 * Company: zx
 * Description:
 * FIXME
 */

public class KeyboardStatusDetector {

    private static final int SOFT_KEY_BORAD_MIN_HEIGH = 100;

    private KeyboradVisibilityListener mVisibilityListener;

    boolean keyboardVisible = false;

    public KeyboardStatusDetector registerActivity(Activity a) {
        return registerView(a.getWindow().getDecorView().findViewById(android.R.id.content));
    }

    public KeyboardStatusDetector registerView(final View v) {
        v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                v.getWindowVisibleDisplayFrame(r);

                int heightDiff = v.getRootView().getHeight()
                        - (r.bottom - r.top);
                if(heightDiff > SOFT_KEY_BORAD_MIN_HEIGH) {
                    if(!keyboardVisible) {
                        keyboardVisible = true;
                        if(mVisibilityListener != null) {
                            mVisibilityListener.onVisibilityChanged(true);
                        }

                    }
                } else {
                    if(keyboardVisible) {
                        keyboardVisible = false;
                        if(mVisibilityListener != null) {
                            mVisibilityListener.onVisibilityChanged(false);
                        }
                    }
                }
            }
        });

        return this;
    }

    public KeyboardStatusDetector setVisibilityListener(KeyboradVisibilityListener visibilityListener) {
        mVisibilityListener = visibilityListener;
        return this;
    }

    public interface KeyboradVisibilityListener {
        void onVisibilityChanged(boolean keyboardVisible);
    }
}
