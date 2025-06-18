package com.example.rillchat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.animation.ObjectAnimator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.rillchat.R;
import com.example.rillchat.databinding.ActivitySplashSignInBinding;
import com.example.rillchat.databinding.ActivitySignInBinding;
import com.example.rillchat.utilities.Constants;
import com.example.rillchat.utilities.PreferenceManager;
import com.example.rillchat.utilities.OneSignalNotificationHelper;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {

    private ActivitySplashSignInBinding binding;
    private ActivitySignInBinding signInBinding;
    private PreferenceManager preferenceManager;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private ImageView logoImageView;
    private View swipeUpContainer;
    private ObjectAnimator blinkAnimator;
    private boolean isBottomSheetExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        binding = ActivitySplashSignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize views
        logoImageView = binding.logoImage;
        swipeUpContainer = binding.swipeUpContainer;
        
        // Get the included layout
        View bottomSheetView = binding.bottomSheet.getRoot();
        signInBinding = ActivitySignInBinding.bind(bottomSheetView);
        
        setupBottomSheet();
        setupBlinkAnimation();
        setListeners();
    }

    private void setupBlinkAnimation() {
        blinkAnimator = ObjectAnimator.ofFloat(swipeUpContainer, "alpha", 1f, 0.3f);
        blinkAnimator.setDuration(1000);
        blinkAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        blinkAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        startBlinkAnimation();
    }

    private void startBlinkAnimation() {
        if (!isBottomSheetExpanded) {
            blinkAnimator.start();
        }
    }

    private void stopBlinkAnimation() {
        blinkAnimator.cancel();
    }

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.getRoot());
        
        // Configure the bottom sheet behavior
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setPeekHeight(100); // Set a fixed peek height for now
        bottomSheetBehavior.setDraggable(true);
        bottomSheetBehavior.setFitToContents(true);
        bottomSheetBehavior.setSkipCollapsed(false);
        
        // Set the maximum height to full screen
        binding.bottomSheet.getRoot().post(() -> {
            int windowHeight = getWindow().getDecorView().getHeight();
            bottomSheetBehavior.setMaxHeight(windowHeight);
        });

        // Add callback to handle state changes and animations
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheet.setTranslationY(0);
                    isBottomSheetExpanded = true;
                    stopBlinkAnimation();
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    isBottomSheetExpanded = false;
                    swipeUpContainer.setAlpha(1f);
                    startBlinkAnimation();
                }
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {
                // Animate logo scale based on slide offset
                float scale = 1.0f - (slideOffset * 0.5f);
                logoImageView.setScaleX(scale);
                logoImageView.setScaleY(scale);
                
                // Adjust logo vertical position
                float translationY = -slideOffset * 100;
                logoImageView.setTranslationY(translationY);

                // Animate swipe up container
                if (slideOffset > 0) {
                    stopBlinkAnimation();
                    // Follow bottom sheet movement and fade out
                    float containerTranslationY = slideOffset * 100;
                    float containerAlpha = 1.0f - (slideOffset * 2f); // Fade out faster
                    swipeUpContainer.setTranslationY(containerTranslationY);
                    swipeUpContainer.setAlpha(Math.max(0, containerAlpha));
                } else {
                    // Reset translation when collapsed
                    swipeUpContainer.setTranslationY(0);
                    swipeUpContainer.setAlpha(1f);
                    startBlinkAnimation();
                }
            }
        });
    }

    private void setListeners() {
        signInBinding.textCreateNewAccount.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));

        signInBinding.buttonSignIn.setOnClickListener(v -> {
            if (isValidSignInDetails()) {
                signIn();
            }
        });
    }

    private void signIn() {
        loading(true);
        String email = signInBinding.inputEmail.getText().toString().trim();
        String password = signInBinding.inputPassword.getText().toString();
        
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        String storedPassword = documentSnapshot.getString(Constants.KEY_PASSWORD);
                        
                        if (storedPassword != null && storedPassword.equals(password)) {
                            String userId = documentSnapshot.getId();
                            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                            preferenceManager.putString(Constants.KEY_USER_ID, userId);
                            preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                            preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                            
                            // Set OneSignal external user ID
                            OneSignalNotificationHelper.setExternalUserId(userId);
                            
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            loading(false);
                            showToast("Invalid password");
                        }
                    } else {
                        loading(false);
                        showToast("No account found with this email");
                    }
                })
                .addOnFailureListener(e -> {
                    loading(false);
                    showToast("Error checking credentials: " + e.getMessage());
                });
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            signInBinding.buttonSignIn.setVisibility(View.INVISIBLE);
            signInBinding.progressBar.setVisibility(View.VISIBLE);
        } else {
            signInBinding.buttonSignIn.setVisibility(View.VISIBLE);
            signInBinding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidSignInDetails() {
        String email = signInBinding.inputEmail.getText().toString().trim();
        String password = signInBinding.inputPassword.getText().toString().trim();
        
        if (email.isEmpty()) {
            showToast("Enter Email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Enter valid email");
            return false;
        } else if (password.isEmpty()) {
            showToast("Enter Password");
            return false;
        } else if (password.length() < 6) {
            showToast("Password must be at least 6 characters");
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (blinkAnimator != null) {
            blinkAnimator.cancel();
        }
    }
}
