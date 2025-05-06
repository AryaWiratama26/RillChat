package com.example.rillchat.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rillchat.databinding.ItemContainerReceivedMessageBinding;
import com.example.rillchat.databinding.ItemContainerSentMessageBinding;
import com.example.rillchat.databinding.ItemContainerReceivedMessageAiBinding;
import com.example.rillchat.models.ChatMessage;

import java.util.List;

import io.noties.markwon.Markwon;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<ChatMessage> chatMessages;
    private final Bitmap receiverProfileImage;
    private final String senderId;
    private final Markwon markwon;

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;
    public static final int VIEW_TYPE_AI = 3;

    public ChatAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId, Context context) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
        this.context = context;
        this.markwon = Markwon.create(context);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(ItemContainerSentMessageBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false), markwon);
        } else if (viewType == VIEW_TYPE_AI) {
            return new AIMessageViewHolder(ItemContainerReceivedMessageAiBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false), markwon);
        } else {
            return new ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false), markwon);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(message);
        } else if (getItemViewType(position) == VIEW_TYPE_AI) {
            ((AIMessageViewHolder) holder).setData(message);
        } else {
            ((ReceivedMessageViewHolder) holder).setData(message, receiverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatMessages.get(position);
        Log.d("ChatAdapter", "senderId: " + senderId + ", message.senderId: " + message.senderId + ", isFromAI: " + message.isFromAI());
        if (message.isFromAI()) {
            return VIEW_TYPE_AI;
        } else if (senderId != null && senderId.equals(message.senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    private static boolean isBase64Image(String text) {
        return text != null && text.length() > 100 && text.matches("^[A-Za-z0-9+/=\\s]+$");
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentMessageBinding binding;
        private final Markwon markwon;

        SentMessageViewHolder(ItemContainerSentMessageBinding binding, Markwon markwon) {
            super(binding.getRoot());
            this.binding = binding;
            this.markwon = markwon;
        }

        void setData(ChatMessage chatMessage) {
            if (isBase64Image(chatMessage.message)) {
                byte[] decodedBytes = Base64.decode(chatMessage.message, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                binding.imageMessage.setVisibility(View.VISIBLE);
                binding.textMessage.setVisibility(View.GONE);
                binding.imageMessage.setImageBitmap(bitmap);

                binding.imageMessage.getLayoutParams().width = 600;
                binding.imageMessage.requestLayout();
            } else {
                binding.imageMessage.setVisibility(View.GONE);
                binding.textMessage.setVisibility(View.VISIBLE);
                markwon.setMarkdown(binding.textMessage, chatMessage.message);
            }

            binding.textDateTime.setText(chatMessage.dateTime);
            binding.textMessage.setOnLongClickListener(v -> {
                copyToClipboard(v.getContext(), chatMessage.message);
                return true;
            });
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageBinding binding;
        private final Markwon markwon;

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding binding, Markwon markwon) {
            super(binding.getRoot());
            this.binding = binding;
            this.markwon = markwon;
        }

        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            if (isBase64Image(chatMessage.message)) {
                byte[] decodedBytes = Base64.decode(chatMessage.message, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                binding.imageMessage.setVisibility(View.VISIBLE);
                binding.textMessage.setVisibility(View.GONE);
                binding.imageMessage.setImageBitmap(bitmap);
            } else {
                binding.imageMessage.setVisibility(View.GONE);
                binding.textMessage.setVisibility(View.VISIBLE);
                markwon.setMarkdown(binding.textMessage, chatMessage.message);
            }

            binding.textDateTime.setText(chatMessage.dateTime);
            binding.imageProfile.setImageBitmap(receiverProfileImage);
            binding.textMessage.setOnLongClickListener(v -> {
                copyToClipboard(v.getContext(), chatMessage.message);
                return true;
            });
        }
    }

    static class AIMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageAiBinding binding;
        private final Markwon markwon;

        AIMessageViewHolder(ItemContainerReceivedMessageAiBinding binding, Markwon markwon) {
            super(binding.getRoot());
            this.binding = binding;
            this.markwon = markwon;
        }

        void setData(ChatMessage chatMessage) {
            markwon.setMarkdown(binding.textMessage, chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);
            binding.textMessage.setOnLongClickListener(v -> {
                copyToClipboard(v.getContext(), chatMessage.message);
                return true;
            });
        }
    }

    private static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Teks disalin ke clipboard", Toast.LENGTH_SHORT).show();
    }
}
