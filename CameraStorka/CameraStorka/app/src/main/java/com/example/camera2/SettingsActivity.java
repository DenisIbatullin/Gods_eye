package com.example.camera2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREF_PHONE = "Phone";
    private static final String PREF_MAIL = "Mail";
    private static final String PREF_DELT = "Delt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button ok = findViewById(R.id.buttonOk);
        Button cansel = findViewById(R.id.Cansel);
        EditText phone = findViewById(R.id.phoneValue);
        EditText mail = findViewById(R.id.mailValue);
        EditText delt = findViewById(R.id.delt);

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("Mail", mail.getText().toString());
                intent.putExtra("Phone", phone.getText().toString());
                float d = Float.valueOf(delt.getText().toString());
                intent.putExtra("Delt", d);

                setResult(RESULT_OK, intent);
                finish();

            }
        });

        cansel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);
                finish();
            }
        });

        Intent intent = getIntent();
        Log.i("MY_LOG", "Settibgs " + intent.getStringExtra(PREF_PHONE));
        mail.setText(intent.getStringExtra(PREF_MAIL));
        phone.setText(intent.getStringExtra(PREF_PHONE));
        delt.setText(String.valueOf(intent.getFloatExtra("Delt", 6)));
    }
}