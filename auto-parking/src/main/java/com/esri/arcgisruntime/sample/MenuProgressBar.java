package com.esri.arcgisruntime.sample;

import android.content.Context;
import android.widget.ProgressBar;

import com.mypopsy.widget.internal.ViewUtils;

/**
 * Created by mani8177 on 7/12/16.
 */
public class MenuProgressBar extends ProgressBar {

    public MenuProgressBar(Context context) {
        super(context);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(ViewUtils.dpToPx(48), ViewUtils.dpToPx(24));
    }
}