package com.softwinner.tv_d_video;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.softwinner.tv_d_video.adapter.VideoAdapter;
import com.softwinner.tv_d_video.model.FileVideo;
import com.softwinner.tv_d_video.utils.VideoData;
import com.softwinner.tv_d_video.utils.VideoUtils;
import com.softwinner.tv_d_video.view.CustomToast;
import com.softwinner.tv_d_video.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {
    private ListView mVideoList;
    private VideoAdapter adapter;
    private LinearLayout mVideoInfoList;
    private RadioGroup mBtnSelector;
    private ImageView mSpende;
    private ImageView mNextSong;
    private ImageView mOnSong;
    private SeekBar mSeekBar;
    private SurfaceView mSurfaceView;
    private TextView mMusicTotalTime;
    private TextView mMusicCurrentTime;
    private MediaPlayer mPlayer = null;
    private Handler mSeekBarSyncHandler = new Handler();
    private Runnable runnable;
    private LinearLayout mController;

    private int mPosition = 0;
    private int currentPosition = 0;
    private boolean PasueFlag = false;

    private List<FileVideo> mVideo = new ArrayList<FileVideo>();
    private String path000 = null;
    private Uri mUrl;
    private SurfaceHolder mSurfaceHolder;
    private ImageView mImage;

    private static final int REQUEST = 112;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST );
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);// 隐藏标题栏
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);// 隐藏状态栏
            setContentView(R.layout.activity_main);
            VideoUtils.getBootStatus(MainActivity.this);
            initView();
            registerListener();
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestWindowFeature(Window.FEATURE_NO_TITLE);// 隐藏标题栏
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);// 隐藏状态栏
                    setContentView(R.layout.activity_main);
                    VideoUtils.getBootStatus(MainActivity.this);
                    initView();
                    registerListener();
                } else {
                    finish();
                }
            }
        }
    }

    @SuppressLint("CutPasteId")
    private void initView() {
        mVideoList = (ListView) findViewById(R.id.swipe_list);
        mBtnSelector = (RadioGroup) findViewById(R.id.btn_select);
        mVideoInfoList = (LinearLayout) findViewById(R.id.video_list);
        mSpende = (ImageView) findViewById(R.id.image_spende);
        mNextSong = (ImageView) findViewById(R.id.image_next);
        mOnSong = (ImageView) findViewById(R.id.image_on);
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mMusicTotalTime = (TextView) findViewById(R.id.music_total_time);
        mMusicCurrentTime = (TextView) findViewById(R.id.music_current_time);
        mController = (LinearLayout) findViewById(R.id.controller);
        mImage = (ImageView) findViewById(R.id.image_bj);
        mSurfaceView.getHolder().setKeepScreenOn(true);// 使屏幕保持高亮状态
        mBtnSelector.check(mBtnSelector.getChildAt(0).getId());
        mSeekBar.setOnSeekBarChangeListener(chenageSeekBar);
        mController.getBackground().setAlpha(120);
        mSurfaceView.setOnClickListener(this);
        mOnSong.setOnClickListener(this);
        mSpende.setOnClickListener(this);
        mNextSong.setOnClickListener(this);
        mController.setOnClickListener(this);
        mImage.setOnClickListener(this);
        mPlayer = new MediaPlayer();
        // 把输送给surfaceView视频画面，直接显示在屏幕上，不要维持它自身的缓冲区
        mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceView.getHolder().setKeepScreenOn(true);// 使屏幕保持高亮状态
        mSurfaceHolder = mSurfaceView.getHolder();
        runnable = new Runnable() {
            @Override
            public void run() {
                mhandler.sendMessage(Message.obtain(mhandler, 1));
            }
        };
        mSurfaceHolder.addCallback(new Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mPlayer != null) {
                    if (mPlayer.isPlaying()) {
                        currentPosition = mPlayer.getCurrentPosition();
                        mPlayer.pause();
                    }
                }
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (!PasueFlag) {
                    if (getIntent().getData() != null) {
                        path000 = getIntent().getStringExtra("VideoPath000");
                        mUrl = VideoUtils.Uri2File2Uri(getIntent().getData(), getApplicationContext(), path000);
                        mVideoList.setVisibility(View.GONE);
                        mNextSong.setEnabled(false);
                        mOnSong.setEnabled(false);
                        initData(-1, mUrl, currentPosition);
                        currentPosition = 0;
                    } else {
                        new VideoData(FileVideoCallback, getApplicationContext()).run();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

        });
    }

    private Handler mhandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                if (mPlayer != null) {
                    final int max = mPlayer.getDuration();
                    mSeekBar.setMax(max);
                    mMusicTotalTime.setText(VideoUtils.getTime(max));
                    if (mPlayer != null && mPlayer.isPlaying()) {
                        currentPosition = mPlayer.getCurrentPosition();
                        mMusicCurrentTime.setText(VideoUtils.getTime(currentPosition));
                        mSeekBar.setProgress(currentPosition);
                        mSeekBarSyncHandler.postDelayed(runnable, 1000);
                    }
                }
            }
        }

        ;
    };

    private SeekBar.OnSeekBarChangeListener chenageSeekBar = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getProgress();
            currentPosition = seekBar.getProgress();
            setImageView(true);
            if (mPlayer != null) {
                mPlayer.seekTo(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mMusicCurrentTime.setText(VideoUtils.getTime(progress));
        }
    };

    private void registerListener() {
        mBtnSelector.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mBtnSelector.getChildAt(0).getId() == checkedId) {
                    mVideoInfoList.setVisibility(View.VISIBLE);
                } else if (mBtnSelector.getChildAt(1).getId() == checkedId) {
                    mVideoInfoList.setVisibility(View.GONE);
                }
            }
        });
        mVideoList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPosition = position;
                mSpende.setVisibility(View.GONE);
                if (PasueFlag == true) {
                    PasueFlag = false;
                    mSpende.setVisibility(View.GONE);
                }
                initData(position, null, currentPosition);
            }
        });
    }

    private void initData(int position, Uri path, int currentPosition) {
        if (mSurfaceView.getVisibility() != 0) {
            setImageView(false);
        }
        if (position != -1) {
            adapter.setPositionSelector(position);
            mSpende.setVisibility(View.GONE);
            mController.setVisibility(View.GONE);
        }
        if (PasueFlag) {
            if (mPlayer != null) {
                mPlayer.setDisplay(mSurfaceHolder);
                mPlayer.seekTo(currentPosition);
                mPlayer.start();
                mSeekBarSyncHandler.postDelayed(runnable, 1000);
            }
            PasueFlag = false;
        } else {
            try {
                mPlayer.reset();
                if (mPlayer != null) {
                    mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mPlayer.setDisplay(mSurfaceHolder);
                    if (position != -1) {
                        if (mVideo.size() != 0) {
                            Log.d("mplayer", mVideo.get(position).getUrl());
                            mPlayer.setDataSource(mVideo.get(position).getUrl());
                            mPlayer.setOnCompletionListener(MediaPlayerCompletionListener);
                        }
                    } else {
                        mPlayer.setDataSource(getApplicationContext(), Uri.parse(mUrl.getPath().replace("file", "")));
                        mPlayer.setLooping(true);
                    }
                    mPlayer.setOnPreparedListener(new MediaPlayerPreparedListener(currentPosition));
                    mPlayer.prepareAsync();
                }
            } catch (IOException e) {
                CustomToast.show(getApplicationContext(), "无法播放此视频");
                e.printStackTrace();
            }
        }

    }

    private final class MediaPlayerPreparedListener implements OnPreparedListener {
        private int position;

        public MediaPlayerPreparedListener(int position) {
            this.position = position;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            PasueFlag = false;
            mPlayer.start();
            if (position > 0) mPlayer.seekTo(position);
            mSeekBarSyncHandler.postDelayed(runnable, 1000);
        }
    }

    MediaPlayer.OnCompletionListener MediaPlayerCompletionListener = new OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            setNextSong();
        }
    };

    public VideoData.Callback FileVideoCallback = new VideoData.Callback() {
        @Override
        public void onSuccess(List<FileVideo> mVideoInfo) {
            if (mVideoInfo.size() != 0) {
                mVideo = mVideoInfo;
                mVideoList.setVisibility(View.VISIBLE);
                mSurfaceView.setEnabled(true);
                mNextSong.setEnabled(true);
                mOnSong.setEnabled(true);
                mVideoList.setBackgroundResource(R.color.gray);
                adapter = new VideoAdapter(MainActivity.this, mVideo);
                mVideoList.setAdapter(adapter);
                initData(mPosition, null, currentPosition);
                currentPosition = 0;
            } else {
                mSurfaceView.setEnabled(false);
                mNextSong.setEnabled(false);
                mOnSong.setEnabled(false);
                mVideoList.setVisibility(View.GONE);
                mSurfaceView.setBackgroundResource(R.color.gray);
            }
        }
    };

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.image_spende) {
            setPlay();
        } else if (R.id.surface == id) {
            if (PasueFlag == false) {
                setSpende();
            } else {
                setPlay();
            }
        } else if (R.id.image_bj == id) {
            setPlay();
        } else if (R.id.image_next == id) {
            setOnSong();
        } else if (R.id.image_on == id) {
            setNextSong();
        }
//        switch (v.getId()) {
//            case R.id.image_spende -> setPlay();
//            case R.id.surface -> {
//                if (PasueFlag == false) {
//                    setSpende();
//                } else {
//                    setPlay();
//                }
//            }
//            case R.id.image_bj -> setPlay();
//            case R.id.image_next -> setOnSong();
//            case R.id.image_on -> setNextSong();
//            default -> {
//            }
//        }
    }

    /**
     * TODO 播放
     */
    public void setPlay() {
        if (getIntent().getData() != null) {
            initData(-1, mUrl, currentPosition);
        } else {
            initData(mPosition, null, currentPosition);
        }
    }

    /**
     * TODO 暂停
     */
    public void setSpende() {
        mSpende.setVisibility(View.VISIBLE);
        if (mPlayer != null && mPlayer.isPlaying()) {
            currentPosition = mPlayer.getCurrentPosition();
            mSeekBarSyncHandler.removeCallbacks(runnable);
            mPlayer.pause();
            setImageView(true);
            PasueFlag = true;
            mController.setVisibility(View.VISIBLE);
        }
    }

    private void setImageView(final boolean isStatus) {
        mImage.setImageBitmap(VideoUtils.setVideoImage(mVideo.get(mPosition).getUrl(), currentPosition));
        mImage.setVisibility(isStatus ? View.VISIBLE : View.GONE);
        mSurfaceView.setVisibility(isStatus ? View.GONE : View.VISIBLE);
    }

    /**
     * TODO 下一个
     */
    public void setOnSong() {
        if (mPosition > 0) {
            mPosition -= 1;
        } else {
            mPosition = mVideo.size() - 1;
        }
        if (PasueFlag == true) {
            PasueFlag = false;
            mSpende.setVisibility(View.GONE);
        }
        currentPosition = 0;
        initData(mPosition, null, currentPosition);
    }

    /**
     * TODO 上一个
     */
    public void setNextSong() {
        if (mPosition < mVideo.size() - 1) {
            mPosition += 1;
        } else {
            mPosition = 0;
        }
        if (PasueFlag == true) {
            PasueFlag = false;
            mSpende.setVisibility(View.GONE);
        }
        currentPosition = 0;
        initData(mPosition, null, currentPosition);
    }

    @Override
    protected void onResume() {
        VideoUtils.getStatus(MainActivity.this);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (mPlayer.isPlaying()) {
            mPlayer.stop();
        }
        mSeekBarSyncHandler.removeCallbacks(runnable);
        mPlayer.release();
        mPlayer = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        VideoUtils.setExitSettingStatus(MainActivity.this);
        super.onBackPressed();
    }
}
