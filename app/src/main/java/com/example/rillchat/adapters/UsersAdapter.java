package com.example.rillchat.adapters;

import static android.media.CamcorderProfile.get;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rillchat.databinding.ItemContainerUserBinding;
import com.example.rillchat.listeners.UserListener;
import com.example.rillchat.models.User;

import java.util.List;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.graphics.Color;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private List<User> users;
    private final UserListener userListener;
    private String highlightQuery = "";

    public UsersAdapter(List<User> users, UserListener userListener) {
        this.users = users;
        this.userListener = userListener;
    }

    public void setUsers(List<User> users, String query) {
        this.users = users;
        this.highlightQuery = query;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new UserViewHolder(itemContainerUserBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersAdapter.UserViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {

        ItemContainerUserBinding binding;
        UserViewHolder(ItemContainerUserBinding itemContainerUserBinding) {
            super(itemContainerUserBinding.getRoot());
            binding = itemContainerUserBinding;
        }

        void setUserData(User user) {
            if (!TextUtils.isEmpty(highlightQuery) && !TextUtils.isEmpty(user.name)) {
                int start = user.name.toLowerCase().indexOf(highlightQuery.toLowerCase());
                if (start >= 0) {
                    SpannableString spannable = new SpannableString(user.name);
                    spannable.setSpan(
                        new BackgroundColorSpan(Color.YELLOW),
                        start,
                        start + highlightQuery.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    binding.textName.setText(spannable);
                } else {
                    binding.textName.setText(user.name);
                }
            } else {
                binding.textName.setText(user.name);
            }
            binding.textEmail.setText(user.email);
            binding.imageProfile.setImageBitmap(getUserImage(user.image));
            binding.getRoot().setOnClickListener(v -> userListener.onUserClicked(user));
        }
    }
    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
