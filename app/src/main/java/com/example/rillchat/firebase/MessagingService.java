package com.example.rillchat.firebase;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.rillchat.R;
import com.example.rillchat.activities.ChatActivity;
import com.example.rillchat.activities.MainActivity;
import com.example.rillchat.models.User;
import com.example.rillchat.utilities.Constants;
import com.example.rillchat.utilities.OneSignalNotificationHelper;
import com.example.rillchat.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.onesignal.OneSignal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MessagingService extends FirebaseMessagingService {
    private static final String TAG = "MessagingService";
    private static final String CHANNEL_ID = "chat_notifications";
    private ListenerRegistration chatListener;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MessagingService onCreate");
        createNotificationChannel();
        setupChatListener();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove the listener when service is destroyed
        if (chatListener != null) {
            chatListener.remove();
        }
    }
    
    // Listen for new messages in the background
    private void setupChatListener() {
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        
        if (currentUserId != null && !currentUserId.isEmpty()) {
            Log.d(TAG, "Setting up chat listener for user: " + currentUserId);
            
            // Listen for new messages where the current user is the receiver
            chatListener = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, currentUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed", error);
                        return;
                    }
                    
                    Log.d(TAG, "Received database update. Document count: " + 
                        (snapshots != null ? snapshots.size() : 0));
                    
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : snapshots) {
                            // Only process new documents
                            if (document.getMetadata().hasPendingWrites()) {
                                Log.d(TAG, "Skipping local change");
                                continue; // Skip local changes
                            }
                            
                            // Get sender info
                            String senderId = document.getString(Constants.KEY_SENDER_ID);
                            if (senderId == null) {
                                Log.d(TAG, "Sender ID is null in document: " + document.getId());
                                continue;
                            }
                            
                            String message = document.getString(Constants.KEY_MESSAGE);
                            Log.d(TAG, "New message from " + senderId + ": " + 
                                (message != null ? (message.length() > 20 ? message.substring(0, 20) + "..." : message) : "null"));
                            
                            // Check if app is in foreground and on chat screen with this sender
                            if (!isAppInForeground() || !isChatWithSender(senderId)) {
                                // Fetch sender info and show notification only if not actively chatting
                                fetchSenderAndShowNotification(senderId, message);
                            } else {
                                Log.d(TAG, "App is in foreground and user is chatting with sender. Skipping notification.");
                            }
                        }
                    }
                });
        } else {
            Log.e(TAG, "Can't set up listener - user ID is null or empty");
        }
    }
    
    private boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
        if (runningProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(getPackageName())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    private boolean isChatWithSender(String senderId) {
        // This is a simplified check - in a real app you would need to check
        // the current activity and any extras to see if user is chatting with this person
        // For now, we'll always return false to ensure notifications are shown
        return false;
    }
    
    private void fetchSenderAndShowNotification(String senderId, String message) {
        if (senderId == null || message == null) {
            Log.e(TAG, "Cannot fetch sender - invalid senderId or message");
            return;
        }
        
        Log.d(TAG, "Fetching sender info for ID: " + senderId);
        
        FirebaseFirestore.getInstance()
            .collection(Constants.KEY_COLLECTION_USERS)
            .document(senderId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = new User();
                    user.id = senderId;
                    user.name = documentSnapshot.getString(Constants.KEY_NAME);
                    user.token = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                    user.image = documentSnapshot.getString(Constants.KEY_IMAGE);
                    
                    Log.d(TAG, "Found sender: " + user.name + ", preparing to send OneSignal notification");
                    
                    // Get current user's ID (receiver)
                    PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    
                    if (currentUserId != null && !currentUserId.isEmpty()) {
                        Log.d(TAG, "Sending OneSignal notification to user: " + currentUserId);
                        // Send notification via OneSignal
                        OneSignalNotificationHelper.sendChatNotification(
                            currentUserId,  // receiver's external user ID
                            user.name,      // sender's name
                            user.id,        // sender's ID
                            message,        // message content
                            user.image      // sender's image
                        );
                    } else {
                        Log.e(TAG, "Cannot send notification: Current user ID is null or empty");
                    }
                } else {
                    Log.e(TAG, "Sender document does not exist");
                }
            })
            .addOnFailureListener(e -> Log.e(TAG, "Error fetching sender details", e));
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "========== FCM Token Information ==========");
        Log.d(TAG, "New FCM token received: " + token);
        Log.d(TAG, "Token length: " + token.length());
        Log.d(TAG, "Token timestamp: " + System.currentTimeMillis());
        Log.d(TAG, "=========================================");
        
        // Update token in Firebase when a new token is generated
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Log.d(TAG, "User is signed in - updating token in Firestore");
            updateToken(token);
            
            // Also update OneSignal external user ID
            String userId = preferenceManager.getString(Constants.KEY_USER_ID);
            if (userId != null && !userId.isEmpty()) {
                Log.d(TAG, "Setting OneSignal external user ID: " + userId);
                OneSignalNotificationHelper.setExternalUserId(userId);
            }
        } else {
            Log.d(TAG, "User is not signed in - token update skipped");
        }
    }
    
    private void updateToken(String token) {
        try {
            PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            String userId = preferenceManager.getString(Constants.KEY_USER_ID);
            if (userId != null && !userId.isEmpty()) {
                Log.d(TAG, "Updating FCM token for user: " + userId);
                DocumentReference documentReference = 
                        database.collection(Constants.KEY_COLLECTION_USERS).document(userId);
                documentReference.update(Constants.KEY_FCM_TOKEN, token)
                        .addOnSuccessListener(unused -> {
                            Log.d(TAG, "FCM token successfully updated in Firestore");
                            // Save token in preferences
                            preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
                            Log.d(TAG, "FCM token saved in SharedPreferences");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating FCM token in Firestore", e);
                            Log.e(TAG, "Error details: " + e.getMessage());
                        });
            } else {
                Log.e(TAG, "Cannot update token - User ID is null or empty");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update token", e);
            Log.e(TAG, "Exception details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        // Create handler for the main thread
        Handler mainHandler = new Handler(Looper.getMainLooper());
        
        // Process message on main thread
        mainHandler.post(() -> {
            try {
                Log.d(TAG, "Received remote message from: " + remoteMessage.getFrom());
                Log.d(TAG, "Message data: " + remoteMessage.getData());
                
                // Extract data from the message
                User user = new User();
                user.id = remoteMessage.getData().get(Constants.KEY_USER_ID);
                user.name = remoteMessage.getData().get(Constants.KEY_NAME);
                user.token = remoteMessage.getData().get(Constants.KEY_FCM_TOKEN);
                user.image = remoteMessage.getData().get(Constants.KEY_IMAGE);
                String messageText = remoteMessage.getData().get(Constants.KEY_MESSAGE);
                
                if (user.id == null || user.name == null || messageText == null) {
                    Log.e(TAG, "Invalid notification data received");
                    return;
                }
                
                // Check if app is in foreground and on chat screen with this sender
                if (!isAppInForeground() || !isChatWithSender(user.id)) {
                    showNotification(user, messageText);
                } else {
                    Log.d(TAG, "App is in foreground and user is chatting with sender. Skipping notification.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing FCM message", e);
            }
        });
    }
    
    private void showNotification(User user, String messageText) {
        if (user == null || messageText == null) {
            Log.e(TAG, "Cannot show notification - user or message is null");
            return;
        }
        
        Log.d(TAG, "Showing notification from " + user.name + ": " + messageText);
        
        // Intent to open ChatActivity
        Intent intent = new Intent(this, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Constants.KEY_USER, user);

        // Fallback intent if user doesn't exist
        Intent fallbackIntent = new Intent(this, MainActivity.class);
        fallbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Create unique request code based on sender ID to avoid PendingIntent collisions
        int requestCode = user.id.hashCode();
        
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(
                    this, requestCode, intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
        } else {
            pendingIntent = PendingIntent.getActivity(
                    this, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_rillchat_upb_fixed_foreground)
                .setContentTitle(user.name)
                .setContentText(messageText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Add user's profile image
        if (user.image != null && !user.image.isEmpty()) {
            Bitmap profileImage = decodeImage(user.image);
            if (profileImage != null) {
                builder.setLargeIcon(profileImage);
            }
        }

        // Generate unique notification ID for this sender
        int notificationId = user.id.hashCode();
        
        // Show the notification
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            // Check for notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= 33) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) 
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, builder.build());
                    Log.d(TAG, "Notification shown with ID: " + notificationId);
                } else {
                    Log.e(TAG, "Notification permission not granted!");
                }
            } else {
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "Notification shown with ID: " + notificationId);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when showing notification", e);
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Chat Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Notifications for new chat messages");
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.setShowBadge(true);
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created successfully");
                } else {
                    Log.e(TAG, "NotificationManager is null");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel", e);
            }
        }
    }

    // Decode base64 string to Bitmap
    private Bitmap decodeImage(String encodedImage) {
        try {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding image", e);
            return null;
        }
    }

    // Add this method to handle chat message sending
    public static void sendChatMessageNotification(String receiverId, String senderName, String senderId, String message, String senderImage) {
        Log.d(TAG, "Attempting to send chat notification to: " + receiverId);
        OneSignalNotificationHelper.sendChatNotification(
            receiverId,
            senderName,
            senderId,
            message,
            senderImage
        );
    }
}
