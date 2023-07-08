package com.example.safemedia;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout main_REL_photo,main_REL_voice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        createListeners();
    }

    private void createListeners() {
        main_REL_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PhotoCrypt.class);
                startActivity(intent);
                finish();
            }
        });

        main_REL_voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, VoiceCrypt.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void findViews() {
        main_REL_photo = findViewById(R.id.main_REL_photo);
        main_REL_voice = findViewById(R.id.main_REL_voice);
    }
}