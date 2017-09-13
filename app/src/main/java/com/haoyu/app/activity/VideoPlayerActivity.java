package com.haoyu.app.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.haoyu.app.adapter.DiscussFileAdapter2;
import com.haoyu.app.base.BaseActivity;
import com.haoyu.app.basehelper.BaseRecyclerAdapter;
import com.haoyu.app.entity.MFileInfo;
import com.haoyu.app.entity.VideoMobileEntity;
import com.haoyu.app.lingnan.teacher.R;
import com.haoyu.app.rxBus.MessageEvent;
import com.haoyu.app.service.NetSpeedService;
import com.haoyu.app.utils.Constants;
import com.haoyu.app.utils.MyUtils;
import com.haoyu.app.utils.OkHttpClientManager;
import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLMediaPlayer;
import com.pili.pldroid.player.widget.PLVideoTextureView;
import com.pili.pldroid.player.widget.PLVideoView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import okhttp3.Request;

public class VideoPlayerActivity extends BaseActivity implements View.OnClickListener/*, OnFileDownloadStatusListener */ {
    private VideoPlayerActivity context = this;
    @BindView(R.id.videoView)
    PLVideoTextureView mVideoView;
    private final int VIDEO_HIDECENTERBOX = 1;// 视屏的亮度
    private final int VIDEO_FORWARD = 2;// 滑动屏幕快进
    private final int VIDEO_SEEKBARFORWARD = 3;// 拖动进度条快进
    private final int VIDEO_HIDECONTROLLBAR = 4;// 隐藏控制栏
    public final int UPDATE_SEEKBAR = 0;
    public final int HIDDEN_SEEKBAR = 1;
    public final int VIDEO_WARN_MESSAGE = 5;//网络消息提
    @BindView(R.id.linear_centercontroll)
    RelativeLayout linear_centercontroll;
    @BindView(R.id.bottomControll)
    RelativeLayout bottomControll;
    @BindView(R.id.topControll)
    RelativeLayout topControll;
    @BindView(R.id.warn_controll)
    LinearLayout warnControl;// 当发生错误时的提示
    @BindView(R.id.video_title)
    TextView videoTitle;
    @BindView(R.id.currentTime)
    TextView currentTime;
    @BindView(R.id.totalTime)
    TextView totalTime;
    @BindView(R.id.iv_back)
    ImageView iv_back;
    @BindView(R.id.video_play)
    ImageView videoPlay;
    @BindView(R.id.video_centerpause)
    ImageView videoCenterPause;
    @BindView(R.id.video_seekbar)
    SeekBar videoSeekBar;
    @BindView(R.id.video_framelayout)
    FrameLayout framelayout;
    @BindView(R.id.center_content)
    TextView center_content;
    @BindView(R.id.loadingbar)
    LinearLayout loadingView;
    @BindView(R.id.my_video_zhangjie)
    TextView mRead;//课前阅读
    private boolean isLoading = false; //判断当前是否正在缓冲加载
    private boolean mPause = false; //判断当前是否暂停了

    private long mPauseStartTime = 0;
    private long mPausedTime = 0;
    private int defaultTime = 5 * 1000;
    private int screenWidthPixels;  //获取屏幕的宽度像素
    GestureDetector gestureDetector;
    String mVideoPath;
    private boolean isDownload = false;
    @BindView(R.id.warn_continue)
    TextView warnContinue;
    @BindView(R.id.warn_content)
    TextView warnContent;
    @BindView(R.id.video_lock)
    ImageView mVideoLock;//视频锁，可以让屏幕不跟随旋转
    private MyOrientationListener myOrientationListener;
    private List<MFileInfo> mFileInfoList = new ArrayList<>();
    boolean isLocal = false;//判断播放的事本地视频还是网络视频
    private String summary;
    private String videoId;//视频id
    private String activityId;//活动Id

    private boolean isReCheck = false;
    private boolean running;

    private MyHandler videoHandler = new MyHandler(this);

    private class MyHandler extends Handler {
        private WeakReference<Context> reference;

        public MyHandler(Context context) {
            reference = new WeakReference<>(context);

        }

        @Override

        public void handleMessage(Message msg) {
            VideoPlayerActivity context = (VideoPlayerActivity) reference.get();
            switch (msg.what) {
                case VIDEO_HIDECENTERBOX:
                    if (context != null) {
                        hideCenterBox();
                        if (isLoading) {
                            hideVideoCenterPause();
                            showLoading();
                        } else {
                            hideLoading();
                            hideVideoCenterPause();
                            if (mVideoView != null) {
                                mVideoView.start();
                            }

                        }
                    }

                    break;
                case VIDEO_FORWARD:
                    if (context != null) {
                        hideLoading();
                        hideVideoCenterPause();
                        if (newPosition != 0) {
                            if (mVideoView != null) {
                                mVideoView.seekTo(newPosition);
                            }
                        }

                    }

                    break;
                case VIDEO_SEEKBARFORWARD:
                    if (context != null) {
                        if (isDownload) {
                            if (mVideoView != null && mVideoView.getCurrentPosition() == 0) {
                                mVideoView.start();
                                return;
                            }
                            hideLoading();
                            hideVideoCenterPause();
                            showCenterBox();
                            center_content.setText(MyUtils.generateTime(mVideoView
                                    .getCurrentPosition()));
                            Drawable drawable;
                            // / 这一步必须要做,否则不会显示.
                            if (seekbarEndTrackPosition > seekbarStartTrackPosition)
                                drawable = ContextCompat.getDrawable(null,
                                        R.drawable.video_btn_fast_forword);
                            else
                                drawable = ContextCompat.getDrawable(null,
                                        R.drawable.video_btn_back_forword);
                            drawable.setBounds(0, 0, drawable.getMinimumWidth(),
                                    drawable.getMinimumHeight());
                            center_content.setCompoundDrawables(drawable, null, null,
                                    null);
                            videoHandler.removeMessages(VIDEO_HIDECENTERBOX);
                            videoHandler.removeMessages(VIDEO_SEEKBARFORWARD);
                            videoHandler.sendEmptyMessageDelayed(VIDEO_HIDECENTERBOX,
                                    1 * 500);
                            setVideoProgress(mVideoProgress);
                            seekbarEndTrackPosition = -1;
                            seekbarStartTrackPosition = -1;
                        }
                    }
                    break;
                case VIDEO_HIDECONTROLLBAR:
                    if (context != null) {
                        hideControllBar();
                    }

                    break;
                case UPDATE_SEEKBAR:
                    if (context != null) {
                        setVideoProgress(0);
                    }

                    break;
                case VIDEO_WARN_MESSAGE:
                    if (context != null) {
                        if (mVideoView.getDuration() == -1) {
                            if (videoPosition > 0) {
                                if (!isReCheck) {
                                    isReCheck = true;
                                    showLoading();
                                    hideWarnControll();
                                    videoViewStart();
                                    mVideoView.seekTo(videoPosition);
                                    mVideoView.start();
                                }
                            } else {

                                showWarnControll();
                                warnContent.setText("该视频暂时无法播放");
                                mVideoView.pause();
                                hideVideoCenterPause();
                                warnContinue.setVisibility(View.GONE);


                            }
                        } else if ((okWifi && mVideoView.getDuration() != -1) || (isNet && mVideoView.getDuration() != -1)) {
                            if (mVideoView != null && mVideoView.getCurrentPosition() == 0) {
                                if (!isReCheck) {
                                    isReCheck = true;
                                    mVideoView.start();
                                }

                            } else {
                                if (mVideoView.isPlaying()) {
                                    mVideoView.seekTo(videoPosition);
                                    mVideoView.start();
                                } else {
                                    mVideoView.pause();
                                }
                            }
                        }
                    }

                    break;
                default:
                    break;
            }
        }
    }

    private long videoPosition;
    private String type, workshopId;

    @Override
    public int setLayoutResID() {
        return R.layout.activity_videoplayer;
    }

    @Override
    public void initView() {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        videoId = getIntent().getStringExtra("videoId");
        activityId = getIntent().getStringExtra("activityId");
        type = getIntent().getStringExtra("type");
        running = getIntent().getBooleanExtra("running", false);
        VideoMobileEntity entity = (VideoMobileEntity) getIntent().getSerializableExtra("attach");
        workshopId = getIntent().getStringExtra("workshopId");

        if (entity != null && entity.getAttchFiles() != null && entity.getAttchFiles().size() > 0) {
            mFileInfoList.addAll(entity.getAttchFiles());
        }
        //
        mVideoView.setBufferingIndicator(loadingView);
        linear_centercontroll.getBackground().setAlpha(80);
        screenWidthPixels = MyUtils.screenWidthPixels(this);
        topControll.getBackground().setAlpha(80);
        bottomControll.getBackground().setAlpha(80);
        mRead = (TextView) findViewById(R.id.my_video_zhangjie);
        videoSeekBar.setEnabled(true);
        audioManager = (AudioManager) this
                .getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        updateVideoCatch();
        //开启播放
        initContent();
        MyUtils.Land(this);//取消手机的状态栏
        MyUtils.hideBottomUIMenu(this);//如果手机又虚拟按键则隐藏改虚拟按键
        myOrientationListener = new MyOrientationListener(this);//设置手机屏幕旋转监听
        myOrientationListener.enable();
        isLocal = !(mVideoPath != null && (mVideoPath.startsWith("http://") || mVideoPath.startsWith("https://")));
        startService(new Intent(context, NetSpeedService.class));
        registRxBus();
    }

    @Override
    public void setListener() {
        mRead.setOnClickListener(this);
        mVideoLock.setOnClickListener(this);
        warnContinue.setOnClickListener(this);
        framelayout.setClickable(true);
        iv_back.setOnClickListener(this);
        videoCenterPause.setOnClickListener(this);
        videoPlay.setOnClickListener(mStartBtnListener);
        videoPlay.setImageResource(R.drawable.zanting);
        videoSeekBar.setOnSeekBarChangeListener(mSeekBarListener);
    }

    private String netType;//手机的网络状态,3表示是wifi ，0表示当前没有网络，4表示当前是手机流量2G/3G/4G
    private boolean isWarn = false;
    private boolean okWifi = false;

    @Override
    public void obBusEvent(MessageEvent event) {
        String action = event.getAction();
        if (action.equals(Constants.speedAction)) {
            if (mVideoView != null && mVideoView.getCurrentPosition() > 0) {
                videoPosition = mVideoView.getCurrentPosition();
            }
            if (!isLocal) {
                String obj = event.getObj().toString();
                netType = obj;
                if (obj.equals("0") || obj.equals("1")) {
                    //没有网络
                    mVideoView.pause();
                    if (!isWarn) {
                        isWarn = true;
                        okWifi = false;
                        isNet = false;
                        showWarnControll();
                        hideVideoCenterPause();
                        hideCenterBox();
                        warnContent.setText("当前没有网络，\n请开启手机网络");
                    }

                } else if (obj.equals("3")) {
                    //wifi
                    okWifi = true;
                    if (mVideoView != null && mVideoView.getDuration() != -1) {
                        hideWarnControll();
                    }
                    isWarn = false;
                    if (okWifi && mVideoView.getDuration() != -1) {
                        if (mVideoView.isPlaying()) {
                            mVideoView.start();
                        }
                        okWifi = false;
                    }
                    if (videoPosition == 0 && mVideoView.getDuration() != -1) {
                        mVideoView.start();
                    }
                    if (mVideoView.getDuration() == -1 && videoPosition > 0) {
                        if (!isReCheck) {
                            isReCheck = true;
                            videoViewStart();
                        }
                    }
                } else {
                    //移动流量
                    if (!isNet) {
                        isWarn = false;
                        mVideoView.pause();
                        hideVideoCenterPause();
                        hideCenterBox();
                        showWarnControll();
                        warnContent.setText("当前是移动流量，\n您要继续播放吗");
                        if (mVideoView.getDuration() == -1 && videoPosition > 0) {
                            videoViewStart();
                        }
                    } else {
                        okWifi = false;
                        isWarn = false;
                        isNet = true;
                        hideLoading();
                        hideCenterBox();
                        hideVideoCenterPause();
                        hideWarnControll();
                        mVideoView.start();
                        if (mVideoView.getDuration() == -1 && videoPosition > 0) {
                            if (!isReCheck) {
                                isReCheck = true;
                                videoViewStart();
                            }
                        }
                    }
                }
            }

        }
    }

    private boolean isNet;//判断是否非wifi下播放
    private PopupWindow popWindow;

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mVideoView != null) {
            mVideoView.start();
            hideVideoCenterPause();
            videoPlay.setImageResource(R.drawable.zanting);
        }
    }

    private void showVideoCenterPause() {
        if (videoCenterPause != null) {
            videoCenterPause.setVisibility(View.VISIBLE);
        }

    }

    private void hideVideoCenterPause() {
        if (videoCenterPause != null) {
            videoCenterPause.setVisibility(View.GONE);
        }
    }

    private void videoPause() {
        mVideoView.pause();
    }

    private void showWarnControll() {
        warnControl.setVisibility(View.VISIBLE);
    }

    private void hideWarnControll() {
        warnControl.setVisibility(View.GONE);
    }

    private void videoStart() {
        showLoading();
        mVideoView.setVideoPath(mVideoPath);
        mVideoView.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mVideoView.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mVideoView != null) {
            updateVideoTime(length);
        }
        mVideoView.stopPlayback();
        videoHandler.removeCallbacksAndMessages(null);
        stopService(new Intent(context, NetSpeedService.class));
        myOrientationListener.disable();
        unsubscribe();
    }


    private void initContent() {
        mVideoPath = getIntent().getStringExtra("videoUrl");
        summary = getIntent().getStringExtra("summary");
        String fileName = getIntent().getStringExtra("fileName");
        String activityTitle = getIntent().getStringExtra("activityTitle");
        if (fileName != null) {
            videoTitle.setText(fileName);
        } else {
            videoTitle.setText(activityTitle);
        }

        AVOptions options = new AVOptions();
        // 设置链接超时时间
        options.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 20 * 1000);
        options.setInteger(AVOptions.KEY_GET_AV_FRAME_TIMEOUT, 20 * 1000);
        int codec = getIntent().getIntExtra("mediaCodec", 0);
        options.setInteger(AVOptions.KEY_MEDIACODEC, codec);
        options.setInteger(AVOptions.KEY_START_ON_PREPARED, 0);
        mVideoView.setAVOptions(options);
        mVideoView.setOnInfoListener(mOnInfoListener);
        mVideoView.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        mVideoView.setOnCompletionListener(mOnCompletionListener);
        mVideoView.setOnSeekCompleteListener(mOnSeekCompleteListener);
        mVideoView.setOnErrorListener(mOnErrorListener);
        mVideoView.setOnPreparedListener(mOnPreparedListener);
        mVideoView.setVideoPath(mVideoPath);
        mVideoView.start();
        mVideoView.setDisplayAspectRatio(PLVideoView.ASPECT_RATIO_PAVED_PARENT);
        // 手势监听
        gestureDetector = new GestureDetector(this, new PlayerGestureListener());
        framelayout.setClickable(true);
        framelayout.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                // TODO Auto-generated method stub
                if (gestureDetector.onTouchEvent(motionEvent))
                    return true;
                // 处理手势结束
                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_UP:
                        endGesture();
                        break;
                }
                return false;
            }
        });
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    //课前指导
    PopupWindow window;
    ImageView popClose;//关闭课前指导弹出页

    private void showPopWindow() {
        View view = LayoutInflater.from(this).inflate(R.layout.video_courseread_guide, null);
        View parentView = LayoutInflater.from(this).inflate(R.layout.activity_videoplayer, null);
        popClose = getView(view, R.id.pop_close);
        TextView read_guide_content = getView(view, R.id.read_guide_content);
        TextView read_guide = getView(view, R.id.read_guide);
        RecyclerView recyclerView = getView(view, R.id.recyclerView);
        if (summary != null) {
            read_guide_content.setText(summary);
        } else {
            read_guide_content.setText("暂无内容");
        }

        if (mFileInfoList.size() > 0) {
            LinearLayoutManager manager = new LinearLayoutManager(context);
            manager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(manager);
            DiscussFileAdapter2 discussFileAdapter = new DiscussFileAdapter2(context, mFileInfoList);
            recyclerView.setAdapter(discussFileAdapter);
            discussFileAdapter.setOnItemClickListener(new BaseRecyclerAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(BaseRecyclerAdapter adapter, BaseRecyclerAdapter.RecyclerHolder holder, View view, int position) {
                    MFileInfo mFileInfo = mFileInfoList.get(position);
                    String url = mFileInfo.getUrl();
                    if (url == null)
                        toast(context, "文件链接不存在");
                    else {
                        Intent intent = new Intent(context, MFileInfoActivity.class);
                        intent.putExtra("fileInfo", mFileInfo);
                        startActivity(intent);
                    /*downloadFile(mFileInfo);*/
                    }
                }
            });
        } else {
            recyclerView.setVisibility(View.GONE);
        }
        if (window == null) {
            window = new PopupWindow(view, MyUtils.getWidth(this) * 3 / 5, MyUtils.getHeight(this));
        }
        window.setOutsideTouchable(true);
        window.setBackgroundDrawable(new BitmapDrawable());
        popClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (window != null && window.isShowing()) {
                    window.dismiss();
                    window = null;
                }
            }
        });
        window.showAtLocation(parentView, Gravity.RIGHT, 0, 0);
    }

    //显示弹出内容
    private void showToastTips(String toast) {
        toast(context, toast);
    }

    //视频错误内容
    private PLMediaPlayer.OnErrorListener mOnErrorListener = new PLMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(PLMediaPlayer mp, int errorCode) {

            String message;
            switch (errorCode) {
                case PLMediaPlayer.ERROR_CODE_INVALID_URI:
                    message = "该视频暂时无法播放";
                    showToastTips(message);
                    mVideoView.pause();
                    videoHandler.sendEmptyMessageDelayed(VIDEO_WARN_MESSAGE, 2000);
                    break;
                case PLMediaPlayer.ERROR_CODE_404_NOT_FOUND:
                    // showToastTips("404 resource not found !");
                    message = "该视频暂时无法播放";
                    showToastTips(message);
                    videoHandler.sendEmptyMessageDelayed(VIDEO_WARN_MESSAGE, 2000);
                    break;
                case PLMediaPlayer.ERROR_CODE_CONNECTION_REFUSED:
                    //   showToastTips("Connection refused !");
                    message = "该视频暂时无法播放";
                    showToastTips(message);
                    videoHandler.sendEmptyMessageDelayed(VIDEO_WARN_MESSAGE, 2000);
                    break;
                case PLMediaPlayer.ERROR_CODE_CONNECTION_TIMEOUT:
                    if (!isWarn) {
                        showLoading();
                        videoHandler.sendEmptyMessageDelayed(VIDEO_WARN_MESSAGE, 2000);
                    }

                    break;
                case PLMediaPlayer.ERROR_CODE_EMPTY_PLAYLIST:
                    showToastTips("Empty playlist !");
                    showWarn();
                    break;
                case PLMediaPlayer.ERROR_CODE_STREAM_DISCONNECTED:
                    showToastTips("Stream disconnected !");
                    videoHandler.sendEmptyMessageDelayed(VIDEO_WARN_MESSAGE, 2000);
                    break;
                case PLMediaPlayer.ERROR_CODE_IO_ERROR:
                    if (!isWarn) {
                        showToastTips("该视频暂时无法播放！");
                        hideLoading();
                        videoHandler.sendEmptyMessageDelayed(VIDEO_WARN_MESSAGE, 2000);
                    }
                    break;
                case PLMediaPlayer.ERROR_CODE_UNAUTHORIZED:
                    showToastTips("Unauthorized Error !");
                    break;
                case PLMediaPlayer.ERROR_CODE_PREPARE_TIMEOUT:
                    showToastTips("Prepare timeout !");
                    videoHandler.sendEmptyMessageDelayed(VIDEO_WARN_MESSAGE, 2000);
                    break;
                case PLMediaPlayer.ERROR_CODE_READ_FRAME_TIMEOUT:
                    // showToastTips("Read frame timeout !");
                    message = "该视频暂时无法播放";
                    showToastTips(message);
                    videoHandler.sendEmptyMessageDelayed(VIDEO_WARN_MESSAGE, 2000);
                    break;
                case PLMediaPlayer.MEDIA_ERROR_UNKNOWN:
                    message = "该视频暂时无法播放";
                    showToastTips(message);
                    videoHandler.sendEmptyMessageDelayed(VIDEO_WARN_MESSAGE, 2000);
                    break;
                default:
                    message = "该视频暂时无法播放,请稍后重试";
                    if (!isWarn) {
                        warnContent.setText("当前没有网络，\n请开启手机网络");
                    } else {
                        showToastTips(message);
                    }
                    break;
            }
            showControllBar();
            hideVideoCenterPause();
            return true;
        }
    };

    private void videoViewStart() {
        if (mVideoView != null) {
            showLoading();
            mVideoView.setVideoPath(mVideoPath);
            mVideoView.start();
            mVideoView.seekTo(videoPosition);
        }
    }

    private void showWarn() {
        warnControl.setVisibility(View.VISIBLE);
        warnContent.setText("该视频暂时无法播放！");
    }

    private PLMediaPlayer.OnCompletionListener mOnCompletionListener = new PLMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(PLMediaPlayer plMediaPlayer) {
            showToastTips("视频播放完成");
            updateVideoTime(mVideoView.getDuration());
            showVideoCenterPause();
            videoPlay.setImageResource(R.drawable.ic_play);
            clearVideoCatch();

        }
    };
    //缓冲监听
    private PLMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new PLMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(PLMediaPlayer plMediaPlayer, int precent) {

            videoSeekBar.setSecondaryProgress(precent);

        }
    };
    private PLMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = new PLMediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(PLMediaPlayer plMediaPlayer) {

        }
    };

    private PLMediaPlayer.OnPreparedListener mOnPreparedListener = new PLMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(PLMediaPlayer plMediaPlayer) {

            if (mVideoView != null && mVideoView.getDuration() != -1) {
                setVideoProgress(0);
                hideLoading();

                mVideoView.start();
            }


        }
    };
    /*    private PLMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = new PLMediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(PLMediaPlayer plMediaPlayer, int width, int height) {


            }
        };*/
    // 开始拖动时的位置
    private long seekbarStartTrackPosition;
    // 拖动到的位置
    private long seekbarEndTrackPosition;
    // seekbar事件
    private int mVideoProgress = 0;
    private SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            if (fromUser && mVideoView.getCurrentPosition() > 0) {
                mVideoProgress = progress;
                videoHandler.removeMessages(HIDDEN_SEEKBAR);
                Message msg = new Message();
                msg.what = HIDDEN_SEEKBAR;
                videoHandler.sendMessageDelayed(msg, 1000);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            seekbarStartTrackPosition = mVideoView.getCurrentPosition();

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mVideoView.getCurrentPosition() > 0) {
                if (mVideoView != null) {
                    mVideoView.seekTo(mVideoProgress * mVideoView.getDuration()
                            / 100);
                    seekbarEndTrackPosition = mVideoView.getCurrentPosition();
                    videoHandler.removeMessages(VIDEO_SEEKBARFORWARD);
                    videoHandler.sendEmptyMessageDelayed(VIDEO_SEEKBARFORWARD,
                            1 * 100);
                }
            }
        }
    };

    //播放按钮
    private View.OnClickListener mStartBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            mPause = !mPause;
            if (mPause) {
                hideLoading();
                videoPlay.setImageResource(R.drawable.ic_play);
                mVideoView.pause();
                mPauseStartTime = System.currentTimeMillis();
                showVideoCenterPause();
            } else {
                videoPlay.setImageResource(R.drawable.zanting);
                mVideoView.start();
                mPausedTime += System.currentTimeMillis() - mPauseStartTime;
                mPauseStartTime = 0;
                hideVideoCenterPause();
            }


        }
    };
    /**
     * 视频的方向
     */
    private int mVideoRotation;
    private boolean needResume;
    private PLMediaPlayer.OnInfoListener mOnInfoListener = new PLMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(PLMediaPlayer plMediaPlayer, int what, int extra) {
            switch (what) {
                case PLMediaPlayer.MEDIA_INFO_BUFFERING_START:
                    hideCenterBox();
                    videoCenterPause.setVisibility(View.GONE);
                    showLoading();
                    isLoading = true;
                    // 开始缓存，暂停播放
                    break;
                case PLMediaPlayer.MEDIA_INFO_BUFFERING_END:
                    hideLoading();
                    if (mVideoView.isPlaying()) {
                        hideVideoCenterPause();
                    } else {
                        showVideoCenterPause();
                    }
                    isLoading = false;
                    break;
                case PLMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                    // 缓存完成，继续播放
                    if (needResume)
                        // startPlayer();
                        if (mVideoView != null) {
                            mVideoView.start();
                        }
                    break;
                case PLMediaPlayer.MEDIA_INFO_BUFFERING_BYTES_UPDATE:
                    // 显示 下载速度
                    // mListener.onDownloadRateChanged(arg2);
                    break;
                case 10001:
                    // 视频准备完成
                    mVideoRotation = extra;
                    // mListener.onDownloadRateChanged(arg2);
                    break;
            }
            return false;
        }
    };

    /*
        *当前视频是否是正在播放
        *
        * */
    private boolean isPlaying() {
        return mVideoView != null && mVideoView.isPlaying();
    }


    private boolean isShowing;  //控制栏是否是显示
    private float brightness = -1; //亮度
    private int volume = -1;
    private long newPosition = -1; /*滑动屏幕快进到的新位置*/
    private AudioManager audioManager;
    private int mMaxVolume;

    public class PlayerGestureListener extends
            GestureDetector.SimpleOnGestureListener {
        private boolean firstTouch;
        private boolean volumeControl;
        private boolean toSeek;

        /**
         * 双击
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            /*
             * if (!isPrepare) {// 视频没有初始化点击屏幕不起作用 return false; }
			 * videoView.toggleAspectRatio();
			 */
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            firstTouch = true;
            return super.onDown(e);
        }

        /**
         * 滑动
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            float mOldX = e1.getX(), mOldY = e1.getY();
            float deltaY = mOldY - e2.getY();
            float deltaX = mOldX - e2.getX();
            if (firstTouch) {
                toSeek = Math.abs(distanceX) >= Math.abs(distanceY);
                volumeControl = mOldX > screenWidthPixels * 0.5f;
                firstTouch = false;
            }
            if (toSeek) {
                onProgressSlide(-deltaX / framelayout.getWidth());

            } else {
                float percent = deltaY / framelayout.getHeight();
                if (volumeControl) {
                    onVolumeSlide(percent);
                } else {
                    onBrightnessSlide(percent);
                }
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (isShowing) {
                hideControllBar();
            } else {
                showControllBar();
            }
            return true;
        }
    }

    /**
     * 手势结束
     */
    private void endGesture() {
        volume = -1;
        brightness = -1f;
    }

    /**
     * 滑动改变声音大小
     *
     * @param percent
     */
    private void onVolumeSlide(float percent) {
        hideLoading();
        hideVideoCenterPause();
        if (volume == -1) {
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (volume < 0)
                volume = 0;
        }

        int index = (int) (percent * mMaxVolume) + volume;
        if (index > mMaxVolume)
            index = mMaxVolume;
        else if (index < 0)
            index = 0;

        // 变更声音
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
        // 变更进度条
        int i = (int) (index * 1.0 / mMaxVolume * 100);
        String s = i + "%";
        showMessage(s);
        showImg(R.drawable.ic_volume_up_white_36dp);
        showCenterBox();
        videoHandler.removeMessages(VIDEO_HIDECENTERBOX);
        videoHandler.sendEmptyMessageDelayed(VIDEO_HIDECENTERBOX, defaultTime);
    }

    //滑动屏幕快进
    private void onProgressSlide(float percent) {
        hideLoading();
        hideVideoCenterPause();
        long position = mVideoView.getCurrentPosition();
        long duration = mVideoView.getDuration();
        long deltaMax = Math.min(100 * 1000, duration - position);
        long delta = (long) (deltaMax * percent);

        newPosition = delta + position;
        if (newPosition > duration) {
            newPosition = duration;
        } else if (newPosition <= 0) {
            newPosition = 0;
            delta = -position;
        }
        int showDelta = (int) delta / 1000;
        if (showDelta != 0) {
            if (showDelta > 0)
                showImg(R.drawable.video_btn_fast_forword);
            else
                showImg(R.drawable.video_btn_back_forword);

            showMessage(MyUtils.generateTime(newPosition) + "/"
                    + MyUtils.generateTime(mVideoView.getDuration()));

            showCenterBox();
            videoHandler.removeMessages(VIDEO_FORWARD);
            videoHandler.sendEmptyMessageDelayed(VIDEO_FORWARD, 1 * 500);
            videoHandler.removeMessages(VIDEO_HIDECENTERBOX);
            videoHandler.sendEmptyMessageDelayed(VIDEO_HIDECENTERBOX, 1 * 500);

        }
    }

    /**
     * 滑动改变亮度
     *
     * @param percent
     */
    private void onBrightnessSlide(float percent) {
        hideLoading();
        hideVideoCenterPause();
        hideCenterBox();
        if (brightness < 0) {
            brightness = getWindow().getAttributes().screenBrightness;
            if (brightness <= 0.00f) {
                brightness = 0.50f;
            } else if (brightness < 0.01f) {
                brightness = 0.01f;
            }
        }

        WindowManager.LayoutParams lpa = getWindow().getAttributes();
        lpa.screenBrightness = brightness + percent;
        if (lpa.screenBrightness > 1.0f) {
            lpa.screenBrightness = 1.0f;
        } else if (lpa.screenBrightness < 0.01f) {
            lpa.screenBrightness = 0.01f;
        }
        getWindow().setAttributes(lpa);
        showMessage((int) (lpa.screenBrightness * 100) + "%");
        showImg(R.drawable.ic_brightness_6_white_36dp);
        showCenterBox();
        videoHandler.removeMessages(VIDEO_HIDECENTERBOX);
        videoHandler.sendEmptyMessageDelayed(VIDEO_HIDECENTERBOX, defaultTime);

    }

    // 处理点击事件
    private boolean lockVideo = false;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back:
                super.finish();
                context.finish();
                updateVideoCatch();


                break;
            case R.id.warn_continue://屏幕提醒
                if (!isLocal) {
                    if (netType != null && !netType.equals("0")) {
                        isNet = true;
                        hideWarnControll();
                        if ((isNet && mVideoView.getDuration() != -1) || (okWifi && mVideoView.getDuration() != -1)) {
                            if (mVideoView.getCurrentPosition() == 0) {
                                videoViewStart();
                            } else {
                                mVideoView.start();
                            }
                        }

                    } else {
                        Intent intent;
                        //判断手机系统的版本  即API大于10 就是3.0或以上版本
                        if (android.os.Build.VERSION.SDK_INT > 10) {
                            intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                        } else {
                            intent = new Intent();
                            ComponentName component = new ComponentName("com.android.settings", "com.android.settings.WirelessSettings");
                            intent.setComponent(component);
                            intent.setAction("android.intent.action.VIEW");
                        }
                        context.startActivity(intent);
                    }
                }

                break;

            case R.id.video_centerpause:
                //屏幕中间播放按钮
                if (mVideoView != null) {
                    if (mVideoView.getCurrentPosition() == mVideoView.getDuration())
                        mVideoView.seekTo(0);
                    else {
                        if (mVideoView.getCurrentPosition() > 0) {
                            mVideoView.start();
                        } else {
                            mVideoView.setVideoPath(mVideoPath);
                            mVideoView.start();
                            showLoading();
                        }
                    }

                }
                hideVideoCenterPause();
                videoPlay.setImageResource(R.drawable.zanting);
                break;

            case R.id.video_lock:
                if (lockVideo == true) {
                    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    lockVideo = false;
                    mVideoLock.setImageResource(R.drawable.playerunlocked);
                } else {
                    lockVideo = true;
                    mVideoLock.setImageResource(R.drawable.playerlocked);
                    if (Orieantation == 1) {
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    } else if (Orieantation == 2) {
                        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    }
                }
                break;
            case R.id.pop_close:
                //课前指导
                if (window != null && window.isShowing())
                    window.dismiss();
                break;
            case R.id.my_video_zhangjie:
                showPopWindow();
                break;
            default:
                break;
        }
    }


    // 显示中间按钮
    private void showCenterBox() {
        center_content.setVisibility(View.VISIBLE);
        linear_centercontroll.setVisibility(View.VISIBLE);
    }

    // 隐藏中间按钮
    private void hideCenterBox() {
        center_content.setVisibility(View.INVISIBLE);
        linear_centercontroll.setVisibility(View.INVISIBLE);
    }

    // 设置显示内容
    private void showMessage(String message) {
        if (message != null)
            center_content.setText(message);
    }

    // 设置图片
    private void showImg(int imgId) {
        Drawable drawable = ContextCompat.getDrawable(context, imgId);
        // / 这一步必须要做,否则不会显示.
        drawable.setBounds(0, 0, drawable.getMinimumWidth(),
                drawable.getMinimumHeight());
        center_content.setCompoundDrawables(drawable, null, null, null);
    }

    // 显示加载进度条
    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
    }

    // 隐藏加载进度条
    private void hideLoading() {
        loadingView.setVisibility(View.INVISIBLE);
    }

    // 显示控制层
    private void showControllBar() {
        topControll.setVisibility(View.VISIBLE);
        bottomControll.setVisibility(View.VISIBLE);
        isShowing = true;
        videoHandler.removeMessages(VIDEO_HIDECONTROLLBAR);
        videoHandler
                .sendEmptyMessageDelayed(VIDEO_HIDECONTROLLBAR, defaultTime);
    }

    // 隐藏控制层
    private void hideControllBar() {
        topControll.setVisibility(View.INVISIBLE);
        bottomControll.setVisibility(View.INVISIBLE);
        isShowing = false;
    }

    private long length;

    // 设置进度条进度
    public int setVideoProgress(int currentProgress) {
        if (mVideoView == null)
            return -1;
        long time = currentProgress > 0 ? currentProgress : mVideoView
                .getCurrentPosition();
        length = mVideoView.getDuration();
        int curProgress = length == 0 ? 0 : (int) (time * 100 / length);
        if (curProgress > 100)
            curProgress = 100;
        videoSeekBar.setProgress(curProgress);
        //更新播放时间
        if (mVideoView.getCurrentPosition() > 0 && mVideoView.isPlaying() && mVideoView.getCurrentPosition() / 10 % 30 == 0) {
            updateVideoTime(length);
        }
        if (time >= 0) {
            currentTime.setText(MyUtils.generateTime(time));
            totalTime.setText("/" + MyUtils.generateTime(length));
        }
        Message msg = new Message();
        msg.what = UPDATE_SEEKBAR;
        if (videoHandler != null)
            videoHandler.sendMessageDelayed(msg, 1000);

        return (int) time;
    }

    /*
    * 屏幕旋转监听
    * */
    private int Orieantation;

    class MyOrientationListener extends OrientationEventListener {
        public MyOrientationListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int i) {
            if (i > 45 && i < 135) {
                //反横向屏幕
                Orieantation = 1;
            } else if (i > 225 && i < 335) {
                //横屏
                Orieantation = 2;
            }

        }

    }
    String url;
    //更新当前播放视频缓存
    private void updateVideoCatch() {
        if (running) {
            if (type != null) {
                if (type.equals("course")) {
                    url = Constants.OUTRT_NET + "/" + activityId + "/teach/m/video/user/" + videoId + "/updateVideoStatus";
                } else if (type.equals("workshop")) {
                    url = Constants.OUTRT_NET + "/student_" + workshopId + "/m/video/user/" + videoId + "/updateVideoStatus";
                }

                OkHttpClientManager.getAsyn(context, url, new OkHttpClientManager.ResultCallback<String>() {
                    @Override
                    public void onError(Request request, Exception e) {

                    }

                    @Override
                    public void onResponse(String response) {


                    }
                });
            }

        }

    }


    //更新视频的观看时间
    private void updateVideoTime(long lastUpdateTime) {
        if (running) {
            if (type != null) {
                if (type.equals("course")) {
                    url = Constants.OUTRT_NET + "/" + activityId + "/teach/m/video/user/" + videoId + "/updateViewTime";

                } else if (type.equals("workshop")) {
                    url = Constants.OUTRT_NET + "/student_" + workshopId + "/m/video/user/" + videoId + "/updateViewTime";
                }

                Map<String, String> map = new HashMap<>();
                map.put("lastUpdateTime", String.valueOf(lastUpdateTime));
                map.put("isLimit", "true");
                map.put("_method", "PUT");
                OkHttpClientManager.postAsyn(context, url, new OkHttpClientManager.ResultCallback<String>() {
                    @Override
                    public void onError(Request request, Exception e) {

                    }

                    @Override
                    public void onResponse(String response) {


                    }
                }, map);
            }
        }


    }

    //清除当前播放视频缓存
    private void clearVideoCatch() {

        if (running) {
            if (type != null) {
                if (type.equals("course")) {
                    url = Constants.OUTRT_NET + "/" + activityId + "/teach/m/video/user/" + videoId + "/removeVideoStatus";
                } else if (type.equals("workshop")) {
                    url = Constants.OUTRT_NET + "/student_" + workshopId + "/m/video/user/" + videoId + "/removeVideoStatus";
                }
                OkHttpClientManager.getAsyn(context, url, new OkHttpClientManager.ResultCallback<String>() {
                    @Override
                    public void onError(Request request, Exception e) {

                    }

                    @Override
                    public void onResponse(String response) {


                    }
                });
            }

        }
    }

}
