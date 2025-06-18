package com.example.rillchat;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.onesignal.Continue;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

public class RillChatApp extends Application {
    private static final String TAG = "RillChatApp"; // Define TAG for logging
    public static final String ONESIGNAL_APP_ID = "d9ed139a-88b4-4c9b-b6b7-ba01933dbb75";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RillChatApp onCreate");
        
        // Initialize Firebase first
        FirebaseApp.initializeApp(this);
        Log.d(TAG, "Firebase initialized");
        
        // Initialize OneSignal with debugging
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        
        // Initialize OneSignal
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
        
        // Enable notifications by default
        OneSignal.getNotifications().requestPermission(true, Continue.with(r -> {
            if (r.isSuccess()) {
                Log.d(TAG, "Push notification permission granted");
                // Explicitly opt in to push notifications after permission is granted
                OneSignal.getUser().getPushSubscription().optIn();
            } else {
                Log.e(TAG, "Push notification permission denied");
            }
        }));
    }
}

