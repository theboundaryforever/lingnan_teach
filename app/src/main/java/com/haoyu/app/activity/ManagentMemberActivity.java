package com.haoyu.app.activity;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.haoyu.app.adapter.ManagementMemberAdapter;
import com.haoyu.app.base.BaseActivity;
import com.haoyu.app.base.BaseResponseResult;
import com.haoyu.app.entity.ManagementMemberEntity;
import com.haoyu.app.entity.ManagementMemberResult;
import com.haoyu.app.entity.MobileUser;
import com.haoyu.app.lingnan.teacher.R;
import com.haoyu.app.rxBus.MessageEvent;
import com.haoyu.app.utils.Constants;
import com.haoyu.app.utils.OkHttpClientManager;
import com.haoyu.app.view.AppToolBar;
import com.haoyu.app.view.LoadFailView;
import com.haoyu.app.view.LoadingView;
import com.haoyu.app.xrecyclerview.XRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import okhttp3.Request;

/**
 * Created by acer1 on 2017/2/8.
 * 成员管理
 */
public class ManagentMemberActivity extends BaseActivity implements View.OnClickListener, XRecyclerView.LoadingListener {
    private ManagentMemberActivity context = this;
    @BindView(R.id.toolBar)
    AppToolBar toolBar;
    @BindView(R.id.member_count)
    TextView member_count;//人数
    @BindView(R.id.xRecyclerView)
    XRecyclerView xRecyclerView;

    @BindView(R.id.warn_msg)
    TextView warn_msg;
    private String workshopId;
    private int page = 1;
    private boolean isRefresh, isLoadMore;
    private ManagementMemberAdapter adapter;
    private List<ManagementMemberEntity> mobileUserList = new ArrayList<>();
    @BindView(R.id.ll_show)
    LinearLayout ll_show;
    @BindView(R.id.loadingView)
    LoadingView loadingView;
    @BindView(R.id.loadFailView)
    LoadFailView loadFailView;

    @Override
    public void obBusEvent(MessageEvent event) {
        String action = event.getAction();
        if (action.equals("nameSearch")) {
            String msg = event.getObj().toString();
            searchByName(msg);
        }
    }

    @Override
    public int setLayoutResID() {
        return R.layout.management_member;
    }

    @Override
    public void initView() {
        workshopId = getIntent().getStringExtra("workshopId");
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        xRecyclerView.setLayoutManager(layoutManager);
        adapter = new ManagementMemberAdapter(context, mobileUserList);
        xRecyclerView.setArrowImageView(R.drawable.refresh_arrow);
        xRecyclerView.setAdapter(adapter);
        xRecyclerView.setLoadingListener(this);
        registRxBus();
    }

    @Override
    public void setListener() {

        member_count.setOnClickListener(context);
    toolBar.setOnLeftClickListener(new AppToolBar.OnLeftClickListener() {
        @Override
        public void onLeftClick(View view) {
            finish();
        }
    });
        toolBar.setOnRightClickListener(new AppToolBar.OnRightClickListener() {
            @Override
            public void onRightClick(View view) {
                startActivity(new Intent(context, SearchMemeberActivity.class));
            }
        });
        loadFailView.setOnRetryListener(new LoadFailView.OnRetryListener() {
            @Override
            public void onRetry(View v) {
                initData();
            }
        });
        adapter.setDisposeCallBack(new ManagementMemberAdapter.onDisposeCallBack() {
            @Override
            public void onAlter(int position, MobileUser entity) {
            }

            @Override
            public void onDelete(int position, MobileUser entity) {

                String id = mobileUserList.get(position).getId();
                //删除成员
                String url = Constants.OUTRT_NET + "/master_" + workshopId + "/m/workshop_user/" + id;
                Map<String, String> map = new HashMap<>();
                map.put("_method", "delete");

                OkHttpClientManager.postAsyn(context, url, new OkHttpClientManager.ResultCallback<BaseResponseResult>() {
                    @Override
                    public void onBefore(Request request) {
                        super.onBefore(request);
                        showTipDialog();
                    }

                    @Override
                    public void onError(Request request, Exception e) {
                        hideTipDialog();
                        toast(context, "删除失败");

                    }

                    @Override
                    public void onResponse(BaseResponseResult response) {
                        hideTipDialog();
                        if (response != null && response.getSuccess() == true) {
                            mobileUserList.clear();
                            initData();
                            toast(context, "删除成功");
                        } else {
                            toast(context, "删除失败");
                        }
                    }
                }, map);

            }
        });
    }

    private void searchByName(String searchName) {
        mobileUserList.clear();
        page = 1;
        String url = Constants.OUTRT_NET + "/master_" + workshopId + "/m/workshop_user/" + workshopId + "/members?page=" + page + "&realName=" + searchName;
        OkHttpClientManager.getAsyn(context, url, new OkHttpClientManager.ResultCallback<ManagementMemberResult>() {

            @Override
            public void onError(Request request, Exception e) {
                loadingView.setVisibility(View.GONE);
                loadFailView.setVisibility(View.VISIBLE);
                ll_show.setVisibility(View.GONE);
            }

            @Override
            public void onResponse(ManagementMemberResult response) {
                loadingView.setVisibility(View.GONE);
                if (response != null && response.getResponseData() != null && response.getResponseData().getWorkshopUsers() != null && response.getResponseCode() != null && response.getResponseCode().equals("00")) {
                    member_count.setText("共有成员" + String.valueOf(response.getResponseData().getWorkshopUsers().size()) + "人");
                    if (isRefresh) {
                        mobileUserList.clear();
                        xRecyclerView.refreshComplete(true);
                    } else if (isLoadMore) {
                        xRecyclerView.loadMoreComplete(true);
                    }
                    if (response.getResponseData().getPaginator() != null && response.getResponseData().getPaginator().getHasNextPage()) {
                        xRecyclerView.setLoadingMoreEnabled(true);
                    } else {
                        xRecyclerView.setLoadingMoreEnabled(false);
                    }
                    mobileUserList.addAll(response.getResponseData().getWorkshopUsers());
                    adapter.notifyDataSetChanged();
                } else {
                    loadFailView.setVisibility(View.VISIBLE);
                    ll_show.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.bt_retry:
                initData();
                break;

        }
    }

    @Override
    public void onRefresh() {
        isRefresh = true;
        isLoadMore = false;
        page = 1;
        initData();
    }

    @Override
    public void onLoadMore() {
        isRefresh = false;
        isLoadMore = true;
        page += 1;
        initData();
    }

    public void initData() {
        String url = Constants.OUTRT_NET + "/master_" + workshopId + "/m/workshop_user/" + workshopId + "/members?page=" + page;
        OkHttpClientManager.getAsyn(context, url, new OkHttpClientManager.ResultCallback<ManagementMemberResult>() {

            @Override
            public void onError(Request request, Exception e) {
                loadingView.setVisibility(View.GONE);
                loadFailView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onResponse(ManagementMemberResult response) {
                loadingView.setVisibility(View.GONE);
                if (response != null && response.getResponseData() != null && response.getResponseData().getWorkshopUsers() != null && response.getResponseCode() != null && response.getResponseCode().equals("00")) {
                    member_count.setText("共有成员" + String.valueOf(response.getResponseData().getWorkshopUsers().size()) + "人");
                    if (isRefresh) {
                        mobileUserList.clear();
                        xRecyclerView.refreshComplete(true);
                    } else if (isLoadMore) {
                        xRecyclerView.loadMoreComplete(true);
                    }
                    if (response.getResponseData().getWorkshopUsers().size() == 0) {
                        warn_msg.setVisibility(View.VISIBLE);
                    }
                    if (response.getResponseData().getPaginator() != null && response.getResponseData().getPaginator().getHasNextPage()) {
                        xRecyclerView.setLoadingMoreEnabled(true);
                    } else {
                        xRecyclerView.setLoadingMoreEnabled(false);
                    }
                    mobileUserList.addAll(response.getResponseData().getWorkshopUsers());
                    adapter.notifyDataSetChanged();
                    loadFailView.setVisibility(View.GONE);
                    ll_show.setVisibility(View.VISIBLE);
                } else {
                    loadFailView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unsubscribe();
    }
}
