package com.zhuyong.audiorecord;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.czt.activity.AudioPlayActivity;
import com.czt.view.MP3RecordButton;
import com.czt.view.MP3RecordView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    /**
     * 录音控件
     */
    private MP3RecordView mViewRecord;
    /**
     * 录音文件路径
     */
    private String mAudioPath = "";

    private TextView mTvText;
    private Button mBtnRecord;
    private MP3RecordButton mMP3Record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mTvText = findViewById(R.id.tv_text);
        mViewRecord = findViewById(R.id.view_record);
        mBtnRecord = findViewById(R.id.btn_rwcord);
        mMP3Record = findViewById(R.id.btn_record);

        /**
         * 设置可触控View
         */
        mViewRecord.setRootView(mBtnRecord);
        mViewRecord.setOnRecordCompleteListener((filePath, duration) -> {
            mAudioPath = filePath;
            String str = "文件地址1：" + filePath + "\n\n录音时长:" + duration / 1000 + "秒";
            Log.i("MainActivity", str);
            mTvText.setText(str);
        });

        /**
         * 设置回调
         */
        mMP3Record.setOnRecordCompleteListener((filePath, duration) -> {
            mAudioPath = filePath;
            String str = "文件地址：" + filePath + "\n\n录音时长:" + duration / 1000 + "秒";
            Log.i("MainActivity", str);
            mTvText.setText(str);
        });

        findViewById(R.id.btn_play).setOnClickListener(v -> {
            if (TextUtils.isEmpty(mAudioPath) || !new File(mAudioPath).exists()) {
                Toast.makeText(MainActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MainActivity.this, AudioPlayActivity.class);
            intent.putExtra(AudioPlayActivity._AUDIO_PATH, mAudioPath);
            startActivity(intent);

        });
    }
}
