package com.haoyu.app.activity;

import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.haoyu.app.base.BaseActivity;
import com.haoyu.app.basehelper.BaseArrayRecyclerAdapter;
import com.haoyu.app.entity.MTrainRegisterStat;
import com.haoyu.app.entity.MobileUser;
import com.haoyu.app.entity.TrainScoreSingleResult;
import com.haoyu.app.lingnan.teacher.R;
import com.haoyu.app.utils.Constants;
import com.haoyu.app.utils.OkHttpClientManager;
import com.haoyu.app.utils.TimeUtil;
import com.haoyu.app.view.FullyLinearLayoutManager;
import com.haoyu.app.view.LoadFailView;
import com.haoyu.app.view.LoadingView;

import java.util.List;

import butterknife.BindView;
import okhttp3.Request;


/**
 * 创建日期：2017/1/19 on 19:52
 * 描述:
 */
public class StudentAchievementDetailActivity extends BaseActivity implements View.OnClickListener {
    private StudentAchievementDetailActivity context = this;
    @BindView(R.id.person_name)
    TextView person_name;
    @BindView(R.id.person_unit)
    TextView person_unit;//所在单位
    @BindView(R.id.id_card)
    TextView id_card;//身份证号
    @BindView(R.id.join_project)
    TextView join_project;//参与项目
    @BindView(R.id.train_semester)
    TextView train_semester;//培训期次
    @BindView(R.id.train_time)
    TextView train_time;//培训时间
    @BindView(R.id.course_num)
    TextView course_num;//课程学习数量
    @BindView(R.id.course_learn)
    TextView course_learn;//课程学习结果
    @BindView(R.id.workgroup_reaearch)
    TextView workgroup_reaearch;//工作坊研修数量
    @BindView(R.id.workgroup_score)
    TextView workgroup_score;//工作坊研修成绩
    @BindView(R.id.society)
    TextView society;//社区拓展数量
    @BindView(R.id.society_state)
    TextView society_state;//社区拓展成绩
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.re_back)
    ImageView re_back;//返回
    @BindView(R.id.course_hours)
    TextView course_hours;//学时
    private String trainId;
    private String trainRegisterId;

    private MobileUser mUser;
    @BindView(R.id.scrollview)
    NestedScrollView scrollview;
    @BindView(R.id.loadingView)
    LoadingView loadingView;
    @BindView(R.id.loadFailView)
    LoadFailView loadFailView;

    @Override
    public int setLayoutResID() {
        return R.layout.activity_student_achievement;
    }

    @Override
    public void initView() {
        trainId = getIntent().getStringExtra("trainId");
        trainRegisterId = getIntent().getStringExtra("trainRegisterId");
        mUser = (MobileUser) getIntent().getSerializableExtra("mUser");
    }

    @Override
    public void setListener() {
        re_back.setOnClickListener(this);
        loadFailView.setOnRetryListener(new LoadFailView.OnRetryListener() {
            @Override
            public void onRetry(View v) {
                initData();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.re_back:
                this.finish();
                break;
        }
    }

    public void initData() {
        String url = Constants.OUTRT_NET + "/m/manage/getTrainRegisterStat?trainId=" + trainId + "&trainRegisterId=" + trainRegisterId;
        OkHttpClientManager.getAsyn(context, url, new OkHttpClientManager.ResultCallback<TrainScoreSingleResult>() {

            @Override
            public void onError(Request request, Exception e) {

                loadFailView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onResponse(TrainScoreSingleResult response) {
                loadingView.setVisibility(View.GONE);
                if (response != null && response.getResponseData() != null && response.getResponseCode() != null && response.getResponseCode().equals("00")) {
                    updateUI(response.getResponseData());
                    loadFailView.setVisibility(View.GONE);
                    scrollview.setVisibility(View.VISIBLE);
                } else {
                    loadFailView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void updateUI(MTrainRegisterStat responseData) {
        if (mUser != null) {
            person_name.setText(mUser.getRealName());
            person_unit.setText(mUser.getDeptName());
        }
        join_project.setText(responseData.getProjectName());
        course_num.setText(String.valueOf(responseData.getRegistedCourseNum()));
        course_learn.setText(responseData.getCourseEvaluate());
        workgroup_reaearch.setText(String.valueOf(responseData.getWorkshopNum()));
        workgroup_score.setText(responseData.getWorkshopEvaluate());
        society_state.setText(responseData.getCommunityEvaluate());
        if (responseData.getmTrain() != null && responseData.getmTrain().getTrainingTime() != null) {
            long startTime = responseData.getmTrain().getTrainingTime().getStartTime();
            long endTime = responseData.getmTrain().getTrainingTime().getEndTime();
            train_time.setText(TimeUtil.convertDayOfMinute(startTime, endTime));
        }
        course_hours.setText(responseData.getTotalStudyHours() + "学时");
        CourseAdapter mAdapter = new CourseAdapter(responseData.getmCourseRegisters());
        FullyLinearLayoutManager manager = new FullyLinearLayoutManager(context);
        manager.setOrientation(FullyLinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(mAdapter);
    }

    class CourseAdapter extends BaseArrayRecyclerAdapter<MTrainRegisterStat.CourseRegisters> {

        public CourseAdapter(List<MTrainRegisterStat.CourseRegisters> mDatas) {
            super(mDatas);
        }

        @Override
        public void onBindHoder(RecyclerHolder holder, MTrainRegisterStat.CourseRegisters entity, int position) {
            TextView choose_achievement_title = holder.obtainView(R.id.choose_achievement_title);
            TextView choose_achievement_score = holder.obtainView(R.id.choose_achievement_score);
            if (entity.getmCourse() != null) {
                choose_achievement_title.setText(entity.getmCourse().getTitle());
            }
            choose_achievement_score.setText(String.valueOf(entity.getScore()));
        }

        @Override
        public int bindView(int viewtype) {
            return R.layout.student_choose_achievement;
        }
    }
}
