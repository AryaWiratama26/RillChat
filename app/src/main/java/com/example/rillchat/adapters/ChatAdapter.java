package com.example.rillchat.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
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
    private Context context;

    private final List<ChatMessage> chatMessages;
    private final Bitmap receiverProfileImage;
    private final String senderId;
    private final Markwon markwon; // Keep markwon as a field

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;
    public static final int VIEW_TYPE_AI = 3;

    public ChatAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId, Context context) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
        this.context = context;
        this.markwon = Markwon.create(context); // Initialize markwon here
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            ItemContainerSentMessageBinding binding = ItemContainerSentMessageBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new SentMessageViewHolder(binding, markwon); // Pass markwon to the ViewHolder
        } else if (viewType == VIEW_TYPE_AI) {
            ItemContainerReceivedMessageAiBinding binding = ItemContainerReceivedMessageAiBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new AIMessageViewHolder(binding, markwon); // Pass markwon to the ViewHolder
        } else {
            ItemContainerReceivedMessageBinding binding = ItemContainerReceivedMessageBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new ReceivedMessageViewHolder(binding, markwon); // Pass markwon to the ViewHolder
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



    static class SentMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerSentMessageBinding binding;
        private final Markwon markwon; // Keep markwon as a field

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding, Markwon markwon) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
            this.markwon = markwon; // Initialize markwon here
        }

        void setData(ChatMessage chatMessage) {
            markwon.setMarkdown(binding.textMessage, chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);

            binding.textMessage.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Text", chatMessage.message);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(v.getContext(), "Teks disalin ke clipboard", Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerReceivedMessageBinding binding;
        private final Markwon markwon; // Keep markwon as a field

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding, Markwon markwon) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
            this.markwon = markwon; // Initialize markwon here
        }

        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            markwon.setMarkdown(binding.textMessage, chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);
            binding.imageProfile.setImageBitmap(receiverProfileImage);

            binding.textMessage.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Text", chatMessage.message);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(v.getContext(), "Teks disalin ke clipboard", Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    static class AIMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerReceivedMessageAiBinding binding;
        private final Markwon markwon; // Keep markwon as a field

        AIMessageViewHolder(ItemContainerReceivedMessageAiBinding binding, Markwon markwon) {
            super(binding.getRoot());
            this.binding = binding;
            this.markwon = markwon; // Initialize markwon here
        }

        void setData(ChatMessage chatMessage) {
            markwon.setMarkdown(binding.textMessage, chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);

            binding.textMessage.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Text", chatMessage.message);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(v.getContext(), "Teks disalin ke clipboard", Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }
}
