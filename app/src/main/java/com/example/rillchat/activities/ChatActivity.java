package com.example.rillchat.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.rillchat.R;
import com.example.rillchat.adapters.ChatAdapter;
import com.example.rillchat.databinding.ActivityChatBinding;
import com.example.rillchat.models.ChatMessage;
import com.example.rillchat.models.User;
import com.example.rillchat.utilities.Constants;
import com.example.rillchat.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentChange;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversionId = null;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;

    private Uri imageUri;




    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        EditText inputMessage = findViewById(R.id.inputMessage);
        ImageView btnSend = findViewById(R.id.btnSend);
        ImageView btnCamera = findViewById(R.id.btnCamera);


        loadReceiverDetails();
        init();
        setListeners();
        listenMessages();

        inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    btnSend.setVisibility(View.GONE);
                    btnCamera.setVisibility(View.VISIBLE);
                } else {
                    btnSend.setVisibility(View.VISIBLE);
                    btnCamera.setVisibility(View.GONE);
                }
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.chatRecyclerView.setPadding(
                    binding.chatRecyclerView.getPaddingLeft(),
                    0,
                    binding.chatRecyclerView.getPaddingRight(),
                    systemBars.bottom
            );
            binding.inputMessage.setPadding(
                    binding.inputMessage.getPaddingLeft(),
                    binding.inputMessage.getPaddingTop(),
                    binding.inputMessage.getPaddingRight(),
                    systemBars.bottom
            );
            return insets;
        });
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();

        Bitmap receiverImage = null;
        if (receiverUser != null && receiverUser.image != null) {
            receiverImage = getBitmapFromEncodedString(receiverUser.image);
        }

        chatAdapter = new ChatAdapter(
                chatMessages,
                receiverImage,
                preferenceManager.getString(Constants.KEY_USER_ID),
                this
        );

        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        if (encodedImage == null || encodedImage.isEmpty()) return null;
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        if (receiverUser != null) {
            binding.textName.setText(receiverUser.name);
        }
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.btnSend.setOnClickListener(v -> sendMessage());
        binding.btnCamera.setOnClickListener(v -> {
            String[] options = {"Ambil Foto", "Pilih dari Galeri"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Pilih Sumber Gambar")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            checkPermissions(true);
                        } else {
                            checkPermissions(false);
                        }
                    }).show();
        });
    }
    private void checkPermissions(boolean forCamera) {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            if (forCamera) openCamera();
            else openGallery();
        }
    }

    public String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String, Object> conversion) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATION)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATION).document(conversionId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

    private void checkForConversion() {
        if(!chatMessages.isEmpty()) {
            checkForConversionRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.id
            );
            checkForConversionRemotely(
                    receiverUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

   private void checkForConversionRemotely(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATION)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
   }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
      if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
          DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
          conversionId = documentSnapshot.getId();
        }
    };

    private void sendMessage() {
        String userMessage = binding.inputMessage.getText().toString().trim();
        if (userMessage.isEmpty()) {
            return;
        }
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, userMessage);
        message.put(Constants.KEY_TIMESTAMP, new Date());

        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        if(conversionId != null) {
            updateConversion(userMessage);
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE, userMessage);
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        binding.inputMessage.setText(null);
    }

    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);

        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error) -> {
            if(error != null) {
                return;
            }

            if(value != null) {
                if(value.getLong(Constants.KEY_AVAILABILITY) != null) {
                    int availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    isReceiverAvailable = availability == 1;
                }
                receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
            }
            if(isReceiverAvailable) {
                binding.textAvailability.setVisibility(View.VISIBLE);
            }else {
                binding.textAvailability.setVisibility(View.GONE);
            }
        });
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }

            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }

            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }

        binding.progressBar.setVisibility(View.GONE);
        if (conversionId == null) {
            checkForConversion();
        }
    };

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = File.createTempFile("IMG_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(cameraIntent, REQUEST_CAMERA);
            }
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    private void sendImageMessage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            message.put(Constants.KEY_MESSAGE, encodedImage);
            message.put(Constants.KEY_TIMESTAMP, new Date());

            database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && imageUri != null) {
                sendImageMessage(imageUri);
            } else if (requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
                sendImageMessage(data.getData());
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Izin dibutuhkan untuk melanjutkan", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override 
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}





