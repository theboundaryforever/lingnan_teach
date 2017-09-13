package com.haoyu.app.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.haoyu.app.lingnan.teacher.R;
import com.haoyu.app.utils.ScreenUtils;

/**
 * 创建日期：2017/4/6 on 17:24
 * 描述:
 * 作者:马飞奔 Administrator
 */
public class MaterialDialog extends AlertDialog {
    private View view;
    private TextView tv_tips; // 提示框标题
    private TextView tv_message; // 提示内容
    private Button bt_makesure; // 确定按钮
    private Button bt_cancel; // 取消按钮
    private int dialog_width;

    public MaterialDialog(Context context) {
        super(context);
        view = LayoutInflater.from(context).inflate(R.layout.dialog_material, new LinearLayout(context), false);
        tv_tips = view.findViewById(R.id.tv_tips);
        tv_message = view.findViewById(R.id.tv_message);
        bt_makesure = view.findViewById(R.id.bt_makesure);
        bt_cancel = view.findViewById(R.id.bt_cancel);
        dialog_width = ScreenUtils.getScreenWidth(context) / 4 * 3;
        tv_message.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dialog_width,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        setContentView(view, params);
        setCanceledOnTouchOutside(false);
    }

    @Override
    public void setTitle(CharSequence title) {
        tv_tips.setText(title);
    }

    @Override
    public void setMessage(CharSequence message) {
        tv_message.setText(message);
    }

    /* 确定按钮 */
    public void setPositiveButton(String text, final ButtonClickListener listener) {
        bt_makesure.setText(text);
        bt_makesure.setVisibility(View.VISIBLE);
        bt_makesure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.onClick(bt_makesure, MaterialDialog.this);
                }
            }
        });
    }

    /* 取消按钮 */
    public void setNegativeButton(String text, final ButtonClickListener listener) {
        bt_cancel.setText(text);
        bt_cancel.setVisibility(View.VISIBLE);
        bt_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.onClick(bt_cancel, MaterialDialog.this);
                }
            }
        });
    }

    public void setNegativeTextColor(int color) {
        bt_cancel.setTextColor(color);
    }

    public void setPositiveTextColor(int color) {
        bt_makesure.setTextColor(color);
    }

    public interface ButtonClickListener {
        void onClick(View v, AlertDialog dialog);
    }
}
