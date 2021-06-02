package com.example.camera2;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.Date;

public class SensorService extends Service implements SensorEventListener {
    public static final String LOG_TAG = "myLogs";
    private SensorManager sensorManager;
    private Sensor sensorLine;
    private float oldLineAcc;

    private float srvDelt = 6;
    private String srvPhoneStr = "не определено";
    private String srvMailStr = "не определено";
    private boolean srvRecord = false;

    private final IBinder serviceBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        SensorService getService() {
            return SensorService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    //При запуске сервиса. Если нужно, то запускаем его в фоновом режиме
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorLine = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, sensorLine, SensorManager.SENSOR_DELAY_NORMAL);

        if (sensorLine == null) {
            Toast.makeText(this, "Невозможно подключиться к акселерометру", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            if (Math.abs(event.values[0] - oldLineAcc) > srvDelt) {
                if (srvRecord == false) {
                    Log.i(LOG_TAG, "Delt " + srvDelt);
                    try {
                        if (srvPhoneStr.isEmpty() == false && srvPhoneStr.startsWith("8") && srvPhoneStr.length() == 11) {
                            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel: " + srvPhoneStr));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                        if (srvMailStr.isEmpty() == false && srvMailStr.contains("@")) {

                        }

                    } catch (android.content.ActivityNotFoundException ex) {
                    }
                    oldLineAcc = event.values[0];
                    return;
                }
                Date date = new Date();
                long dateCat = date.getTime() - 30000;
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());
                File[] path = file.listFiles();
                if (path.length > 0) {
                    for (File currentFile : path) {
                        if (currentFile.isDirectory() == false || currentFile.getName().startsWith("video_")) {
                            String valueFile = currentFile.getName();
                            String str = valueFile.substring(valueFile.indexOf("_") + 1, valueFile.length() - 4);
                            Long fileLong = Long.valueOf(str);
                            if (fileLong > dateCat) {
                                Log.i(LOG_TAG, "currentFile: " + currentFile.getParent() + "/accident/" + currentFile.getName());
                                currentFile.renameTo(new File(currentFile.getParent() + "/accident/" + currentFile.getName()));
                            }
                        }
                    }
                }
            }
            oldLineAcc = event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    public void setSrvDelt(float srvDelt) {
        Log.i(LOG_TAG, "setSrvDelt=" + srvDelt);
        this.srvDelt = srvDelt;
    }

    public void setSrvPhoneStr(String srvPhoneStr) {
        this.srvPhoneStr = srvPhoneStr;
    }

    public void setSrvMailStr(String srvMailStr) {
        this.srvMailStr = srvMailStr;
    }

    public void setSrvRecord(boolean srvRecord) {
        this.srvRecord = srvRecord;
    }
}