package com.haoyu.app.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.haoyu.app.adapter.FileSubmitAdapter;
import com.haoyu.app.base.BaseActivity;
import com.haoyu.app.base.BaseResponseResult;
import com.haoyu.app.dialog.FileUploadDialog;
import com.haoyu.app.dialog.MaterialDialog;
import com.haoyu.app.entity.FileUploadResult;
import com.haoyu.app.entity.MFileInfo;
import com.haoyu.app.filePicker.LFilePicker;
import com.haoyu.app.lingnan.teacher.R;
import com.haoyu.app.rxBus.MessageEvent;
import com.haoyu.app.rxBus.RxBus;
import com.haoyu.app.utils.Action;
import com.haoyu.app.utils.Constants;
import com.haoyu.app.utils.MPermissionUtils;
import com.haoyu.app.utils.OkHttpClientManager;
import com.haoyu.app.view.AppToolBar;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import okhttp3.Request;


/**
 * Created by acer1 on 2017/2/13.
 * 学员提交作业
 */
public class AppSubmitAssignmentActivity extends BaseActivity implements View.OnClickListener {
    private AppSubmitAssignmentActivity context = this;
    @BindView(R.id.toolBar)
    AppToolBar toolBar;
    private String aid;
    private String auid;
    private String uid;
    private FileSubmitAdapter adapter;
    //上传文件后返回的对象
    private List<MFileInfo> idsList = new ArrayList<>();
    //要上传的文件对象
    private List<MFileInfo> filePathList = new ArrayList<>();
    private List<String> pathList = new ArrayList<>();
    @BindView(R.id.fileList)
    RecyclerView recyclerView;
    @BindView(R.id.iv_add)
    ImageView iv_add;
    private String[] types;
    private String fileType;

    @Override
    public int setLayoutResID() {
        return R.layout.test_assignment;
    }

    @Override
    public void initView() {
        aid = getIntent().getStringExtra("aid");
        auid = getIntent().getStringExtra("auid");
        uid = getIntent().getStringExtra("uid");
        fileType = getIntent().getStringExtra("fileType");
        types = fileType.split(",");
        adapter = new FileSubmitAdapter(filePathList);
        LinearLayoutManager manager = new LinearLayoutManager(context);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

    }


    @Override
    public void setListener() {
        iv_add.setOnClickListener(context);
        toolBar.setOnLeftClickListener(new AppToolBar.OnLeftClickListener() {
            @Override
            public void onLeftClick(View view) {
                finish();
            }
        });
        toolBar.setOnRightClickListener(new AppToolBar.OnRightClickListener() {
            @Override
            public void onRightClick(View view) {
                if (filePathList.size() > 0) {
                    for (int i = 0; i < filePathList.size(); i++) {
                        initData(i);
                    }
                } else {
                    toast(context, "请选择上传的文件");
                }
            }
        });
        adapter.setDisposeCallBack(new FileSubmitAdapter.onDisposeCallBack() {
            @Override
            public void onDelete(int position) {
                filePathList.remove(position);
                pathList.remove(position);
                adapter.notifyDataSetChanged();
            }
        });

    }


    private FileUploadDialog fileDialog;

    private void initDialog(File mFileInfo) {
        dismissDialog();
        fileDialog = new FileUploadDialog(context, mFileInfo.getName(), "正在上传");
        fileDialog.show();
        fileDialog.setCancelListener(new FileUploadDialog.CancelListener() {
            @Override
            public void cancel() {
                fileDialog.dismiss();
            }
        });
    }

    private void dismissDialog() {
        if (fileDialog != null) {
            fileDialog.dismiss();
            fileDialog = null;
        }
    }

    //上传临时文件

    private void initData(final int index) {
        String url = Constants.OUTRT_NET + "/m/file/uploadTemp";
        File file = new File(filePathList.get(index).getUrl());
        try {
            OkHttpClientManager.postAsyn(context, url, new OkHttpClientManager.ResultCallback<FileUploadResult>() {
                @Override
                public void onBefore(Request request) {
                    initDialog(new File(filePathList.get(index).getFileName()));
                }

                @Override
                public void onError(Request request, Exception e) {
                    dismissDialog();
                }

                @Override
                public void onResponse(FileUploadResult response) {
                    dismissDialog();
                    if (response != null && response.getSuccess()) {

                        MFileInfo entity = new MFileInfo();
                        if (response.getResponseData() != null && response.getResponseData().getId() != null) {
                            if (response.getResponseData().getId() != null) {
                                entity.setId(response.getResponseData().getId());
                            }
                            if (response.getResponseData().getFileName() != null) {
                                entity.setFileName(response.getResponseData().getFileName());
                            }
                            if (response.getResponseData().getUrl() != null) {
                                entity.setUrl(response.getResponseData().getUrl());
                            }
                            idsList.add(entity);
                            if (index == filePathList.size() - 1) {
                                dismissDialog();
                                commitAssignment();
                            }

                        }

                    }
                }
            }, file, file.getName(), new OkHttpClientManager.ProgressListener() {
                @Override
                public void onProgress(long totalBytes, long remainingBytes, boolean done, File file) {
                    if (totalBytes > 0) {
                        int percent = (int) (100 - remainingBytes * 100 / totalBytes);
                        Message msg = handler.obtainMessage();
                        msg.what = 2;
                        msg.obj = percent;
                        msg.arg1 = (int) remainingBytes;
                        msg.arg2 = (int) totalBytes;
                        handler.sendMessage(msg);
                    }


                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            adapter.notifyDataSetChanged();
            switch (msg.what) {
                case 1:
                    adapter.notifyDataSetChanged();
                    break;
                case 2:
                    int totalSize = msg.arg2;
                    int remainBytes = msg.arg1;
                    fileDialog.setUploadProgressBar(totalSize, remainBytes);
                    fileDialog.setUploadText(totalSize, remainBytes);
                    break;
            }

        }
    };


    private void addFilePath(String filePath) {
        boolean flag = false;
        for (int i = 0; i < types.length; i++) {
            if (filePath.endsWith(types[i])) {
                flag = true;
                break;
            }
        }
        if (flag) {
            if (pathList.contains(filePath)) {
                toast(context, "您已经添加过该文件了！");
            } else {
                MFileInfo entity = new MFileInfo();
                File file = new File(filePath);
                entity.setFileSize(file.length());
                entity.setFileName(file.getName());
                entity.setUrl(filePath);
                filePathList.add(entity);
                adapter.notifyDataSetChanged();
            }
        } else {
            toast(context, "该文件格式不能添加");
        }
    }


    //提交作业
    private void commitAssignment() {
        String
                url = Constants.OUTRT_NET + "/" + aid + "/study/unique_uid_" + uid + "/m/assignment/user/" + auid;


        Map<String, String> map = new HashMap<>();
        map.put("state", "commit");
        map.put("_method", "put");
        for (int i = 0; i < idsList.size(); i++) {
            MFileInfo fileInfo = idsList.get(i);
            map.put("fileInfos[" + i + "].id", fileInfo.getId());
            map.put("fileInfos[" + i + "].url", fileInfo.getUrl());
            map.put("fileInfos[" + i + "].fileName", fileInfo.getFileName());
        }
        OkHttpClientManager.postAsyn(context, url, new OkHttpClientManager.ResultCallback<BaseResponseResult>() {
            @Override
            public void onError(Request request, Exception e) {


            }

            @Override
            public void onResponse(BaseResponseResult response) {
                if (response != null && response.getSuccess() != null && response.getSuccess()) {
                    toast(context, "提交成功");
                    MessageEvent event = new MessageEvent();
                    event.setAction(Action.SUBMIT_COURSE_ASSIGNMENT);
                    RxBus.getDefault().post(event);
                    finish();
                }
            }
        }, map);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.iv_add:
                openFilePicker();
                break;
        }
    }

    private void openFilePicker() {
        new LFilePicker()
                .withActivity(context)
                .withRequestCode(1)
                .withMutilyMode(false)
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            List<String> list = data.getStringArrayListExtra(RESULT_INFO);
            if (list != null && list.size() > 0) {
                String filePath = list.get(0);
                addFilePath(filePath);
                pathList.add(filePath);
            }
        }
    }

    private void showError() {
        MaterialDialog dialog = new MaterialDialog(context);
        dialog.setTitle("温馨提示");
        dialog.setMessage("当前应用缺少SD卡权限，暂时无法访问手机文件。如若需要，请单击【确定】按钮前往设置中心进行权限授权。");
        dialog.setNegativeButton("取消", new MaterialDialog.ButtonClickListener() {
            @Override
            public void onClick(View v, AlertDialog dialog) {

            }
        });
        dialog.setPositiveButton("设置", new MaterialDialog.ButtonClickListener() {
            @Override
            public void onClick(View v, AlertDialog dialog) {
                MPermissionUtils.startAppSettings(context);
            }
        });
        dialog.setNegativeTextColor(ContextCompat.getColor(context, R.color.gray_text));
        dialog.setPositiveTextColor(ContextCompat.getColor(context, R.color.defaultColor));
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
