package com.haoyu.app.adapter;

import android.widget.TextView;

import com.haoyu.app.basehelper.BaseArrayRecyclerAdapter;
import com.haoyu.app.entity.MobileUser;
import com.haoyu.app.lingnan.teacher.R;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by acer1 on 2017/2/16.
 * 得分明细
 */
public class TeachingStudyScoreDetailAdapter extends BaseArrayRecyclerAdapter<Double> {

    public TeachingStudyScoreDetailAdapter(List<Double> mDatas) {
        super(mDatas);
    }


    @Override
    public int bindView(int viewtype) {
        return R.layout.teaching_study_lecture_item;
    }


    @Override
    public void onBindHoder(RecyclerHolder holder, final Double mobileUser, int position) {
        TextView tv_name = holder.obtainView(R.id.tv_name);
        tv_name.setText(String.valueOf(Integer.parseInt(new DecimalFormat("0").format(mobileUser))));

    }

    public interface PersonName {
        void setName(MobileUser mobileUser);
    }

    public PersonName personName;

    public void setPersonName(PersonName personName) {
        this.personName = personName;
    }

}
