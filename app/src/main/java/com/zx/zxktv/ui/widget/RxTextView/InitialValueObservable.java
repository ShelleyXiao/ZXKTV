package com.zx.zxktv.ui.widget.RxTextView;

import io.reactivex.Observable;
import io.reactivex.Observer;

/**
 * User: ShaudXiao
 * Date: 2018-09-12
 * Time: 15:27
 * Company: zx
 * Description: 来自RxBinding
 * FIXME
 */

public abstract class InitialValueObservable<T> extends Observable<T> {
    @Override protected final void subscribeActual(Observer<? super T> observer) {
        subscribeListener(observer);
        observer.onNext(getInitialValue());
    }

    protected abstract void subscribeListener(Observer<? super T> observer);
    protected abstract T getInitialValue();

    public final Observable<T> skipInitialValue() {
        return new Skipped();
    }

    private final class Skipped extends Observable<T> {
        Skipped() {
        }

        @Override protected void subscribeActual(Observer<? super T> observer) {
            subscribeListener(observer);
        }
    }
}