package com.czt.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.czt.mp3recorder.MP3Recorder;
import com.czt.utils.FileUtils;
import com.czt.utils.LogUtils;

import java.io.File;
import java.io.IOException;

public class MP3RecordButton extends AppCompatImageButton implements View.OnTouchListener {
    private static final String TAG = "MP3RecordView";

    /**
     * 录音类
     */
    private MP3Recorder mRecorder;
    //录音对话框
    private DialogManager mDialogManager;
    /**
     * 录音文件地址
     */
    private String mFilePath = "";
    /**
     * 录音时间标记参数
     */
    private long mDuration;

    private boolean mIsOnTouch = false;
    private boolean mIsRecording = false;
    /**
     * 点击时间间隔控制参数，防止快速点击造成录音异常
     */
    private long time;
    /**
     * 录音结果回调接口
     */
    private OnRecordCompleteListener onRecordCompleteListener;

    @Override
    public boolean isInEditMode() {
        return true;
    }

    public MP3RecordButton(Context context) {
        this(context, null);
    }

    public MP3RecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        //初始化语音对话框
        mDialogManager = new DialogManager(getContext());
    }

    //手指滑动监听
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && System.currentTimeMillis() - time < 1000) {
            LogUtils.i(TAG, "时间间隔小于1秒，不执行以下代码");
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsOnTouch = true;
                mDialogManager.showRecordingDialog();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mIsOnTouch) {
                            LogUtils.i(TAG, "执行以下代码");
                            time = System.currentTimeMillis();
                            mIsRecording = true;
                            startRecording();
                        }
                    }
                }, 300);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (event.getY() < 0) {
                    mDialogManager.wantToCancel();
                } else {
                    mDialogManager.recording();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsOnTouch = false;
                if (mIsRecording) {
                    if (event.getY() < 0) {
                        resolveError();
                        LogUtils.i(TAG, "取消录音");
                    } else {
                        long length = resolveStopRecord();
                        mIsRecording = false;
                        if (length > 1000) {
                            this.setVisibility(INVISIBLE);
                            Message message = new Message();
                            message.what = MP3Recorder.MESSAGE_FINISH;
                            message.arg1 = (int) length;
                            mHandler.sendMessage(message);
                        } else {
                            mDialogManager.tooShort();
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    setVisibility(INVISIBLE);
                                }
                            }, 1000);
                        }
                    }
                }
                mDialogManager.dismissDialog();
                return true;
            default:
                LogUtils.i(TAG, "执行到了其他的:" + event.getAction());
                resolveError();
                return false;
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MP3Recorder.MESSAGE_RECORDING://获取录音音量
                    int mVolume = msg.arg1;//0-2000
                    int index = mVolume / 300;
//                    mImgRecordVolume.setImageDrawable(mImagesVolume[index]);
                    mDialogManager.updateVoiceLevel(index + 1);
                    break;
                case MP3Recorder.MESSAGE_ERROR_PERMISSION://没有麦克风权限
                    Toast.makeText(getContext(), "没有麦克风权限", Toast.LENGTH_SHORT).show();
                    resolveError();
                    break;
                case MP3Recorder.MESSAGE_ERROR://录音异常
                    Toast.makeText(getContext(), "录音异常,请检查权限是否打开", Toast.LENGTH_SHORT).show();
                    resolveError();
                    break;
                case MP3Recorder.MESSAGE_FINISH://录音完成
                    int duration = msg.arg1;
                    LogUtils.i(TAG, "录音文件地址:" + mFilePath + "   时长duration:" + duration / 1000 + "S");

                    if (onRecordCompleteListener != null) {
                        onRecordCompleteListener.onComplete(mFilePath, duration);
                    }
                    break;
            }
        }
    };

    /**
     * 开始录音
     */
    private void startRecording() {
        mFilePath = FileUtils.getAppPath();
        File file = new File(mFilePath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Toast.makeText(getContext(), "创建文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        mFilePath = FileUtils.getAppPath() + System.currentTimeMillis() + "-176.mp3";
        if (mRecorder == null) {
            mRecorder = new MP3Recorder(new File(mFilePath));
            mRecorder.setHandler(mHandler);
        }
        mRecorder.setmRecordFile(new File(mFilePath));

        try {
            mRecorder.start();
            mDuration = System.currentTimeMillis();
            this.setVisibility(VISIBLE);
        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.i(TAG, "录音出现异常：" + e.toString());
            mHandler.sendEmptyMessage(MP3Recorder.MESSAGE_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG, "录音出现异常：" + e.toString());
            mHandler.sendEmptyMessage(MP3Recorder.MESSAGE_ERROR);
        }
    }


    /**
     * 停止录音
     *
     * @return 返回录音时间间隔, 即录音时长
     */
    private long resolveStopRecord() {
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.setPause(false);
            mRecorder.stop();
        }
        return System.currentTimeMillis() - mDuration;
    }

    /**
     * 录音异常
     */
    private void resolveError() {
        this.setVisibility(View.INVISIBLE);
        FileUtils.deleteFile(mFilePath);
        mIsOnTouch = false;
        mFilePath = "";
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.stop();
        }
    }

    /**
     * 设置是否打印录音相关参数
     *
     * @param debug
     */
    public void setDebug(boolean debug) {
        LogUtils.IS_DEBUGING = debug;
    }

    /**
     * 录音结果回调
     */

    public void setOnRecordCompleteListener(OnRecordCompleteListener listener) {
        this.onRecordCompleteListener = listener;
    }

    public interface OnRecordCompleteListener {
        void onComplete(String filePath, int duration);
    }
}
