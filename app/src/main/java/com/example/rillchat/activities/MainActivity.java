package com.example.rillchat.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.style.BackgroundColorSpan;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.rillchat.R;
import com.example.rillchat.adapters.RecentConversationAdapter;
import com.example.rillchat.databinding.ActivityMainBinding;
import com.example.rillchat.firebase.MessagingService;
import com.example.rillchat.listeners.ConversionListener;
import com.example.rillchat.models.ChatMessage;
import com.example.rillchat.models.User;
import com.example.rillchat.utilities.Constants;
import com.example.rillchat.utilities.PreferenceManager;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.example.rillchat.utilities.OneSignalNotificationHelper;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversionListener {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationAdapter conversationAdapter;
    private FirebaseFirestore database;
    private ExtendedFloatingActionButton fabMenu;
    private ExtendedFloatingActionButton fabNewChat;
    private ExtendedFloatingActionButton fabAIChat;
    private boolean isFabMenuExpanded = false;
    private List<ChatMessage> allConversations = new ArrayList<>(); // Store all for search
    private String mainSearchQuery = "";
    private TextView noResultsTextView;

    // Declare the launcher at the top of your Activity/Fragment:
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted
                    showToast("Notification permission granted");
                } else {
                    // Permission is denied
                    showToast("Notifications disabled. You won't receive message alerts.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        
        // First set content view
        setContentView(binding.getRoot());
        
        // Configure EdgeToEdge with system bars visible
        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        
        // Apply status bar color AFTER EdgeToEdge and setContentView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.biru_tua_upb, getTheme()));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.biru_tua_upb));
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
        
        // Set up immersive mode
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        
        preferenceManager = new PreferenceManager(getApplicationContext());
        
        // Initialize FABs first
        fabMenu = findViewById(R.id.fabMenu);
        fabNewChat = findViewById(R.id.fabNewChat);
        fabAIChat = findViewById(R.id.fabAIChat);
        
        init();
        loadUsersDetails();
        getToken();
        setListeners();
        listenConversations();
        setupBottomNavigation();
        
        // Request notification permission for Android 13+
        askNotificationPermission();
        
        // Start the messaging service explicitly
        startMessagingService();
            
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Add a TextView for 'No results found' (programmatically)
        noResultsTextView = new TextView(this);
        noResultsTextView.setText("No results found");
        noResultsTextView.setTextColor(Color.GRAY);
        noResultsTextView.setTextSize(16);
        noResultsTextView.setVisibility(View.GONE);
        ((FrameLayout) findViewById(R.id.conversationRecyclerView).getParent()).addView(noResultsTextView);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Re-apply immersive mode when focus is regained
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                
            // Reapply status bar color
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.biru_tua_upb, getTheme()));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.biru_tua_upb));
            }
        }
    }

    private void init() {
        conversations = new ArrayList<>();
        conversationAdapter = new RecentConversationAdapter(conversations, this);
        binding.conversationRecyclerView.setAdapter(conversationAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void setupBottomNavigation() {
        // Set Chats as the default selected tab
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_chats);
        
        // Set up click listener for bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_chats) {
                return true;
            } else if (itemId == R.id.navigation_announce) {
                startActivity(new Intent(MainActivity.this, AnnouncementActivity.class));
                return false;
            } else if (itemId == R.id.navigation_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return false;
            }
            return false;
        });
    }

    private void setListeners() {
        binding.imageSignOut.setOnClickListener(v -> signOut());
        
        // Set up FAB menu click listener
        fabMenu.setOnClickListener(v -> toggleFabMenu());

        // Set up individual FAB click listeners
        fabNewChat.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), UsersActivity.class));
            toggleFabMenu();
        });

        fabAIChat.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AIChatActivity.class);
            startActivity(intent);
            toggleFabMenu();
        });
        
        // WhatsApp-style search bar logic
        binding.searchCardView.setOnClickListener(v -> {
            binding.textSearchHint.setVisibility(View.GONE);
            binding.editTextMainSearch.setVisibility(View.VISIBLE);
            binding.editTextMainSearch.requestFocus();
        });
        binding.editTextMainSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && TextUtils.isEmpty(binding.editTextMainSearch.getText())) {
                binding.editTextMainSearch.setVisibility(View.GONE);
                binding.textSearchHint.setVisibility(View.VISIBLE);
                filterConversations("");
            }
        });
        binding.editTextMainSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterConversations(s.toString());
                if (TextUtils.isEmpty(s)) {
                    binding.editTextMainSearch.setVisibility(View.GONE);
                    binding.textSearchHint.setVisibility(View.VISIBLE);
                } else {
                    binding.editTextMainSearch.setVisibility(View.VISIBLE);
                    binding.textSearchHint.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUsersDetails() {
        binding.textName.setText("Chats");
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenConversations() {
        database.collection(Constants.KEY_COLLECTION_CONVERSATION)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATION)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
      if (error != null) {
          return;
      }
      if (value != null) {
          for (DocumentChange documentChange : value.getDocumentChanges()) {
              if(documentChange.getType() == DocumentChange.Type.ADDED) {
                  String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                  String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                  ChatMessage chatMessage = new ChatMessage();
                  chatMessage.senderId = senderId;
                  chatMessage.receiverId = receiverId;
                  if(preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                      chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                      chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                      chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                  } else {
                      chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                      chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                      chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                  }
                  chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                  chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                  conversations.add(chatMessage);
              } else {
                  for (int i = 0; i < conversations.size(); i++) {
                      String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                      String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                      if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)) {
                          conversations.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                          conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                          break;
                      }
                  }
              }
          }
          Collections.sort(conversations, (obj1, obj2) -> obj2.dateObject.compareTo(obj2.dateObject));
          // Store all conversations for search
          allConversations.clear();
          allConversations.addAll(conversations);
          filterConversations(mainSearchQuery);
          binding.conversationRecyclerView.smoothScrollToPosition(0);
          binding.conversationRecyclerView.setVisibility(View.VISIBLE);
          binding.progressBar.setVisibility(View.GONE);
      }
    };

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != 
                    PackageManager.PERMISSION_GRANTED) {
                // Permission not yet granted, request it
                Log.d("NOTIFICATION_PERMISSION", "Requesting POST_NOTIFICATIONS permission");
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d("NOTIFICATION_PERMISSION", "POST_NOTIFICATIONS permission already granted");
            }
        } else {
            Log.d("NOTIFICATION_PERMISSION", "POST_NOTIFICATIONS permission not required for this Android version");
        }
    }

    private void updateToken(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = 
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                    preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN,token)
                .addOnFailureListener(e -> showToast("Unable to update the Token"));
    }

    private void signOut() {
        showToast("Signing out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = 
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                    preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        updates.put(Constants.KEY_AVAILABILITY, 0);
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    // Clear OneSignal external user ID
                    OneSignalNotificationHelper.clearExternalUserId();
                    // Clear preferences and redirect to sign in
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

    @Override
    public void onConversionClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }

    private void startMessagingService() {
        Intent serviceIntent = new Intent(this, MessagingService.class);
        Log.d("MainActivity", "Explicitly starting MessagingService");
        
        // Use regular service instead of foreground service
        startService(serviceIntent);
    }

    private void toggleFabMenu() {
        isFabMenuExpanded = !isFabMenuExpanded;

        if (isFabMenuExpanded) {
            fabNewChat.show();
            fabAIChat.show();
            fabNewChat.setVisibility(View.VISIBLE);
            fabAIChat.setVisibility(View.VISIBLE);
            fabMenu.setIcon(getDrawable(R.drawable.ic_close));
            fabMenu.setText("Close");
        } else {
            fabNewChat.hide();
            fabAIChat.hide();
            fabNewChat.postDelayed(() -> fabNewChat.setVisibility(View.GONE), 200);
            fabAIChat.postDelayed(() -> fabAIChat.setVisibility(View.GONE), 200);
            fabMenu.setIcon(getDrawable(R.drawable.ic_arrow_up));
            fabMenu.setText("New Chat");
        }
    }

    private void filterConversations(String query) {
        mainSearchQuery = query;
        List<ChatMessage> filtered = new ArrayList<>();
        for (ChatMessage chat : allConversations) {
            boolean nameMatch = chat.conversionName != null && chat.conversionName.toLowerCase().contains(query.toLowerCase());
            boolean messageMatch = chat.message != null && chat.message.toLowerCase().contains(query.toLowerCase());
            if (nameMatch || messageMatch) {
                filtered.add(chat);
            }
        }
        conversationAdapter.setConversations(filtered, query);
        boolean noResults = filtered.isEmpty() && !TextUtils.isEmpty(query);
        binding.conversationRecyclerView.setVisibility(noResults ? View.GONE : View.VISIBLE);
        noResultsTextView.setVisibility(noResults ? View.VISIBLE : View.GONE);
    }

}