package com.example.rillchat.utilities;



import android.util.Log;

import androidx.annotation.NonNull;

import com.example.rillchat.RillChatApp;

import com.onesignal.OneSignal;
import com.onesignal.notifications.INotificationReceivedEvent;
import com.onesignal.user.subscriptions.IPushSubscription;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Utility methods for sending OneSignal push notifications an  d
 * working with the new User-centric API introduced in SDK 5.x.
 *
 * IMPORTANT:
 *  • Replace ONESIGNAL_APP_ID  with your real App ID (Keys & IDs page)
 *  • Replace REST_API_KEY      with your REST API Key (Settings → API Keys)
 */
public class OneSignalNotificationHelper {

    private static final String TAG              = "OneSignalHelper";
    private static final String REST_API_KEY     = "os_v2_app_3hwrhguiwrgjxnvxxiazgpn3owdxva4jhpfeeenu644lmgwifreqxg5yesbgq3u3adbsp2j3ynrxetp7o5pcr3qxmaplzix757sl3ei";

    private static final String ONESIGNAL_ENDPOINT = "https://onesignal.com/api/v1/notifications";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient httpClient  = new OkHttpClient();

    /**
     * Send a chat notification to a specific user (identified by external_user_id).
     *
     * @param receiverExternalUserId External user-id of the recipient
     * @param senderName             Name shown in the notification title
     * @param senderId               Your own sender/user id
     * @param message                Chat message body
     * @param senderImage            Base-64 avatar (optional, stored in data payload)
     */
    public static void sendChatNotification(
            @NonNull String receiverExternalUserId,
            @NonNull String senderName,
            @NonNull String senderId,
            @NonNull String message,
            String senderImage) {

        try {
            // First verify the user is subscribed
            if (!isUserSubscribed(receiverExternalUserId)) {
                Log.e(TAG, "User " + receiverExternalUserId + " is not subscribed to notifications");
                return;
            }
            JSONObject payload = new JSONObject()
                    .put("app_id", RillChatApp.ONESIGNAL_APP_ID)
                    .put("include_external_user_ids", new JSONArray().put(receiverExternalUserId))
                    .put("channel_for_external_user_ids", "push")
                    .put("headings", new JSONObject().put("en", senderName))
                    .put("contents", new JSONObject().put("en", message))
                    .put("data", new JSONObject()
                            .put(Constants.KEY_USER_ID, senderId)
                            .put(Constants.KEY_NAME, senderName)
                            .put(Constants.KEY_MESSAGE, message)
                            .put(Constants.KEY_IMAGE, senderImage));

            RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);

            Request request = new Request.Builder()
                    .url(ONESIGNAL_ENDPOINT)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Basic " + REST_API_KEY)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Failed to send notification", e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Notification sent successfully: " + responseBody);
                    } else {
                        Log.e(TAG, "Failed to send notification: " + response.code() + " - " + responseBody);
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error creating notification payload", e);
        }
    }

    private static boolean isUserSubscribed(String userId) {
        if (userId == null || userId.isEmpty()) {
            return false;
        }

        // Get the current user's subscription status
        IPushSubscription pushSubscription = OneSignal.getUser().getPushSubscription();
        return pushSubscription != null && pushSubscription.getOptedIn();
    }

    /**
     * Get the current user's external_id (null if not yet logged in).
     */
    public static String getExternalUserId() {
        return OneSignal.getUser() != null
                ? OneSignal.getUser().getExternalId()   // new API (SDK 5)
                : null;
    }

    /**
     * Associate your backend user-id with the current OneSignal user context.
     *
     * @param userId your own user identifier
     */
    public static void setExternalUserId(@NonNull String userId) {
        if (!userId.isEmpty()) {
            Log.d(TAG, "Setting OneSignal external user ID: " + userId);
            
            // First logout to clear any existing session
            OneSignal.logout();
            
            // Login with the new user ID
            OneSignal.login(userId);
            
            // Ensure push notifications are opted in
            OneSignal.getUser().getPushSubscription().optIn();
            
            Log.d(TAG, "OneSignal user setup complete for ID: " + userId);
        } else {
            Log.e(TAG, "Cannot set empty external user ID");
        }
    }

    /**
     * Clear the external user ID when logging out
     */
    public static void clearExternalUserId() {
        Log.d(TAG, "Clearing OneSignal external user ID");
        // Opt out of push notifications first
        OneSignal.getUser().getPushSubscription().optOut();
        // Then logout
        OneSignal.logout();
    }
}
