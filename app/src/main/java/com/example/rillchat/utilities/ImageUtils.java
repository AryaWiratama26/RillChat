package com.example.rillchat.utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

public class ImageUtils {
    public static Bitmap getBitmapFromEncodedString(String encodedImage) {
        if (encodedImage == null || encodedImage.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encodeImage(Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            android.graphics.Bitmap.CompressFormat format = android.graphics.Bitmap.CompressFormat.JPEG;
            int quality = 30; // Compress quality
            java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
            bitmap.compress(format, quality, stream);
            byte[] imageBytes = stream.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
} 