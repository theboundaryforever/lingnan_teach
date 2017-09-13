package com.haoyu.app.view;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;

import com.haoyu.app.lingnan.teacher.R;


public class CenterCheckBox extends Button implements
		PopupWindow.OnDismissListener {
	private int normalIcon;// 正常状态下的图标
	private int pressIcon;// 按下状态下的图标
	private PopupWindow popupWindow;

	public CenterCheckBox(Context context) {
		super(context);
	}

	public CenterCheckBox(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CenterCheckBox(Context context, AttributeSet attrs) {
		super(context, attrs);
		initAttrs(context, attrs);
		setNormal();
	}

	// 初始化各种自定义参数
	private void initAttrs(Context context, AttributeSet attrs) {
		TypedArray typedArray = context.obtainStyledAttributes(attrs,
				R.styleable.drawable_cennter_button);
		normalIcon = typedArray.getResourceId(
				R.styleable.drawable_cennter_button_button_normalIcon, -1);
		pressIcon = typedArray.getResourceId(
				R.styleable.drawable_cennter_button_button_pressIcon, -1);
	}

	/**
	 * 设置正常模式下的按钮状态
	 */
	private void setNormal() {
		if (normalIcon != -1) {
			Drawable drawable = getResources().getDrawable(normalIcon);
			// / 这一步必须要做,否则不会显示.
			drawable.setBounds(0, 0, drawable.getMinimumWidth(),
					drawable.getMinimumHeight());
			this.setCompoundDrawables(null, null, drawable, null);
		}
	}

	/**
	 * 设置popupwindow的view
	 *
	 * @param view
	 */
	public void setPopupView(final View view, final int width) {
		if (popupWindow == null) {
			popupWindow = new PopupWindow(view, width,
					LayoutParams.MATCH_PARENT);
			popupWindow.setFocusable(true);
			popupWindow.setBackgroundDrawable(new BitmapDrawable());
			popupWindow.setOutsideTouchable(true);
			popupWindow.setOnDismissListener(CenterCheckBox.this);
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					popupWindow.dismiss();
				}
			});
		}
		setPress();
		popupWindow.showAsDropDown(this);
	}

	/**
	 * 隐藏弹出框
	 */
	public void hidePopup() {
		if (popupWindow != null && popupWindow.isShowing()) {
			popupWindow.dismiss();
		}
	}

	/**
	 * 设置选中时候的按钮状态
	 */
	private void setPress() {
		if (pressIcon != -1) {
			Drawable drawable = getResources().getDrawable(pressIcon);
			// / 这一步必须要做,否则不会显示.
			drawable.setBounds(0, 0, drawable.getMinimumWidth(),
					drawable.getMinimumHeight());
			this.setCompoundDrawables(null, null, drawable, null);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Drawable[] drawables = getCompoundDrawables();
		if (drawables != null) {
			Drawable drawableLeft = drawables[2];
			if (drawableLeft != null) {
				float textWidth = getPaint().measureText(getText().toString());
				int drawablePadding = getCompoundDrawablePadding();
				int drawableWidth = 0;
				drawableWidth = drawableLeft.getIntrinsicWidth();
				float bodyWidth = textWidth + drawableWidth + drawablePadding;
				setPadding(0, 0, (int) (getWidth() - bodyWidth), 0);
				canvas.translate((getWidth() - bodyWidth) / 2, 0);
			}
		}
		super.onDraw(canvas);
	}

	@Override
	public void onDismiss() {
		setNormal();
	}
}
