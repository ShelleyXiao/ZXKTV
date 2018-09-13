package com.zx.zxktv.ui.widget.RxTextView;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.widget.TextView;

/**
 * User: ShaudXiao
 * Date: 2018-09-12
 * Time: 15:17
 * Company: zx
 * Description:
 * FIXME
 */

public class RxTextView {


    @CheckResult @NonNull
    public static InitialValueObservable<CharSequence> textChanges(@NonNull TextView view) {
        if (view == null) {
            throw new NullPointerException("view == null");
        }
        return new TextViewTextObservable(view);
    }

    private RxTextView() {
        throw new AssertionError("No instances.");
    }
}
