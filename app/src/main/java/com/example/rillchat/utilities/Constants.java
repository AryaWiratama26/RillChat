package com.example.rillchat.utilities;

import java.util.HashMap;

public class Constants {
    public static final String KEY_COLLECTION_USERS = "users";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_PREFERENCE_NAME = "rillChatPreference";
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_FCM_TOKEN = "fcmToken";
    public static final String KEY_USER = "user";
    public static final String KEY_COLLECTION_CHAT = "chat";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_RECEIVER_ID = "receiverId";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_AVAILABILITY = "availability";
    public static final String KEY_COLLECTION_CONVERSATION = "conversations";
    public static final String KEY_SENDER_NAME = "senderName";
    public static final String KEY_RECEIVER_NAME = "receiverName";
    public static final String KEY_SENDER_IMAGE = "senderImage";
    public static final String KEY_RECEIVER_IMAGE = "receiverImage";
    public static final String KEY_LAST_MESSAGE = "lastMessage";
    public static final String KEY_CAPTION = "caption";
    public static final String AI_ID = "AI_BOT";
    public static final String AI_USER_ID = "AI_User";
    public static final String AI_IMAGE_BASE64 = "base64_encoded_image_string";
    
    // Direct device-to-device notifications will be handled in the app itself
    // without requiring server authentication
    public static final String REMOTE_MSG_CONTENT_TYPE = "Content-Type";
    
    // OneSignal related constants
    public static final String KEY_COLLECTION_USER_TOKENS = "user_tokens";
    public static final String KEY_NOTIFICATION_TYPE = "notificationType";
    public static final String NOTIFICATION_TYPE_CHAT = "chat";
    public static final String NOTIFICATION_TYPE_FRIEND_REQUEST = "friend_request";
    
    public static HashMap<String, String> remoteMsgHeaders = null;
    
    public static HashMap<String, String> getRemoteMsgHeaders() {
        if (remoteMsgHeaders == null) {
            remoteMsgHeaders = new HashMap<>();
            remoteMsgHeaders.put(REMOTE_MSG_CONTENT_TYPE, "application/json");
        }
        return remoteMsgHeaders;
    }
}
