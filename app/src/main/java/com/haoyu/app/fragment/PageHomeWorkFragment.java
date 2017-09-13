package com.haoyu.app.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.haoyu.app.activity.MarkAssignmentActivity;
import com.haoyu.app.adapter.AssignmentListAdapter;
import com.haoyu.app.base.BaseFragment;
import com.haoyu.app.base.BaseResponseResult;
import com.haoyu.app.basehelper.AppBaseAdapter;
import com.haoyu.app.basehelper.BaseRecyclerAdapter;
import com.haoyu.app.basehelper.ViewHolder;
import com.haoyu.app.entity.AssignmentListResult;
import com.haoyu.app.entity.AssignmentUserListResult;
import com.haoyu.app.entity.AssignmentUserNumResult;
import com.haoyu.app.entity.MAssignmentEntity;
import com.haoyu.app.entity.MAssignmentUser;
import com.haoyu.app.entity.Paginator;
import com.haoyu.app.entity.State;
import com.haoyu.app.lingnan.teacher.R;
import com.haoyu.app.rxBus.MessageEvent;
import com.haoyu.app.utils.Action;
import com.haoyu.app.utils.Constants;
import com.haoyu.app.utils.OkHttpClientManager;
import com.haoyu.app.view.LoadFailView;
import com.haoyu.app.view.LoadingView;
import com.haoyu.app.xrecyclerview.XRecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import okhttp3.Request;

import static com.haoyu.app.lingnan.teacher.R.id.ll_assignment;
import static com.haoyu.app.lingnan.teacher.R.id.ll_state;

/**
 * 创建日期：2017/2/4 on 17:14
 * 描述:
 * 作者:马飞奔 Administrator
 */
public class PageHomeWorkFragment extends BaseFragment implements View.OnClickListener, XRecyclerView.LoadingListener {
    private String courseId;
    @BindView(R.id.loadView)
    LoadingView loadView;
    @BindView(R.id.loadFailView)
    LoadFailView loadFailView;
    @BindView(R.id.contentView)
    LinearLayout contentView;
    @BindView(R.id.rl_shake)
    View rl_shake;
    @BindView(R.id.ic_state)
    ImageView ic_state;  //领取状态图片标识
    @BindView(R.id.tv_readOverNum)
    TextView tv_readOverNum;  //领取的作业数
    @BindView(R.id.ll_notReceivedNum)
    View ll_notReceivedNum;
    @BindView(R.id.empty_receivedNum)
    View empty_receivedNum;
    @BindView(R.id.tv_notReceivedNum)
    TextView tv_notReceivedNum;
    @BindView(R.id.tv_shark)
    View tv_shark;
    @BindViews({ll_assignment, ll_state})
    LinearLayout[] layouts;  //作业列表查看类型 （作业Id，作业状态）
    private List<MAssignmentEntity> mAssignments = new ArrayList<>();
    @BindViews({R.id.tv_assignmentId, R.id.tv_assignmentState})
    TextView[] textViews;
    @BindView(R.id.xRecyclerView)
    XRecyclerView xRecyclerView;
    private List<MAssignmentUser> mDatas = new ArrayList<>();
    private AssignmentListAdapter adapter;
    private boolean isRefresh, isLoadMore, needDialog = true;
    private int page = 1;
    private int limit = 20;

    @Override
    public int createView() {
        return R.layout.fragment_page_homework;
    }

    @Override
    public void initView(View view) {
        Bundle bundle = getArguments();
        if (bundle != null) {
            courseId = bundle.getString("entityId");
        }
        adapter = new AssignmentListAdapter(context, mDatas);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        xRecyclerView.setLayoutManager(layoutManager);
        xRecyclerView.setAdapter(adapter);
        xRecyclerView.setLoadingListener(this);
        mAssignments.add(0, null);
    }

    @Override
    public void initData() {
        String url = Constants.OUTRT_NET + "/" + courseId + "/teach/m/assignment/user/getAssignmentUserNum?relationId=" + courseId;
        addSubscription(OkHttpClientManager.getAsyn(context, url, new OkHttpClientManager.ResultCallback<AssignmentUserNumResult>() {
            @Override
            public void onBefore(Request request) {
                loadView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(Request request, Exception e) {
                loadView.setVisibility(View.GONE);
                loadFailView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onResponse(AssignmentUserNumResult response) {
                loadView.setVisibility(View.GONE);
                contentView.setVisibility(View.VISIBLE);
                updateUI(response);
                getAssignmentList();
            }
        }));
    }

    /*获取作业列表*/
    private void getAssignmentList() {
        String url = Constants.OUTRT_NET + "/" + courseId + "/teach/m/assignment/user?relationId=" + courseId
                + "&page=" + page + "&limit=" + limit;
        if (assignmentId != null) {
            url += "&assignmentId=" + assignmentId;
        }
        if (state != null) {
            url += "&state=" + state;
        }
        addSubscription(OkHttpClientManager.getAsyn(context, url, new OkHttpClientManager.ResultCallback<AssignmentUserListResult>() {

            @Override
            public void onBefore(Request request) {
                if (needDialog) {
                    showTipDialog();
                }
            }

            @Override
            public void onError(Request request, Exception e) {
                hideTipDialog();
                if (isRefresh) {
                    xRecyclerView.refreshComplete(false);
                } else if (isLoadMore) {
                    page -= 1;
                    xRecyclerView.loadMoreComplete(false);
                }
            }

            @Override
            public void onResponse(AssignmentUserListResult response) {
                hideTipDialog();
                if (response != null && response.getResponseData() != null
                        && response.getResponseData().getmAssignmentUsers() != null) {
                    updateUI(response.getResponseData().getmAssignmentUsers(),
                            response.getResponseData().getPaginator());
                }
            }
        }));
    }

    private void updateUI(List<MAssignmentUser> mAssignmentUsers, Paginator paginator) {
        if (isRefresh) {
            mDatas.clear();
            xRecyclerView.refreshComplete(true);
        } else if (isLoadMore) {
            xRecyclerView.loadMoreComplete(true);
        }
        mDatas.addAll(mAssignmentUsers);
        adapter.notifyDataSetChanged();
        if (paginator != null && paginator.getHasNextPage()) {
            xRecyclerView.setLoadingMoreEnabled(true);
        } else {
            xRecyclerView.setLoadingMoreEnabled(false);
        }
    }

    private int clickPosition;

    @Override
    public void setListener() {
        loadFailView.setOnRetryListener(new LoadFailView.OnRetryListener() {
            @Override
            public void onRetry(View v) {
                initData();
            }
        });
        rl_shake.setOnClickListener(this);
        layouts[0].setOnClickListener(this);
        layouts[1].setOnClickListener(this);
        adapter.setOnItemClickListener(new BaseRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseRecyclerAdapter adapter, BaseRecyclerAdapter.RecyclerHolder holder, View view, int position) {
                Intent intent = new Intent(context, MarkAssignmentActivity.class);
                intent.putExtra("courseId", courseId);
                clickPosition = position - 1;
                MAssignmentUser mAssignmentUser = mDatas.get(clickPosition);
                if (mAssignmentUser.getmUser() != null) {
                    intent.putExtra("userName", mAssignmentUser.getmUser().getRealName());
                }
                intent.putExtra("state", mAssignmentUser.getState());
                intent.putExtra("relationId", mAssignmentUser.getId());
                startActivity(intent);
            }
        });
    }

    private boolean isLoad = false;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_shake:
                getHomeWork();
                break;
            case R.id.ll_assignment:
                if (!isLoad) {
                    getAssignment();
                } else {
                    setAssignmentPopupView(mAssignments);
                }
                break;
            case R.id.ll_state:
                List<State> mStates = getStates();
                setStatePopupView(mStates);
                break;
        }
    }

    /**
     * 领取作业
     */
    private void getHomeWork() {
        String userId = getUserId();
        String url = Constants.OUTRT_NET + "/" + courseId + "/teach/unique_uid_" + userId + "/m/assignment/mark/" + courseId;
        addSubscription(OkHttpClientManager.postAsyn(context, url, new OkHttpClientManager.ResultCallback<BaseResponseResult>() {
            @Override
            public void onBefore(Request request) {
                showTipDialog();
            }

            @Override
            public void onError(Request request, Exception e) {
                hideTipDialog();
            }

            @Override
            public void onResponse(BaseResponseResult response) {
                hideTipDialog();
                if (response != null && response.getResponseData() != null) {
                    toastFullScreen("成功领取了" + response.getResponseData() + "分作业", true);
                    isRefresh = true;
                    page = 1;
                    getAssignmentList();
                } else {
                    if (response != null && response.getResponseMsg() != null) {
                        toast("暂时没有未批阅的作业");
                    }
                }
            }
        }));
    }

    /*作业活动列表*/
    private void getAssignment() {
        String url = Constants.OUTRT_NET + "/" + courseId + "/teach/m/assignment?relationId=" + courseId
                + "&markType=teacher";
        addSubscription(OkHttpClientManager.getAsyn(context, url, new OkHttpClientManager.ResultCallback<AssignmentListResult>() {
            @Override
            public void onError(Request request, Exception e) {
                isLoad = false;
            }

            @Override
            public void onResponse(AssignmentListResult response) {
                isLoad = true;
                if (response != null && response.getResponseData() != null) {
                    mAssignments.addAll(response.getResponseData());
                    setAssignmentPopupView(mAssignments);
                }
            }
        }));
    }

    private List<State> getStates() {
        List<State> mDatas = new ArrayList<>();
        mDatas.add(new State("", "全部状态"));
        mDatas.add(new State("commit", "待批阅"));
        mDatas.add(new State("complete", "已批阅"));
        mDatas.add(new State("return", "发回重做"));
        return mDatas;
    }

    private int selectItem;
    private String assignmentId;

    private void setAssignmentPopupView(final List<MAssignmentEntity> list) {
        Drawable shouqi = ContextCompat.getDrawable(context,
                R.drawable.assignment_shouqi);
        shouqi.setBounds(0, 0, shouqi.getMinimumWidth(),
                shouqi.getMinimumHeight());
        final Drawable zhankai = ContextCompat.getDrawable(context,
                R.drawable.assignment_zhankai);
        zhankai.setBounds(0, 0, zhankai.getMinimumWidth(),
                zhankai.getMinimumHeight());
        textViews[0].setCompoundDrawables(null, null, shouqi, null);
        View view = View.inflate(context, R.layout.popupwindow_listview,
                null);
        final PopupWindow AssignmentPopupWindow = new PopupWindow(view, layouts[0].getWidth(),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        ListView lv = view.findViewById(R.id.listView);
        final AssignmentAdapter adapter = new AssignmentAdapter(context, list, 0);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                AssignmentPopupWindow.dismiss();
                selectItem = position;
                adapter.setSelectItem(selectItem);
                if (position == 0) {
                    assignmentId = null;
                    textViews[0].setText("全部作业");
                } else {
                    assignmentId = list.get(position).getId();
                    textViews[0].setText(list.get(position).getTitle());
                }
                isRefresh = true;
                page = 1;
                needDialog = true;
                getAssignmentList();
            }
        });
        AssignmentPopupWindow.setFocusable(true);
        AssignmentPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        AssignmentPopupWindow.setOutsideTouchable(true);
        AssignmentPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                textViews[0].setCompoundDrawables(null, null, zhankai, null);
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AssignmentPopupWindow.dismiss();
            }
        });
        AssignmentPopupWindow.showAsDropDown(layouts[0]);
    }

    private String state;

    private void setStatePopupView(final List<State> list) {
        Drawable shouqi = ContextCompat.getDrawable(context,
                R.drawable.assignment_shouqi);
        shouqi.setBounds(0, 0, shouqi.getMinimumWidth(),
                shouqi.getMinimumHeight());
        final Drawable zhankai = ContextCompat.getDrawable(context,
                R.drawable.assignment_zhankai);
        zhankai.setBounds(0, 0, zhankai.getMinimumWidth(),
                zhankai.getMinimumHeight());
        textViews[1].setCompoundDrawables(null, null, shouqi, null);
        View view = View.inflate(context, R.layout.popupwindow_listview,
                null);
        final PopupWindow statePopupWindow = new PopupWindow(view, layouts[1].getWidth(),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        ListView lv = view.findViewById(R.id.listView);
        final StateAdapter adapter = new StateAdapter(context, list, 0);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                statePopupWindow.dismiss();
                selectItem = position;
                adapter.setSelectItem(selectItem);
                textViews[1].setText(list.get(position).getContent());
                if (position == 0) {
                    state = null;
                } else {
                    state = list.get(position).getState();
                }
                isRefresh = true;
                page = 1;
                needDialog = true;
                getAssignmentList();
            }
        });
        statePopupWindow.setFocusable(true);
        statePopupWindow.setBackgroundDrawable(new BitmapDrawable());
        statePopupWindow.setOutsideTouchable(true);
        statePopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                textViews[1].setCompoundDrawables(null, null, zhankai, null);
            }
        });
        statePopupWindow.showAsDropDown(layouts[1]);
    }

    @Override
    public void onRefresh() {
        isRefresh = true;
        isLoadMore = false;
        needDialog = false;
        page = 1;
        getAssignmentList();
    }

    @Override
    public void onLoadMore() {
        isRefresh = false;
        isLoadMore = true;
        needDialog = false;
        page += 1;
        getAssignmentList();
    }

    private void updateUI(AssignmentUserNumResult userNumResult) {
        if (userNumResult != null && userNumResult.getResponseData() != null) {
            tv_readOverNum.setText(userNumResult.getResponseData().getMarkNum() + "/" + userNumResult.getResponseData().getAllNum());
            if (userNumResult.getResponseData().getNotReceivedNum() > 0) {
                ll_notReceivedNum.setVisibility(View.VISIBLE);
                ic_state.setImageResource(R.drawable.assignment_queren_tip);
                tv_notReceivedNum.setText(String.valueOf(userNumResult.getResponseData().getNotReceivedNum()));
                tv_shark.setVisibility(View.VISIBLE);
                rl_shake.setEnabled(true);
            } else {
                ic_state.setImageResource(R.drawable.assignment_get_tips);
                empty_receivedNum.setVisibility(View.VISIBLE);
                rl_shake.setEnabled(false);
            }
        }
    }

    @Override
    public void obBusEvent(MessageEvent event) {
        if (event.action.equals(Action.GET_COURSE_ASSIGNMENT)) {
            isRefresh = true;
            page = 1;
            getAssignmentList();
        } else if (event.getAction().equals(Action.READ_OVER_ASSIGNMENT)) {
            mDatas.get(clickPosition).setState("complete");
            mDatas.get(clickPosition).setResponseScore(event.arg1);
            adapter.notifyDataSetChanged();
            toastFullScreen("批阅完成", true);
        } else if (event.getAction().equals(Action.RETURN_ASSIGNMENT_REDO)) {
            mDatas.get(clickPosition).setState("return");
            adapter.notifyDataSetChanged();
            toastFullScreen("发回重做完成", true);
        }
    }

    class AssignmentAdapter extends AppBaseAdapter<MAssignmentEntity> {
        private int selectItem;
        private Context context;

        public AssignmentAdapter(Context context, List<MAssignmentEntity> mDatas, int selectItem) {
            super(context, mDatas);
            this.selectItem = selectItem;
            this.context = context;
        }

        @Override
        public void convert(ViewHolder holder, MAssignmentEntity entity, int position) {
            TextView tv_name = holder.getView(R.id.tv_name);
            if (entity == null) {
                tv_name.setText("全部作业");
            } else {
                tv_name.setText(entity.getTitle());
            }
            if (selectItem == position) {
                tv_name.setTextColor(ContextCompat.getColor(context, R.color.defaultColor));
            } else {
                tv_name.setTextColor(ContextCompat.getColor(context, R.color.gray));
            }
        }

        public void setSelectItem(int selectItem) {
            this.selectItem = selectItem;
            notifyDataSetChanged();
        }

        @Override
        public int getmItemLayoutId() {
            return R.layout.assignment_name_list_item;
        }
    }

    class StateAdapter extends AppBaseAdapter<State> {
        private int selectItem;
        private Context context;

        public StateAdapter(Context context, List<State> mDatas, int selectItem) {
            super(context, mDatas);
            this.selectItem = selectItem;
            this.context = context;
        }

        @Override
        public void convert(ViewHolder holder, State state, int position) {
            TextView tv_name = holder.getView(R.id.tv_name);
            tv_name.setText(state.getContent());
            if (selectItem == position) {
                tv_name.setTextColor(ContextCompat.getColor(context, R.color.defaultColor));
            } else {
                tv_name.setTextColor(ContextCompat.getColor(context, R.color.gray));
            }
        }

        public void setSelectItem(int selectItem) {
            this.selectItem = selectItem;
            notifyDataSetChanged();
        }

        @Override
        public int getmItemLayoutId() {
            return R.layout.assignment_name_list_item;
        }
    }
}
