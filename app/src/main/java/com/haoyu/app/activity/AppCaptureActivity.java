package com.haoyu.app.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.haoyu.app.base.BaseActivity;
import com.haoyu.app.lingnan.teacher.R;
import com.haoyu.app.view.AppToolBar;
import com.uuzuche.lib_zxing.activity.CaptureFragment;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import butterknife.BindView;

/**
 * 创建日期：2017/2/21 on 13:35
 * 描述:  自定义扫一扫界面
 * 定制化显示扫描界面
 * 作者:马飞奔 Administrator
 */
public class AppCaptureActivity extends BaseActivity {
    private AppCaptureActivity context = this;
    @BindView(R.id.toolBar)
    AppToolBar toolBar;
    @BindView(R.id.ll_noCamera)
    LinearLayout ll_noCamera;
    @BindView(R.id.tv_tips)
    TextView tv_tips;
    @BindView(R.id.bt_settings)
    Button bt_settings;
    @BindView(R.id.fl_my_container)
    FrameLayout fl_my_container;
    private final static int CAMERA_OK = 2;
    private boolean requestCamera;

    @Override
    protected void onRestart() {
        super.onRestart();
        if (hasCameraPermission() && !requestCamera) {
            requestCamera();
        }
    }

    @Override
    public int setLayoutResID() {
        return R.layout.activity_app_capture;
    }

    @Override
    public void initView() {
        if (hasCameraPermission()) {  //如果申请了相机权限则显示摄像头
            requestCamera();
        } else {        //否则申请相机权限
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.CAMERA}, CAMERA_OK);
        }
    }

    private boolean hasCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_OK && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestCamera();
        } else {
            ll_noCamera.setVisibility(View.VISIBLE);
            tv_tips.setText("相机权限已被禁止，请到【设置】——>【应用管理】——>" + getResources().getString(R.string.app_name) + "——>【权限】选择打开【相机】。");
        }
    }

    private void requestCamera() {
        requestCamera = true;
        ll_noCamera.setVisibility(View.GONE);
        fl_my_container.setVisibility(View.VISIBLE);
        CaptureFragment captureFragment = new CaptureFragment();
        // 为二维码扫描界面设置定制化界面
        CodeUtils.setFragmentArgs(captureFragment, R.layout.app_capture_camera);
        captureFragment.setAnalyzeCallback(analyzeCallback);
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_my_container, captureFragment).commit();
    }

    @Override
    public void setListener() {
        toolBar.setOnLeftClickListener(new AppToolBar.OnLeftClickListener() {
            @Override
            public void onLeftClick(View view) {
                finish();
            }
        });
        bt_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        });
    }

    private void openSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 二维码解析回调函数
     */
    CodeUtils.AnalyzeCallback analyzeCallback = new CodeUtils.AnalyzeCallback() {
        @Override
        public void onAnalyzeSuccess(Bitmap mBitmap, String result) {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt(CodeUtils.RESULT_TYPE, CodeUtils.RESULT_SUCCESS);
            bundle.putString(CodeUtils.RESULT_STRING, result);
            resultIntent.putExtras(bundle);
            context.setResult(RESULT_OK, resultIntent);
            context.finish();
        }

        @Override
        public void onAnalyzeFailed() {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt(CodeUtils.RESULT_TYPE, CodeUtils.RESULT_FAILED);
            bundle.putString(CodeUtils.RESULT_STRING, "");
            resultIntent.putExtras(bundle);
            context.setResult(RESULT_OK, resultIntent);
            context.finish();
        }
    };
}
