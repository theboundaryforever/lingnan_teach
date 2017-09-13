package com.haoyu.app.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.TextView;

import com.haoyu.app.activity.AppQuestionDetailActivity;
import com.haoyu.app.activity.AppQuestionEditActivity;
import com.haoyu.app.adapter.PageQuestionAdapter;
import com.haoyu.app.base.BaseFragment;
import com.haoyu.app.base.BaseResponseResult;
import com.haoyu.app.basehelper.BaseRecyclerAdapter;
import com.haoyu.app.entity.FAQsEntity;
import com.haoyu.app.entity.FAQsListResult;
import com.haoyu.app.entity.FollowMobileEntity;
import com.haoyu.app.entity.FollowMobileResult;
import com.haoyu.app.entity.Paginator;
import com.haoyu.app.lingnan.teacher.R;
import com.haoyu.app.rxBus.MessageEvent;
import com.haoyu.app.rxBus.RxBus;
import com.haoyu.app.utils.Action;
import com.haoyu.app.utils.Constants;
import com.haoyu.app.utils.OkHttpClientManager;
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
 * 创建日期：2017/8/16 on 11:07
 * 描述:
 * 作者:马飞奔 Administrator
 */
public class PageMyQuestionFragment extends BaseFragment implements XRecyclerView.LoadingListener {
    @BindView(R.id.loadingView)
    LoadingView loadingView;
    @BindView(R.id.loadFailView)
    LoadFailView loadFailView;
    @BindView(R.id.xRecyclerView)
    XRecyclerView xRecyclerView;
    @BindView(R.id.emptyView)
    TextView emptyView;
    private List<FAQsEntity> mDatas = new ArrayList<>();
    private PageQuestionAdapter adapter;
    private String type, relationId, relationType;
    private int page = 1;
    private boolean isRefresh, isLoadMore;

    @Override
    public int createView() {
        return R.layout.fragment_page_question_child;
    }

    @Override
    public void initView(View view) {
        Bundle bundle = getArguments();
        if (bundle != null) {
            type = bundle.getString("type");
            relationId = bundle.getString("relationId");
            relationType = bundle.getString("relationType");
        }
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        xRecyclerView.setLayoutManager(layoutManager);
        if (type != null && type.equals("course"))
            adapter = new PageQuestionAdapter(context, mDatas, 1);
        else
            adapter = new PageQuestionAdapter(context, mDatas, 2);
        xRecyclerView.setAdapter(adapter);
        xRecyclerView.setLoadingListener(this);
        emptyView.setText(getResources().getString(R.string.empty_ask));
    }

    @Override
    public void initData() {
        String url = Constants.OUTRT_NET + "/m/faq_question" + "?relation.id=" + relationId + "&relation.type" + relationType + "&page=" + page + "&orders=CREATE_TIME.DESC" + "&creator.id=" + getUserId();
        addSubscription(OkHttpClientManager.getAsyn(context, url, new OkHttpClientManager.ResultCallback<FAQsListResult>() {
            @Override
            public void onBefore(Request request) {
                if (isRefresh || isLoadMore) {
                    loadingView.setVisibility(View.GONE);
                } else {
                    loadingView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(Request request, Exception e) {
                loadingView.setVisibility(View.GONE);
                if (isRefresh) {
                    xRecyclerView.refreshComplete(false);
                } else if (isLoadMore) {
                    page -= 1;
                    xRecyclerView.loadMoreComplete(false);
                } else {
                    xRecyclerView.setVisibility(View.GONE);
                    loadFailView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onResponse(FAQsListResult response) {
                loadingView.setVisibility(View.GONE);
                if (response != null && response.getResponseData() != null && response.getResponseData().getQuestions() != null && response.getResponseData().getQuestions().size() > 0) {
                    updateUI(response.getResponseData().getQuestions(), response.getResponseData().getPaginator());
                } else {
                    if (isRefresh) {
                        xRecyclerView.refreshComplete(true);
                    } else if (isLoadMore) {
                        xRecyclerView.loadMoreComplete(true);
                    } else {
                        xRecyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }
                    xRecyclerView.setLoadingMoreEnabled(false);
                }
            }
        }));
    }

    private void updateUI(List<FAQsEntity> list, Paginator paginator) {
        if (xRecyclerView.getVisibility() != View.VISIBLE)
            xRecyclerView.setVisibility(View.VISIBLE);
        if (isRefresh) {
            mDatas.clear();
            xRecyclerView.refreshComplete(true);
        } else if (isLoadMore) {
            xRecyclerView.loadMoreComplete(true);
        }
        mDatas.addAll(list);
        adapter.notifyDataSetChanged();
        if (paginator != null && paginator.getHasNextPage()) {
            xRecyclerView.setLoadingMoreEnabled(true);
        } else {
            xRecyclerView.setLoadingMoreEnabled(false);
        }
    }

    @Override
    public void setListener() {
        adapter.setCollectCallBack(new PageQuestionAdapter.CollectCallBack() {
            @Override
            public void collect(int position, FAQsEntity entity) {
                collection(position);
            }

            @Override
            public void cancelCollect(int position, FAQsEntity entity) {
                cancelCollection(position);
            }
        });
        adapter.setAnswerCallBack(new PageQuestionAdapter.AnswerCallBack() {
            @Override
            public void answer(int position, FAQsEntity entity) {
                Intent intent = new Intent();
                intent.setClass(context, AppQuestionEditActivity.class);
                intent.putExtra("isAnswer", true);
                intent.putExtra("questionId", entity.getId());
                startActivity(intent);
            }
        });
        adapter.setOnItemClickListener(new BaseRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseRecyclerAdapter adapter, BaseRecyclerAdapter.RecyclerHolder holder, View view, int position) {
                if (position - 1 >= 0 && position - 1 < mDatas.size()) {
                    Intent intent = new Intent();
                    intent.setClass(getActivity(), AppQuestionDetailActivity.class);
                    intent.putExtra("type", type);
                    intent.putExtra("entity", mDatas.get(position - 1));
                    startActivity(intent);
                }
            }
        });
    }

    /**
     * 取消收藏
     *
     * @param position
     */
    private void cancelCollection(final int position) {
        FollowMobileEntity follow = mDatas.get(position).getFollow();
        if (follow != null) {
            String url = Constants.OUTRT_NET + "/m/follow/" + follow.getId();
            Map<String, String> map = new HashMap<>();
            map.put("_method", "delete");
            addSubscription(OkHttpClientManager.postAsyn(context, url, new OkHttpClientManager.ResultCallback<BaseResponseResult>() {
                @Override
                public void onError(Request request, Exception exception) {
                    onNetWorkError();
                }

                @Override
                public void onResponse(BaseResponseResult response) {
                    if (response != null && response.getResponseCode() != null && response.getResponseCode().equals("00")) {
                        mDatas.get(position).setFollow(null);
                        adapter.notifyDataSetChanged();
                        MessageEvent event = new MessageEvent();
                        event.action = Action.COLLECTION;
                        event.obj = mDatas.get(position);
                        RxBus.getDefault().post(event);
                    }
                }
            }, map));
        }
    }

    /**
     * 创建收藏
     *
     * @param position
     */
    private void collection(final int position) {
        String url = Constants.OUTRT_NET + "/m/follow";
        Map<String, String> map = new HashMap<>();
        map.put("followEntity.id", mDatas.get(position).getId());
        map.put("followEntity.type", "course_study_question");
        OkHttpClientManager.postAsyn(context, url, new OkHttpClientManager.ResultCallback<FollowMobileResult>() {
            @Override
            public void onError(Request request, Exception exception) {
                onNetWorkError();
            }

            @Override
            public void onResponse(FollowMobileResult response) {
                if (response != null && response.getResponseCode() != null && response.getResponseCode().equals("00")) {
                    FollowMobileEntity entity = response.getResponseData();
                    mDatas.get(position).setFollow(entity);
                    adapter.notifyDataSetChanged();
                    MessageEvent event = new MessageEvent();
                    event.action = Action.COLLECTION;
                    event.obj = mDatas.get(position);
                    RxBus.getDefault().post(event);
                } else {
                    if (response.getResponseMsg() != null) {
                        toast(response.getResponseMsg());
                    }
                }
            }
        }, map);
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

    @Override
    public void obBusEvent(MessageEvent event) {
        if (event.getAction().equals(Action.ALTER_COURSE_QUESTION) && event.obj != null && event.obj instanceof FAQsEntity) {
            FAQsEntity entity = (FAQsEntity) event.obj;
            if (mDatas.indexOf(entity) != -1) {
                int index = mDatas.indexOf(entity);
                mDatas.set(index, entity);
                adapter.notifyDataSetChanged();
            }
        } else if (event.getAction().equals(Action.CREATE_COURSE_QUESTION) && event.obj != null && event.obj instanceof FAQsEntity) {
            FAQsEntity entity = (FAQsEntity) event.obj;
            if (!xRecyclerView.isLoadingMoreEnabled()) {
                mDatas.add(entity);
                adapter.notifyDataSetChanged();
            }
            if (xRecyclerView.getVisibility() != View.VISIBLE)
                xRecyclerView.setVisibility(View.VISIBLE);
            if (emptyView.getVisibility() == View.VISIBLE)
                emptyView.setVisibility(View.GONE);
        } else if (event.getAction().equals(Action.COLLECTION) && event.obj != null && event.obj instanceof FAQsEntity) {
            FAQsEntity entity = (FAQsEntity) event.obj;
            if (mDatas.indexOf(entity) != -1) {
                int index = mDatas.indexOf(entity);
                mDatas.set(index, entity);
                adapter.notifyDataSetChanged();
            }
        } else if (event.getAction().equals(Action.DELETE_COURSE_QUESTION) && event.obj != null && event.obj instanceof FAQsEntity) {
            FAQsEntity entity = (FAQsEntity) event.obj;
            mDatas.remove(entity);
            adapter.notifyDataSetChanged();
            if (mDatas.size() == 0) {
                xRecyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        }
    }

}
