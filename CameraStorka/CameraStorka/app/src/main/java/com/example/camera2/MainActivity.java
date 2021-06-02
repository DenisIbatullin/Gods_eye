package com.example.camera2;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    public static final String LOG_TAG = "myLogs";

    private Surface recorderSurface = null;
    private CameraService[] myCameras = null;
    private CameraManager mCameraManager = null;
    private final int CAMERA1 = 0;
    public static TextureView mImageView = null;

    SharedPreferences settings;

    private static final String PREFS_FILE = "CameraSettings";
    private static final String PREF_PHONE = "Phone";
    private static final String PREF_MAIL = "Mail";
    private static final String PREF_DELT = "Delt";
    private String phoneStr = "не определено";
    private String mailStr = "не определено";

    private float delt = 6;
    private boolean record = false;
    private boolean updateSurface = false;
    Timer timerRecord;

    private Button mButtonBeginRecord = null;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler = null;
    private File mCurrentFile;
    private MediaRecorder mMediaRecorder = null;
    private MediaRecorder mMediaRecorder2 = null;
    private boolean mRecorderRunning = false;
    private boolean mRecorder2Running = false;

    SensorService sensorService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(LOG_TAG, "onServiceConnected");
            SensorService.LocalBinder binder = (SensorService.LocalBinder) iBinder;
            sensorService = binder.getService();
            sensorService.setSrvDelt(delt);
            sensorService.setSrvMailStr(mailStr);
            sensorService.setSrvPhoneStr(phoneStr);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (data == null) {
                return;
            }
            if (resultCode == RESULT_OK) {
                phoneStr = data.getStringExtra("Phone");
                mailStr = data.getStringExtra("Mail");
                delt = data.getFloatExtra("Delt", 6);
                sensorService.setSrvDelt(delt);
                sensorService.setSrvMailStr(mailStr);
                sensorService.setSrvPhoneStr(phoneStr);
                SharedPreferences.Editor editor = settings.edit();
                editor.putFloat(PREF_DELT, delt);
                editor.putString(PREF_MAIL, mailStr);
                editor.putString(PREF_PHONE, phoneStr);
                Log.i("MY_LOG", "onActivityResult MAil = " + mailStr);
                editor.apply();
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, SensorService.class);
        startService(intent);



        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        settings = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        if (settings.contains(PREF_MAIL)) {
            mailStr = settings.getString(PREF_MAIL, "");
        }
        if (settings.contains(PREF_PHONE)) {
            phoneStr = settings.getString(PREF_PHONE, "");
        }
        if (settings.contains(PREF_DELT)) {
            delt = settings.getFloat(PREF_DELT, 6);
        }
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.item1) {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    intent.putExtra(PREF_PHONE, phoneStr);
                    intent.putExtra(PREF_MAIL, mailStr);
                    intent.putExtra(PREF_DELT, delt);
                    startActivityForResult(intent, 1);
                }
                return true;
            }
        });

        mImageView = findViewById(R.id.textureView);
        mImageView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.i(LOG_TAG, "onSurfaceTextureAvailable");
                if (!myCameras[CAMERA1].isOpen()) myCameras[CAMERA1].openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.i(LOG_TAG, "onSurfaceTextureSizeChanged");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.i(LOG_TAG, "onSurfaceTextureDestroyed");
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                if (updateSurface) {
                    setUpMediaRecorder();
                    myCameras[CAMERA1].openCamera();
                    updateSurface = false;
                }

            }
        });

        mButtonBeginRecord = findViewById(R.id.button1);
        mButtonBeginRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myCameras[CAMERA1] != null) {
                    if (mMediaRecorder != null) {
                        if (record == false) {
                            mButtonBeginRecord.setText("Закончить запись");
                            record = true;
                            sensorService.setSrvRecord(record);
                            timerRecord = new Timer();
                            myCameras[CAMERA1].startRecordingVideo();
                        } else {
                            mButtonBeginRecord.setText("Начать запись");
                            record = false;
                            sensorService.setSrvRecord(record);
                            timerRecord.cancel();
                        }
                    }
                }
            }
        });

        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            myCameras = new CameraService[mCameraManager.getCameraIdList().length];
            for (String cameraID : mCameraManager.getCameraIdList()) {
                Log.i(LOG_TAG, "cameraID: " + cameraID);
                int id = Integer.parseInt(cameraID);
                myCameras[id] = new CameraService(mCameraManager, cameraID);
            }
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }
        setUpMediaRecorder();

        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/accident");
        if (dir.exists() == false)
            dir.mkdir();
    }


    private void setUpMediaRecorder2() {
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        mMediaRecorder2 = new MediaRecorder();
        Date date = new Date();
        mCurrentFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "video_" + date.getTime() + ".mp4");
        mMediaRecorder2.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder2.setInputSurface(recorderSurface);
        mMediaRecorder2.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder2.setOutputFile(mCurrentFile.getAbsolutePath());
        mMediaRecorder2.setVideoSize(640, 480);
        mMediaRecorder2.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder2.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder2.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder2.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder2.setAudioEncodingBitRate(profile.audioBitRate);
        mMediaRecorder2.setAudioSamplingRate(profile.audioSampleRate);

        Log.i(LOG_TAG, "setUpMediaRecorder2");
    }

    private void setUpMediaRecorder1() {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setInputSurface(recorderSurface);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        ///Environment.getExternalStorageDirectory()
        Date date = new Date();
        mCurrentFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "video_" + date.getTime() + ".mp4");
        mMediaRecorder.setOutputFile(mCurrentFile.getAbsolutePath());
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        mMediaRecorder.setVideoSize(640, 480);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
        Log.i(LOG_TAG, "setUpMediaRecorder1");
    }

    private void setUpMediaRecorder() {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        Date date = new Date();
        mCurrentFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "video_" + date.getTime() + ".mp4");
        mMediaRecorder.setOutputFile(mCurrentFile.getAbsolutePath());
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        mMediaRecorder.setVideoSize(640, 480);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);

        mMediaRecorder2 = new MediaRecorder();
        mMediaRecorder2.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder2.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder2.setOutputFile(mCurrentFile.getAbsolutePath());
        mMediaRecorder2.setVideoSize(640, 480);
        mMediaRecorder2.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder2.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder2.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder2.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder2.setAudioEncodingBitRate(profile.audioBitRate);
        mMediaRecorder2.setAudioSamplingRate(profile.audioSampleRate);

        //mMediaRecorder.next .setNextMediaPlayer(mMediaRecorder2);
    }

    public class CameraService {
        private String mCameraID;
        private CameraDevice mCameraDevice = null;
        private CameraCaptureSession mSession;
        private CaptureRequest.Builder mPreviewBuilder;

        public CameraService(CameraManager cameraManager, String cameraID) {
            mCameraManager = cameraManager;
            mCameraID = cameraID;
        }

        private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(CameraDevice camera) {
                mCameraDevice = camera;
                Log.i(LOG_TAG, "Open camera  with id:" + mCameraDevice.getId());
                startCameraPreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                mCameraDevice.close();
                Log.i(LOG_TAG, "disconnect camera  with id:" + mCameraDevice.getId());
                mCameraDevice = null;
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Log.i(LOG_TAG, "error! camera id:" + camera.getId() + " error:" + error);
            }
        };

        private void startCameraPreviewSession() {
            if (mImageView.getSurfaceTexture() == null)
                return;
            SurfaceTexture texture = mImageView.getSurfaceTexture();

            texture.setDefaultBufferSize(640, 480);
            Surface surface = new Surface(texture);

            try {
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewBuilder.addTarget(surface);

                recorderSurface = MediaCodec.createPersistentInputSurface();
                mMediaRecorder.setInputSurface(recorderSurface);
                mMediaRecorder2.setInputSurface(recorderSurface);
                try {
                    mMediaRecorder.prepare();
                    Log.i(LOG_TAG, " запустили медиа рекордер");

                } catch (Exception e) {
                    Log.i(LOG_TAG, "не запустили медиа рекордер");
                }
                mPreviewBuilder.addTarget(recorderSurface);

                mCameraDevice.createCaptureSession(Arrays.asList(surface, recorderSurface),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                mSession = session;

                                try {
                                    mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                            }
                        }, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        public void startRecordingVideo() {
            timerRecord.schedule(new TimerTask() {
                @Override
                public void run() {
                    MyTask mt = new MyTask();
                    Log.i(LOG_TAG,"startRecordingVideo");
                    mt.execute();
                }
            }, 0, 15300);

        }

        public boolean isOpen() {
            if (mCameraDevice == null) {
                return false;
            } else {
                return true;
            }
        }

        public void openCamera() {
            try {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    mCameraManager.openCamera(mCameraID, mCameraCallback, mBackgroundHandler);
                }
            } catch (CameraAccessException e) {
                Log.i(LOG_TAG, e.getMessage());
            }
        }

        public void closeCamera() {
            Log.i(LOG_TAG, "closeCamera");
            mSession.close();
            mCameraDevice.close();
            updateSurface = true;

            if (record && mRecorderRunning) {
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder.reset();
            }
            if (record && mRecorder2Running) {
                mMediaRecorder2.stop();
                mMediaRecorder2.release();
                mMediaRecorder2.reset();
            }
        }
    }

    @Override
    public void onPause() {
        Log.i(LOG_TAG, "onPause");

        if (myCameras[CAMERA1].isOpen()) {
            myCameras[CAMERA1].closeCamera();
        }

        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        //if (!myCameras[CAMERA1].isOpen()) myCameras[CAMERA1].openCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, SensorService.class);
        stopService(intent);
    }

    class MyTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.i(LOG_TAG, "BEGIN ASYNKTASK" + mRecorderRunning + " " + mRecorder2Running);
            if (mRecorderRunning == false) {
                mRecorderRunning = true;
                Log.i(LOG_TAG, "START TIMER TASK1");
                try {
                    mMediaRecorder.prepare();
                    Log.i(LOG_TAG, " запустили медиа рекордер");
                } catch (Exception e) {
                    Log.i(LOG_TAG, "не запустили медиа рекордер");
                }
                mMediaRecorder.start();

                Timer myTimer = new Timer();
                myTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Log.i(LOG_TAG, "STOP TIMER TASK1");
                        mMediaRecorder.stop();
                        mMediaRecorder.release();
                        mRecorderRunning = false;
                        setUpMediaRecorder1();

                    }
                }, 15000);
            } else {
                mRecorder2Running = true;
                try {
                    mMediaRecorder2.prepare();
                    Log.i(LOG_TAG, " запустили медиа рекордер2");
                } catch (Exception e) {
                    Log.i(LOG_TAG, "не запустили медиа рекордер2");
                }
                mMediaRecorder2.start();
                Log.i(LOG_TAG, "START TIMER TASK2");

                Timer myTimer = new Timer();
                myTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Log.i(LOG_TAG, "STOP TIMER TASK2");
                        mMediaRecorder2.stop();
                        mMediaRecorder2.release();
                        mRecorder2Running = false;
                        setUpMediaRecorder2();
                    }
                }, 15000);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }
}