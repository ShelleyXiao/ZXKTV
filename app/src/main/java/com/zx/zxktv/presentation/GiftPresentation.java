package com.zx.zxktv.presentation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.zx.zxktv.R;

/**
 * User: ShaudXiao
 * Date: 2018-06-28
 * Time: 11:19
 * Company: zx
 * Description:
 * FIXME
 */

public class GiftPresentation extends Presentation {

    private ImageView iv_flower;

    public GiftPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    public GiftPresentation(Context outerContext, Display display, int theme) {
        super(outerContext, display, theme);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progressbar_presentation);

        iv_flower = (ImageView) findViewById(R.id.flower);

    }

    public void startAnimation() {
        PropertyValuesHolder anim1 = PropertyValuesHolder.ofFloat("scaleX", 1.2f, 0.8f, 1f);
        PropertyValuesHolder anim2 = PropertyValuesHolder.ofFloat("scaleY", 1.2f, 0.8f, 1f);
        PropertyValuesHolder anim3 = PropertyValuesHolder.ofFloat("alpha", 1.0f, 0f, 1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(iv_flower, anim1, anim2, anim3).setDuration(1000);
        animator.setInterpolator(new LinearInterpolator());

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                iv_flower.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                iv_flower.setVisibility(View.GONE);
                GiftPresentation.this.dismiss();
            }
        });
        animator.start();
    }
}
