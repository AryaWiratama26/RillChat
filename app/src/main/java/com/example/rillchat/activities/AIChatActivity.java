package com.example.rillchat.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.recyclerview.widget.DefaultItemAnimator;

import com.example.rillchat.adapters.ChatAdapter;
import com.example.rillchat.ai.GroqClient;
import com.example.rillchat.ai.GroqRequest;
import com.example.rillchat.ai.GroqResponse;
import com.example.rillchat.databinding.ActivityAiChatBinding;
import com.example.rillchat.models.ChatMessage;
import com.example.rillchat.utilities.Constants;
import com.example.rillchat.utilities.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIChatActivity extends BaseActivity {

    private ActivityAiChatBinding binding;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiChatBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        init();
        setListeners();

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, null, Constants.AI_USER_ID); // Ganti dengan userID kamu
        binding.chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> {
            String message = binding.inputMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendUserMessage(message);
                getAIResponse(message);
                binding.inputMessage.setText(null);
            }
        });
    }

    private void sendUserMessage(String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessage(message);
        chatMessage.setDateTime(getCurrentTime());
        chatMessage.setFromAI(false);
        chatMessage.senderId = preferenceManager.getString(Constants.KEY_USER_ID);
        chatMessage.receiverId = Constants.AI_ID;

        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
        binding.chatRecyclerView.setVisibility(View.VISIBLE);
        binding.chatRecyclerView.setItemAnimator(new DefaultItemAnimator());

    }

    private void getAIResponse(String userMessage) {
        binding.progressBar.setVisibility(View.VISIBLE);

        List<GroqRequest.Message> messages = new ArrayList<>();
        messages.add(new GroqRequest.Message("user", userMessage));

        GroqRequest request = new GroqRequest();
        request.messages = messages;

        GroqClient.getService().chat(request).enqueue(new Callback<GroqResponse>() {
            @Override
            public void onResponse(Call<GroqResponse> call, Response<GroqResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    String responseText = response.body().choices.get(0).message.content;
                    displayBotMessage(responseText);
                }
            }

            @Override
            public void onFailure(Call<GroqResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e("GroqAI", "Error: " + t.getMessage());
                displayBotMessage("Maaf, terjadi kesalahan. Silakan coba lagi.");
            }
        });
    }

    private void displayBotMessage(String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessage(message);
        chatMessage.setDateTime(getCurrentTime());
        chatMessage.setFromAI(true);
        chatMessage.senderId = Constants.AI_ID;
        chatMessage.receiverId = preferenceManager.getString(Constants.KEY_USER_ID);

        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());
    }
}
