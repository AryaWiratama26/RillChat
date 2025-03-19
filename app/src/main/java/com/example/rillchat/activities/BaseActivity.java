package com.example.rillchat.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.rillchat.utilities.Constants;
import com.example.rillchat.utilities.PreferenceManager;

public class BaseActivity extends AppCompatActivity {

    private DocumentReference documentReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        if (userId != null && !userId.isEmpty()) {
            documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(userId);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (documentReference != null) {
            documentReference.update(Constants.KEY_AVAILABILITY, 0)
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (documentReference != null) {
            documentReference.update(Constants.KEY_AVAILABILITY, 1)
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
        }
    }
}
