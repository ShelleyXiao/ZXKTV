package com.zx.zxktv.ui.widget;

/**
 * User: ShaudXiao
 * Date: 2018-06-21
 * Time: 16:56
 * Company: zx
 * Description:
 * FIXME
 */

public class Tab {
    private int Image;
    private String Text;
    private Class Fragment;

    public Tab(int image, String text, Class fragment) {
        Image = image;
        Text = text;
        Fragment = fragment;
    }

    public int getImage() {
        return Image;
    }

    public void setImage(int image) {
        Image = image;
    }

    public String getText() {
        return Text;
    }

    public void setText(String text) {
        Text = text;
    }

    public Class getFragment() {
        return Fragment;
    }

    public void setFragment(Class fragment) {
        Fragment = fragment;
    }
}


