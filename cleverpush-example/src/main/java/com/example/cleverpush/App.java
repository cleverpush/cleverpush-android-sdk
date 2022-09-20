package com.example.cleverpush;

import android.app.Application;
import com.cleverpush.CleverPush;
import com.google.firebase.FirebaseApp;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
