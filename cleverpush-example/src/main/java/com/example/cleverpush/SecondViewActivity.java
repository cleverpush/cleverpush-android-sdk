package com.example.cleverpush;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.cleverpush.databinding.ActivityMainBinding;

public class SecondViewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_second_view);

//        Intent deepLinkingIntent= getIntent();
//        deepLinkingIntent.getScheme();
//        deepLinkingIntent.getData().getPath();
    }
}