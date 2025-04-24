package com.example.rillchat.activities;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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
    private ObjectAnimator waveAnimator;
    private Handler handler;
    private Runnable loadingRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiChatBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        init();
        setListeners();
        setupWindowInsets();
        startLoadingTextAnimation();
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, null, preferenceManager.getString(Constants.KEY_USER_ID));
        binding.chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> {
            String message = binding.inputMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                if (waveAnimator  != null && waveAnimator.isRunning()) {
                    waveAnimator.cancel();
                }
                binding.tvLoading.setVisibility(View.GONE);
                binding.emojiWave.setVisibility(View.GONE);

                sendUserMessage(message);
                getAIResponse(message);
                binding.inputMessage.setText(null);
            }
        });
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.chatRecyclerView.setPadding(
                    binding.chatRecyclerView.getPaddingLeft(),
                    0,
                    binding.chatRecyclerView.getPaddingRight(),
                    systemBars.bottom
            );
            binding.inputMessage.setPadding(
                    binding.inputMessage.getPaddingLeft(),
                    binding.inputMessage.getPaddingTop(),
                    binding.inputMessage.getPaddingRight(),
                    systemBars.bottom
            );
            return insets;
        });
    }

    private void startLoadingTextAnimation() {
        TextView emojiWave = binding.emojiWave;
        emojiWave.setVisibility(View.VISIBLE);

        waveAnimator = ObjectAnimator.ofFloat(emojiWave, "rotation", 0f, 20f, -20f, 10f, -10f, 0f);
        waveAnimator.setDuration(1000);
        waveAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        waveAnimator.start();
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
        binding.progressBar.setVisibility(View.GONE);
        String systemPrompt = "Saya adalah asisten AI yang mampu membantu menjawab pertanyaan seputar Universitas Pelita Bangsa (UPB)..."; // [Potong untuk ringkas]
        List<GroqRequest.Message> messages = new ArrayList<>();
        messages.add(new GroqRequest.Message("system", systemPrompt));
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
                } else {
                    displayBotMessage("Maaf, saya tidak dapat menjawab saat ini.");
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
        chatMessage.receiverId = Constants.KEY_SENDER_ID;

        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());
    }
}
