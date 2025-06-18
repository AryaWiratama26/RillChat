package com.example.rillchat.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.rillchat.firebase.MessagingService;
import com.example.rillchat.utilities.PreferenceManager;
import com.example.rillchat.utilities.Constants;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Boot completed, checking if user is signed in");
            
            // Check if the user is signed in
            PreferenceManager preferenceManager = new PreferenceManager(context);
            if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
                Log.d(TAG, "User is signed in, starting MessagingService");
                
                // Start the messaging service
                Intent serviceIntent = new Intent(context, MessagingService.class);
                context.startService(serviceIntent);
            }
        }
    }
} 