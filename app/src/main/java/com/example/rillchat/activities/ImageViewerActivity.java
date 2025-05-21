package com.example.rillchat.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rillchat.R;
import com.example.rillchat.databinding.ActivityImageViewerBinding;
import com.example.rillchat.utilities.Constants;

public class ImageViewerActivity extends AppCompatActivity {
    private ActivityImageViewerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String image = getIntent().getStringExtra(Constants.KEY_IMAGE);
        String caption = getIntent().getStringExtra(Constants.KEY_CAPTION);

        if (image != null) {
            binding.imageView.setImageBitmap(com.example.rillchat.utilities.ImageUtils.getBitmapFromEncodedString(image));
        }

        if (caption != null && !caption.isEmpty()) {
            binding.textCaption.setVisibility(View.VISIBLE);
            binding.textCaption.setText(caption);
        } else {
            binding.textCaption.setVisibility(View.GONE);
        }

        binding.imageBack.setOnClickListener(v -> onBackPressed());
    }
} 