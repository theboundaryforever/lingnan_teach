package com.haoyu.app.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import com.haoyu.app.basehelper.BaseArrayRecyclerAdapter;
import com.haoyu.app.entity.MAssignmentUser;
import com.haoyu.app.lingnan.teacher.R;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建日期：2017/2/6 on 14:30
 * 描述:作业列表适配器
 * 作者:马飞奔 Administrator
 */
public class AssignmentListAdapter extends BaseArrayRecyclerAdapter<MAssignmentUser> {
    private Context context;

    public AssignmentListAdapter(Context context, List<MAssignmentUser> mDatas) {
        super(mDatas);
        this.context = context;
    }

    @Override
    public void onBindHoder(RecyclerHolder holder, MAssignmentUser mAssignmentUser, int position) {
        TextView tv_userName = holder.obtainView(R.id.tv_userName);
        TextView tv_responseScore = holder.obtainView(R.id.tv_responseScore);
        TextView tv_state = holder.obtainView(R.id.tv_state);
        if (mAssignmentUser.getmUser() != null) {
            tv_userName.setText(mAssignmentUser.getmUser().getRealName());
        }
        if (mAssignmentUser.getState() != null && mAssignmentUser.getState().equals("commit")) {
            tv_responseScore.setText("--");
            tv_state.setText("待批阅");
            tv_state.setTextColor(ContextCompat.getColor(context, R.color.blue));
        } else if (mAssignmentUser.getState() != null && mAssignmentUser.getState().equals("complete")) {
            tv_responseScore.setText(String.valueOf(getScore(mAssignmentUser.getResponseScore())));
            tv_state.setText("已批阅");
            tv_state.setTextColor(ContextCompat.getColor(context, R.color.defaultColor));
        } else if (mAssignmentUser.getState() != null && mAssignmentUser.getState().equals("return")) {
            tv_responseScore.setText("--");
            tv_state.setText("发回重做");
            tv_state.setTextColor(ContextCompat.getColor(context, R.color.orange));
        } else {
            tv_responseScore.setText("--");
            tv_state.setText("状态未知");
            tv_state.setTextColor(ContextCompat.getColor(context, R.color.red));
        }
    }


    private int getScore(double score) {
        BigDecimal b = new BigDecimal(score);
        int count = (int) b.setScale(0, BigDecimal.ROUND_HALF_UP).floatValue();
        return count;
    }

    @Override
    public int bindView(int viewtype) {
        return R.layout.assignment_list_item;
    }
}
