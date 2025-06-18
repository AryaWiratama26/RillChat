package com.example.rillchat.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rillchat.R;
import com.example.rillchat.databinding.ActivitySettingsBinding;
import com.example.rillchat.utilities.Constants;
import com.example.rillchat.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    private FirebaseFirestore database;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        
        preferenceManager = new PreferenceManager(getApplicationContext());
        database = FirebaseFirestore.getInstance();
        userId = preferenceManager.getString(Constants.KEY_USER_ID);
        
        loadUserDetails();
        setListeners();
    }
    
    private void loadUserDetails() {
        // Load profile image if available
        String userImage = preferenceManager.getString(Constants.KEY_IMAGE);
        if (userImage != null && !userImage.isEmpty()) {
            binding.imageProfile.setImageBitmap(decodeImage(userImage));
            binding.textChangeImage.setVisibility(View.GONE);
            encodedImage = userImage;
        }
        
        // Set name, email from preferenceManager
        binding.inputName.setText(preferenceManager.getString(Constants.KEY_NAME));
        
        // Get email and NIM from Firestore (NIM is read-only)
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        binding.inputEmail.setText(task.getResult().getString(Constants.KEY_EMAIL));
                        binding.inputNIM.setText(task.getResult().getString("nim"));
                    }
                });
    }
    
    private void setListeners() {
        // Back button in toolbar
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        // Change profile image
        binding.layoutProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        
        // Save changes button
        binding.buttonSaveChanges.setOnClickListener(v -> {
            if (isValidDetails()) {
                updateProfile();
            }
        });
        
        // Password change button
        binding.buttonChangePassword.setOnClickListener(v -> {
            if (isValidPasswordDetails()) {
                updatePassword();
            }
        });
        
        // Logout button
        binding.buttonLogout.setOnClickListener(v -> {
            showToast("Logging out...");
            preferenceManager.clear();
            Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
    
    private void updateProfile() {
        loading(true);
        
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId);
        
        HashMap<String, Object> updates = new HashMap<>();
        
        // Only add fields that have changed
        String currentName = preferenceManager.getString(Constants.KEY_NAME);
        String newName = binding.inputName.getText().toString();
        if (!currentName.equals(newName)) {
            updates.put(Constants.KEY_NAME, newName);
            preferenceManager.putString(Constants.KEY_NAME, newName);
        }
        
        String newEmail = binding.inputEmail.getText().toString();
        updates.put(Constants.KEY_EMAIL, newEmail);
        
        if (encodedImage != null && !encodedImage.equals(preferenceManager.getString(Constants.KEY_IMAGE))) {
            updates.put(Constants.KEY_IMAGE, encodedImage);
            preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
        }
        
        if (!updates.isEmpty()) {
            documentReference.update(updates)
                    .addOnSuccessListener(unused -> {
                        loading(false);
                        showToast("Profile updated successfully!");
                    })
                    .addOnFailureListener(e -> {
                        loading(false);
                        showToast("Unable to update profile: " + e.getMessage());
                    });
        } else {
            loading(false);
            showToast("No changes to save");
        }
    }
    
    private Boolean isValidDetails() {
        if (binding.inputName.getText().toString().trim().isEmpty()) {
            showToast("Enter name");
            return false;
        } else if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter a valid email");
            return false;
        } 
        return true;
    }
    
    private Boolean isValidPasswordDetails() {
        if (binding.inputCurrentPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter current password");
            return false;
        } else if (binding.inputNewPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter new password");
            return false;
        } else if (binding.inputConfirmNewPassword.getText().toString().trim().isEmpty()) {
            showToast("Confirm your new password");
            return false;
        } else if (!binding.inputNewPassword.getText().toString().equals(
                binding.inputConfirmNewPassword.getText().toString())) {
            showToast("New password and confirmation don't match");
            return false;
        } else if (binding.inputNewPassword.getText().toString().length() < 6) {
            showToast("Password must be at least 6 characters");
            return false;
        }
        return true;
    }
    
    private void updatePassword() {
        loadingPassword(true);
        
        // First verify current password is correct
        String currentPassword = binding.inputCurrentPassword.getText().toString();
        
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String storedPassword = task.getResult().getString(Constants.KEY_PASSWORD);
                        
                        if (storedPassword != null && storedPassword.equals(currentPassword)) {
                            // Current password is correct, proceed with update
                            String newPassword = binding.inputNewPassword.getText().toString();
                            
                            HashMap<String, Object> updates = new HashMap<>();
                            updates.put(Constants.KEY_PASSWORD, newPassword);
                            
                            database.collection(Constants.KEY_COLLECTION_USERS)
                                    .document(userId)
                                    .update(updates)
                                    .addOnSuccessListener(unused -> {
                                        loadingPassword(false);
                                        showToast("Password updated successfully");
                                        clearPasswordFields();
                                    })
                                    .addOnFailureListener(e -> {
                                        loadingPassword(false);
                                        showToast("Failed to update password: " + e.getMessage());
                                    });
                        } else {
                            loadingPassword(false);
                            showToast("Current password is incorrect");
                        }
                    } else {
                        loadingPassword(false);
                        showToast("Unable to verify current password");
                    }
                });
    }
    
    private void clearPasswordFields() {
        binding.inputCurrentPassword.setText("");
        binding.inputNewPassword.setText("");
        binding.inputConfirmNewPassword.setText("");
    }
    
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSaveChanges.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSaveChanges.setVisibility(View.VISIBLE);
        }
    }
    
    private void loadingPassword(Boolean isLoading) {
        if (isLoading) {
            binding.buttonChangePassword.setVisibility(View.INVISIBLE);
            binding.progressBarPassword.setVisibility(View.VISIBLE);
        } else {
            binding.progressBarPassword.setVisibility(View.INVISIBLE);
            binding.buttonChangePassword.setVisibility(View.VISIBLE);
        }
    }
    
    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
    
    private Bitmap decodeImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textChangeImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            showToast("Error selecting image");
                        }
                    }
                }
            }
    );
} 