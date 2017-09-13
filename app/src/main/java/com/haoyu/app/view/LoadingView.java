package com.haoyu.app.view;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.haoyu.app.lingnan.teacher.R;


/**
 * 创建日期：2016/11/10 on 10:19
 * 描述:
 * 作者:马飞奔 Administrator
 */
public class LoadingView extends FrameLayout {
    private ImageView iv_loading;
    private TextView mLoadTextView;
    private AnimationDrawable mDrawable;
    private String mLoadText;

    public LoadingView(Context context) {
        super(context);
        init(context);
    }

    public LoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mLoadText = getResources().getString(R.string.layout_loading_text);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_loading, null);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        iv_loading = view.findViewById(R.id.iv_loading);
        mLoadTextView = view.findViewById(R.id.loadingText);
        mDrawable = (AnimationDrawable) iv_loading.getDrawable();
        setLoadingText(mLoadText);
        addView(view, layoutParams);
    }

    public void setLoadingText(CharSequence loadingText) {
        if (TextUtils.isEmpty(loadingText)) {
            mLoadTextView.setVisibility(GONE);
        } else {
            mLoadTextView.setVisibility(VISIBLE);
        }
        mLoadTextView.setText(loadingText);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && mDrawable != null && !mDrawable.isRunning()) {
            mDrawable.start();
        } else {
            if (mDrawable != null && mDrawable.isRunning()) {
                mDrawable.stop();
            }
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE && mDrawable != null && !mDrawable.isRunning()) {
            mDrawable.start();
        } else {
            if (mDrawable != null && mDrawable.isRunning()) {
                mDrawable.stop();
            }
        }
    }
}
