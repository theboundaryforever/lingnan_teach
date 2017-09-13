package com.haoyu.app.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.haoyu.app.dialog.PublicTipDialog;
import com.haoyu.app.lingnan.teacher.R;
import com.haoyu.app.rxBus.MessageEvent;
import com.haoyu.app.rxBus.RxBus;
import com.haoyu.app.utils.Constants;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public abstract class BaseFragment extends Fragment {
    public Context context;
    private PublicTipDialog publicTipDialog;
    protected Unbinder unbinder;
    public CompositeDisposable rxSubscriptions = new CompositeDisposable();
    private Disposable rxBusable;
    private Toast mToast;

    public abstract int createView();

    public abstract void initData();

    public void initView(View view) {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rxBusable = RxBus.getDefault().toObservable(MessageEvent.class).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<MessageEvent>() {
            @Override
            public void accept(MessageEvent event) throws Exception {
                obBusEvent(event);
            }
        });
    }

    public void obBusEvent(MessageEvent event) {

    }

    public void addSubscription(Disposable d) {
        if (d != null) {
            rxSubscriptions.add(d);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        preferences = context.getSharedPreferences(Constants.Prefs_user,
                Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(createView(), container, false);
        unbinder = ButterKnife.bind(this, rootView);
        initView(rootView);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initData();
        setListener();
    }

    public void onNetWorkError() {
        toast(getResources().getString(R.string.network_error));
    }

    public void setListener() {
    }

    public void toast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        } else {
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_LONG);
        }
        mToast.show();
    }

    public void cancelToast() {
        if (mToast != null) {
            mToast.cancel();
        }
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        cancelToast();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rxBusable != null && !rxBusable.isDisposed()) {
            rxBusable.dispose();
        }
        rxSubscriptions.dispose();
    }

    SharedPreferences preferences;

    public String getUserName() {
        return preferences.getString("userName", "");
    }

    public String getAvatar() {
        return preferences.getString("avatar", "");
    }

    public String getUserId() {
        return preferences.getString("id", "");
    }

    public String getRealName() {
        return preferences.getString("realName", "");
    }

    public String getDeptName() {
        return preferences.getString("deptName", "");
    }


    public void showTipDialog() {
        publicTipDialog = new PublicTipDialog(context);
        publicTipDialog.show();
    }

    public void hideTipDialog() {
        if (publicTipDialog != null) {
            publicTipDialog.dismiss();
        }
    }

    public <T extends View> T getView(View rootView, int id) {
        return (T) rootView.findViewById(id);
    }

    public void toastFullScreen(String content, boolean success) {
        Toast toast = new Toast(context);
        View view = LayoutInflater.from(context).inflate(
                R.layout.toast_publish_question, null);
        ImageView iv_result = view.findViewById(R.id.iv_result);
        TextView tv_result = view.findViewById(R.id.tv_result);
        if (success)
            iv_result.setImageResource(R.drawable.publish_success);
        else
            iv_result.setImageResource(R.drawable.publish_failure);
        tv_result.setText(content);
        toast.setView(view);
        toast.setGravity(Gravity.FILL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.show();
    }
}
