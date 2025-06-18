package com.example.rillchat.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rillchat.databinding.ItemContainerRecentConversationBinding;
import com.example.rillchat.listeners.ConversionListener;
import com.example.rillchat.models.ChatMessage;
import com.example.rillchat.models.User;

import java.util.List;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.graphics.Color;

public class RecentConversationAdapter extends RecyclerView.Adapter<RecentConversationAdapter.ConversionViewHolder> {

    private List<ChatMessage> chatMessages;
    private final ConversionListener conversionListener;
    private String highlightQuery = "";

    public RecentConversationAdapter(List<ChatMessage> chatMessages, ConversionListener conversionListener) {
        this.chatMessages = chatMessages;
        this.conversionListener = conversionListener;
    }

    public void setConversations(List<ChatMessage> chatMessages, String query) {
        this.chatMessages = chatMessages;
        this.highlightQuery = query;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentConversationBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull RecentConversationAdapter.ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversationBinding binding;

        ConversionViewHolder(ItemContainerRecentConversationBinding itemContainerRecentConversationBinding) {
            super(itemContainerRecentConversationBinding.getRoot());
            binding = itemContainerRecentConversationBinding;
        }

        void setData(ChatMessage chatMessage) {
            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
            // Highlight name
            if (!TextUtils.isEmpty(highlightQuery) && !TextUtils.isEmpty(chatMessage.conversionName)) {
                int start = chatMessage.conversionName.toLowerCase().indexOf(highlightQuery.toLowerCase());
                if (start >= 0) {
                    SpannableString spannable = new SpannableString(chatMessage.conversionName);
                    spannable.setSpan(
                        new BackgroundColorSpan(Color.YELLOW),
                        start,
                        start + highlightQuery.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    binding.textName.setText(spannable);
                } else {
                    binding.textName.setText(chatMessage.conversionName);
                }
            } else {
                binding.textName.setText(chatMessage.conversionName);
            }
            // Highlight message
            if (!TextUtils.isEmpty(highlightQuery) && !TextUtils.isEmpty(chatMessage.message)) {
                int start = chatMessage.message.toLowerCase().indexOf(highlightQuery.toLowerCase());
                if (start >= 0) {
                    SpannableString spannable = new SpannableString(chatMessage.message);
                    spannable.setSpan(
                        new BackgroundColorSpan(Color.YELLOW),
                        start,
                        start + highlightQuery.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    binding.textRecentMessage.setText(spannable);
                } else {
                    binding.textRecentMessage.setText(chatMessage.message);
                }
            } else {
                binding.textRecentMessage.setText(chatMessage.message);
            }
            // Check if the message is an image
            if (isBase64Image(chatMessage.message)) {
                binding.imageMessagePreview.setVisibility(View.VISIBLE);
                binding.textRecentMessage.setText("📷 Image");
            } else {
                binding.imageMessagePreview.setVisibility(View.GONE);
            }
            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.id = chatMessage.conversionId;
                user.name = chatMessage.conversionName;
                user.image = chatMessage.conversionImage;
                conversionListener.onConversionClicked(user);
            });
        }
    }

    private Bitmap getConversionImage(String encodedImage) {
        if (encodedImage == null || encodedImage.isEmpty()) return null;
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private boolean isBase64Image(String text) {
        if (text == null || text.isEmpty()) return false;
        try {
            // Check if the text is a valid base64 string and starts with image data
            if (text.length() > 100 && text.matches("^[A-Za-z0-9+/=\\s]+$")) {
                byte[] decodedBytes = Base64.decode(text, Base64.DEFAULT);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, options);
                return options.outMimeType != null && options.outMimeType.startsWith("image/");
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
