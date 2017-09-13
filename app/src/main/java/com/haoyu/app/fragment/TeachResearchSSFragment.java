package com.haoyu.app.fragment;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.TextView;

import com.haoyu.app.activity.TeachingResearchSSActivity;
import com.haoyu.app.adapter.TeachingResearchAdapter;
import com.haoyu.app.base.BaseFragment;
import com.haoyu.app.basehelper.BaseRecyclerAdapter;
import com.haoyu.app.dialog.CommentDialog;
import com.haoyu.app.entity.AttitudeMobileResult;
import com.haoyu.app.entity.DiscussEntity;
import com.haoyu.app.entity.DiscussListResult;
import com.haoyu.app.entity.Paginator;
import com.haoyu.app.entity.ReplyResult;
import com.haoyu.app.lingnan.teacher.R;
import com.haoyu.app.rxBus.MessageEvent;
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
 * 创建日期：2017/8/15 on 10:12
 * 描述:教研研说
 * 作者:马飞奔 Administrator
 */
public class TeachResearchSSFragment extends BaseFragment implements XRecyclerView.LoadingListener {
    @BindView(R.id.loadingView)
    LoadingView loadingView;
    @BindView(R.id.loadFailView)
    LoadFailView loadFailView;
    @BindView(R.id.xRecyclerView)
    XRecyclerView xRecyclerView;
    @BindView(R.id.emptyView)
    TextView emptyView;
    private List<DiscussEntity> mDatas = new ArrayList<>();
    private TeachingResearchAdapter adapter;
    private boolean isRefresh, isLoadMore;
    private int page = 1;
    private int selected = -1;

    @Override
    public int createView() {
        return R.layout.fragment_teach_research;
    }

    @Override
    public void initView(View view) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        xRecyclerView.setLayoutManager(layoutManager);
        adapter = new TeachingResearchAdapter(context, mDatas);
        xRecyclerView.setAdapter(adapter);
        xRecyclerView.setLoadingListener(this);
        emptyView.setText(getResources().getString(R.string.study_says_emptylist));
    }

    @Override
    public void initData() {
        String url = Constants.OUTRT_NET + "/m/discussion/cmts?discussionRelations[0].relation.id=cmts"
                + "&discussionRelations[0].relation.type=discussion" + "&orders=CREATE_TIME.DESC" + "&page=" + page;
        addSubscription(OkHttpClientManager.getAsyn(context, url, new OkHttpClientManager.ResultCallback<DiscussListResult>() {
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
            public void onResponse(DiscussListResult response) {
                loadingView.setVisibility(View.GONE);
                if (response != null && response.getResponseData() != null
                        && response.getResponseData().getmDiscussions() != null
                        && response.getResponseData().getmDiscussions().size() > 0) {
                    updateUI(response.getResponseData().getmDiscussions(), response.getResponseData().getPaginator());
                } else {
                    if (isRefresh) {
                        xRecyclerView.refreshComplete(true);
                    } else if (isLoadMore) {
                        xRecyclerView.loadMoreComplete(true);
                    } else {
                        xRecyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }));
    }

    private void updateUI(List<DiscussEntity> list, Paginator paginator) {
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
        adapter.setOnItemClickListener(new BaseRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseRecyclerAdapter adapter, BaseRecyclerAdapter.RecyclerHolder holder, View view, int position) {
                selected = position - 1;
                if (selected >= 0 && selected < mDatas.size()) {
                    String id = mDatas.get(selected).getId();
                    String uuid = mDatas.get(selected).getmDiscussionRelations().get(0).getId();
                    Intent intent = new Intent(context, TeachingResearchSSActivity.class);
                    intent.putExtra("id", id);
                    intent.putExtra("uuid", uuid);
                    startActivity(intent);
                }
            }
        });
        adapter.setRequestClickCallBack(new TeachingResearchAdapter.RequestClickCallBack() {
            @Override
            public void support(DiscussEntity entity, int position) {
                if (entity.isSupport())
                    toast("您已点赞过");
                else
                    createLike(position);
            }

            @Override
            public void comment(DiscussEntity entity, int position) {
                showInputDialog(position);
            }
        });
    }

    /**
     * 创建观点（点赞）
     *
     * @param position
     */
    private void createLike(final int position) {
        String url = Constants.OUTRT_NET + "/m/attitude";
        final String entityId = mDatas.get(position).getId();
        Map<String, String> map = new HashMap<>();
        map.put("attitude", "support");
        map.put("relation.id", entityId);
        map.put("relation.type", "discussion");
        addSubscription(OkHttpClientManager.postAsyn(context, url, new OkHttpClientManager.ResultCallback<AttitudeMobileResult>() {
            @Override
            public void onError(Request request, Exception exception) {
                onNetWorkError();
            }

            @Override
            public void onResponse(AttitudeMobileResult response) {
                if (response != null && response.getResponseCode() != null && response.getResponseCode().equals("00")) {
                    if (mDatas.get(position).getmDiscussionRelations() != null && mDatas.get(position).getmDiscussionRelations().size() > 0) {
                        int supportNum = mDatas.get(position).getmDiscussionRelations().get(0).getSupportNum() + 1;
                        mDatas.get(position).getmDiscussionRelations().get(0).setSupportNum(supportNum);
                    }
                    mDatas.get(position).setSupport(true);
                    adapter.notifyDataSetChanged();
                } else if (response != null && response.getResponseMsg() != null) {
                    mDatas.get(position).setSupport(true);
                    toast("您已点赞过");
                } else {
                    toast("点赞失败");
                }
            }
        }, map));
    }

    private void showInputDialog(final int position) {
        CommentDialog dialog = new CommentDialog(context);
        dialog.show();
        dialog.setSendCommentListener(new CommentDialog.OnSendCommentListener() {
            @Override
            public void sendComment(String content) {
                createComment(content, position);
            }
        });
    }

    private void createComment(String content, final int position) {
        DiscussEntity entity = mDatas.get(position);
        if (entity.getmDiscussionRelations() != null
                && entity.getmDiscussionRelations().size() > 0) {
            Map<String, String> map = new HashMap<>();
            map.put("content", content);
            map.put("discussionUser.discussionRelation.id", entity
                    .getmDiscussionRelations().get(0).getId());
            addSubscription(OkHttpClientManager.postAsyn(context, Constants.OUTRT_NET + "/m/discussion/post", new OkHttpClientManager.ResultCallback<ReplyResult>() {

                @Override
                public void onBefore(Request request) {
                    showTipDialog();
                }

                @Override
                public void onError(Request request, Exception exception) {
                    hideTipDialog();
                    onNetWorkError();
                }

                @Override
                public void onResponse(ReplyResult response) {
                    hideTipDialog();
                    if (response != null && response.getResponseData() != null) {
                        if (mDatas.get(position).getmDiscussionRelations() != null
                                && mDatas.get(position).getmDiscussionRelations().size() > 0) {
                            int replyNum = mDatas.get(position)
                                    .getmDiscussionRelations().get(0)
                                    .getReplyNum() + 1;
                            mDatas.get(position).getmDiscussionRelations().get(0).setReplyNum(replyNum);
                            adapter.notifyDataSetChanged();
                            toastFullScreen("发表成功", true);
                        }
                    }
                }
            }, map));
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

    @Override
    public void obBusEvent(MessageEvent event) {
        if (event.getAction().equals(Action.CREATE_STUDY_SAYS) && event.obj != null && event.obj instanceof DiscussEntity) {  //创建研说
            if (xRecyclerView.getVisibility() == View.GONE) {
                xRecyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
            DiscussEntity entity = (DiscussEntity) event.obj;
            mDatas.add(0, entity);
            adapter.notifyDataSetChanged();
        } else if (event.getAction().equals(Action.SUPPORT_STUDY_SAYS)) {   //研说点赞
            int supportNum = event.arg1;
            if (mDatas.get(selected).getmDiscussionRelations() != null
                    && mDatas.get(selected).getmDiscussionRelations().size() > 0) {
                mDatas.get(selected).getmDiscussionRelations().get(0).setSupportNum(supportNum);
                adapter.notifyDataSetChanged();
            }
        } else if (event.getAction().equals(Action.CREATE_MAIN_REPLY)) {    //创建研说评论
            if (mDatas.get(selected).getmDiscussionRelations() != null
                    && mDatas.get(selected).getmDiscussionRelations().size() > 0) {
                int replyNum = mDatas.get(selected).getmDiscussionRelations().get(0).getReplyNum() + 1;
                mDatas.get(selected).getmDiscussionRelations().get(0).setReplyNum(replyNum);
                adapter.notifyDataSetChanged();
            }
        } else if (event.getAction().equals(Action.DELETE_STUDY_SAYS)) {   //删除研说
            mDatas.remove(selected);
            adapter.notifyDataSetChanged();
        }
    }
}
