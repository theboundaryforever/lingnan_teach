package com.haoyu.app.activity;

import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.haoyu.app.base.BaseActivity;
import com.haoyu.app.lingnan.teacher.R;
import com.haoyu.app.rxBus.MessageEvent;
import com.haoyu.app.rxBus.RxBus;
import com.haoyu.app.view.AppToolBar;

import butterknife.BindView;

/**
 * Created by acer1 on 2017/2/21.
 * 搜索成员
 */
public class SearchMemeberActivity extends BaseActivity {
    private SearchMemeberActivity context = this;
    @BindView(R.id.toolBar)
    AppToolBar toolBar;
    @BindView(R.id.iv_search)
    ImageView iv_search;
    @BindView(R.id.et_name)
    EditText et_name;

    @Override
    public int setLayoutResID() {
        return R.layout.managent_member_search;
    }

    @Override
    public void initView() {

    }

    @Override
    public void setListener() {

        toolBar.setOnLeftClickListener(new AppToolBar.OnLeftClickListener() {
            @Override
            public void onLeftClick(View view) {
                finish();
            }
        });
        iv_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = et_name.getText().toString().trim();
                if (msg.length() == 0) {
                    toast(context, "请输入姓名");
                } else {
                    MessageEvent event = new MessageEvent();
                    event.setAction("nameSearch");
                    event.setObj(msg);
                    RxBus.getDefault().post(event);
                    finish();
                }
            }
        });


    }

}
