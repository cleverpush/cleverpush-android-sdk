package com.example.cleverpush;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.cleverpush.CleverPush;
import com.cleverpush.util.Logger;
import com.example.cleverpush.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    String url = "https://cleverpush.com/en/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        Logger.i("CHECKLOG", "MainActivity block L42");
        binding.btnUnsubscribe.setOnClickListener(view -> {
            CleverPush.getInstance(MainActivity.this).unsubscribe();
            binding.tvStatus.setText("Unsubscribe");
        });

        binding.btnTopicsDialog.setOnClickListener(view -> {
            CleverPush.getInstance(MainActivity.this).showTopicsDialog();
            binding.tvStatus.setText("ShowTopicsDialog");
        });

        binding.btnGetId.setOnClickListener(view -> {
            try {
                CleverPush.getInstance(MainActivity.this).getSubscriptionId(this);
                binding.tvStatus.setText(CleverPush.getInstance(MainActivity.this).getSubscriptionId(this));
            } catch (Exception e) {
                Toast.makeText(this, "Please subscribe first", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
