package com.example.rillchat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rillchat.R;
import com.example.rillchat.databinding.ActivityAnnouncementBinding;
import com.example.rillchat.models.Announcement;
import com.example.rillchat.utilities.Constants;
import com.example.rillchat.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementActivity extends BaseActivity {

    private ActivityAnnouncementBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private List<Announcement> announcements;
    private AnnouncementAdapter announcementAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnnouncementBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        
        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        binding.textTitle.setText("University Announcements");
        getAnnouncements();
        setupBottomNavigation();
        
        // Set up immersive mode
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Set bottom padding to 0
            return insets;
        });
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Re-apply immersive mode when focus is regained
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void init() {
        announcements = new ArrayList<>();
        announcementAdapter = new AnnouncementAdapter(announcements);
        binding.announcementRecyclerView.setAdapter(announcementAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void setupBottomNavigation() {
        // Set Announce as the selected tab
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_announce);
        
        // Set up click listener for bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_chats) {
                startActivity(new Intent(AnnouncementActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_announce) {
                return true;
            } else if (itemId == R.id.navigation_settings) {
                startActivity(new Intent(AnnouncementActivity.this, SettingsActivity.class));
                finish();
            }
            return false;
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void getAnnouncements() {
        loading(true);
        // Example: fetch from Firestore - adjust collection name as needed
        database.collection("announcements")
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        announcements.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Announcement announcement = new Announcement();
                            announcement.title = document.getString("title");
                            announcement.content = document.getString("content");
                            announcement.date = document.getDate("date");
                            announcement.postedBy = document.getString("postedBy");
                            
                            announcements.add(announcement);
                        }
                        
                        if (announcements.size() > 0) {
                            announcementAdapter.notifyDataSetChanged();
                            binding.announcementRecyclerView.smoothScrollToPosition(0);
                            binding.announcementRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
    }

    private void showErrorMessage() {
        // Implement error message display logic
        // For now, just a toast
        showToast("No announcements available");
    }

    private void loading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    // AnnouncementAdapter inner class for RecyclerView
    class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder> {

        private final List<Announcement> announcements;

        AnnouncementAdapter(List<Announcement> announcements) {
            this.announcements = announcements;
        }

        @Override
        public AnnouncementViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_announcement, parent, false);
            return new AnnouncementViewHolder(view);
        }

        @Override
        public void onBindViewHolder(AnnouncementViewHolder holder, int position) {
            holder.setData(announcements.get(position));
        }

        @Override
        public int getItemCount() {
            return announcements.size();
        }

        class AnnouncementViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView textTitle, textDate, textContent;

            AnnouncementViewHolder(android.view.View itemView) {
                super(itemView);
                textTitle = itemView.findViewById(R.id.textAnnouncementTitle);
                textDate = itemView.findViewById(R.id.textAnnouncementDate);
                textContent = itemView.findViewById(R.id.textAnnouncementContent);
            }

            void setData(Announcement announcement) {
                textTitle.setText(announcement.title);
                textContent.setText(announcement.content);
                
                // Format date
                if (announcement.date != null) {
                    java.text.SimpleDateFormat dateFormat = 
                            new java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault());
                    textDate.setText(dateFormat.format(announcement.date));
                } else {
                    textDate.setText("No date");
                }
                
                // Add click listener for item
                itemView.setOnClickListener(v -> {
                    // Open detail view when clicked
                    // This could be implemented later
                    Toast.makeText(getApplicationContext(), 
                            "Announcement details coming soon!", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
} 