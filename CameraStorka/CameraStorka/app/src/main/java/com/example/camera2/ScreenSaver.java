package com.example.camera2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ScreenSaver extends AppCompatActivity {
    public static final String LOG_TAG = "myLogs";
    private Boolean perm = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_saver);

        Log.d(LOG_TAG, "Запрашиваем разрешение");
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (perm == false) {
                    Intent intent = new Intent(ScreenSaver.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                else finish();
            }
        });

        Intent intentSend = new Intent(Intent.ACTION_SEND);
        TextView textView = findViewById(R.id.denied);

        if (intentSend.resolveActivity(getPackageManager()) == null) {
            textView.setText("ПРЕДУПРЕЖДЕНИЕ \n У вас не установлен почтовый клиент. Сообщения по почте отправляться не будут. \n Уведомление об угоне работает только если запись не идёт. Если вы оставите запись включённой, то уведомления приходить не будут.");
        } else
            textView.setText("ПРЕДУПРЕЖДЕНИЕ \n Уведомление об угоне работает только если запись не идёт. Если вы оставите запись включённой, то уведомления приходить не будут.");

        ImageView imageView = findViewById(R.id.noneCamera);
        imageView.setVisibility(View.GONE);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CALL_PHONE}, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        TextView textView = findViewById(R.id.denied);
        if (requestCode == 101) {
            for (int res : grantResults) {
                if (res == PackageManager.PERMISSION_DENIED) {
                    perm = true;
                    textView.setText("Не удалось запустить приложение, так как не получены все разрешения");
                }
            }
        }
    }
}