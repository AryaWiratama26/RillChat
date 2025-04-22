package com.example.rillchat.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.recyclerview.widget.DefaultItemAnimator;

import com.example.rillchat.adapters.ChatAdapter;
import com.example.rillchat.ai.GroqClient;
import com.example.rillchat.ai.GroqRequest;
import com.example.rillchat.ai.GroqResponse;
import com.example.rillchat.databinding.ActivityAiChatBinding;
import com.example.rillchat.models.ChatMessage;
import com.example.rillchat.utilities.Constants;
import com.example.rillchat.utilities.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AIChatActivity extends BaseActivity {

    private ActivityAiChatBinding binding;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiChatBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        init();
        setListeners();

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
        chatAdapter = new ChatAdapter(chatMessages, null, preferenceManager.getString(Constants.KEY_USER_ID)); // Ganti dengan userID kamu
        binding.chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> {
            String message = binding.inputMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendUserMessage(message);
                getAIResponse(message);
                binding.inputMessage.setText(null);
            }
        });
    }

    private void sendUserMessage(String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessage(message);
        chatMessage.setDateTime(getCurrentTime());
        chatMessage.setFromAI(false);
        chatMessage.senderId = preferenceManager.getString(Constants.KEY_USER_ID);
        chatMessage.receiverId = Constants.AI_ID;

        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
        binding.chatRecyclerView.setVisibility(View.VISIBLE);
        binding.chatRecyclerView.setItemAnimator(new DefaultItemAnimator());

    }

    private void getAIResponse(String userMessage) {
        binding.progressBar.setVisibility(View.VISIBLE);
        String systemPrompt = "Saya adalah asisten AI yang mampu membantu menjawab pertanyaan seputar Universitas Pelita Bangsa (UPB). Tolong jangan jawab diluar pertanyaan Universitas Pelita Bangsa jika ada Pertanyaan tersebut anda jawab Bukan wewenang aku bang. " +
                "Berikut ini informasi penting: " +
                "# PROFIL\n" +
                "- Nama: Universitas Pelita Bangsa (UPB)\n" +
                "- Berdiri: 2 Agustus 2019 (gabungan STIE & STT Pelita Bangsa)\n" +
                "- Lokasi: Jl. Inspeksi Kalimalang, Tegal Danas, Cikarang Pusat, Bekasi, Jawa Barat\n" +
                "- Luas Lahan: 11.603 m²\n" +
                "- Pendiri: Ir. Moh. Mardiyana, MM\n" +
                "\n" +
                "# VISI & MOTTO\n" +
                "- Visi: Universitas entrepreneur kelas internasional pada 2045\n" +
                "- Motto MEGAH: \n" +
                "  - M: Moral tinggi\n" +
                "  - E: Entrepreneur\n" +
                "  - G: Gigih dalam berkarya\n" +
                "  - A: Ahli di bidangnya\n" +
                "  - H: Harapan bangsa\n" +
                "\n" +
                "# PROGRAM STUDI\n" +
                "- D3: Akuntansi\n" +
                "- S1: Arsitektur, Bimbingan Konseling Pendidikan Islam, Bisnis Digital, Ekonomi Syariah, Hukum, Kewirausahaan, Manajemen, PG-PAUD, PGSD, Teknik Industri, Teknik Informatika, Teknik Lingkungan, Teknik Sipil, Teknologi Hasil Pertanian\n" +
                "- S2: Manajemen\n" +
                "\n" +
                "# FASILITAS\n" +
                "- 125 ruang kuliah\n" +
                "- 20 laboratorium\n" +
                "- Perpustakaan (600 m²)\n" +
                "- Masjid (250 m²)\n" +
                "- Aula (2000 m²)\n" +
                "- 15 ruang dosen tetap\n" +
                "- 6 ruang administrasi\n" +
                "- Sekretariat BEM & UKM\n" +
                "- Kantin, Fasilitas olahraga\n" +
                "- ATM BNI & Mandiri\n" +
                "\n" +
                "# BIAYA KULIAH 2025/2026\n" +
                "## Biaya Awal\n" +
                "- Pendaftaran: Rp500.000\n" +
                "- Jas & PPLK: Rp1.300.000\n" +
                "- Sumbangan Perpustakaan: Rp100.000\n" +
                "\n" +
                "## SPP Bulanan\n" +
                "- FEB: Pagi 350k, Malam 550k, Weekend 650k\n" +
                "- FATEK: Pagi 400k, Malam 650k, Weekend 700k\n" +
                "- FIPHUM/FAI: Pagi 350k, Malam 550k, Weekend 650k\n" +
                "\n" +
                "## Biaya Tambahan\n" +
                "- UTS/UAS: 75k/matkul\n" +
                "- Registrasi per semester: 300k\n" +
                "- KKN/KKP/PLP: 600k\n" +
                "- Skripsi: Bimbingan 1jt, Sidang 1jt\n" +
                "- Yudisium: 250k\n" +
                "- Wisuda: 3jt\n" +
                "\n" +
                "# KONTAK\n" +
                "- Email: humas@pelitabangsa.ac.id\n" +
                "- Telepon: +62 821-8888-2441\n" +
                "- Website: https://humas.pelitabangsa.ac.id"+
                "# KONTAK PROGRAM STUDI\n" +
                "- S2 Manajemen: +62 857-8016-8070\n" +
                "- S1 Manajemen: +62 858-9459-7208\n" +
                "- Akuntansi: +62 852-1497-7280\n" +
                "- Kewirausahaan: +62 896-6522-7695\n" +
                "- Bisnis Digital: +62 815-7929-979\n" +
                "- Arsitektur: +62 878-0544-4499\n" +
                "- Teknik Informatika: +62 838-0700-1945\n" +
                "- Teknik Industri: +62 856-9745-5156\n" +
                "- Teknik Lingkungan: +62 895-4118-5541\n" +
                "- Teknik Sipil: +62 822-8350-5243\n" +
                "- Teknologi Hasil Pertanian: +62 856-9214-5206\n" +
                "- Hukum: +62 897-8821-464\n" +
                "- PGSD: +62 838-3939-3822\n" +
                "- PGPAUD: +62 812-2328-2034\n" +
                "\n" +
                "# KONTAK UMUM & ADMINISTRASI\n" +
                "- Humas: +62 857-7742-1222\n" +
                "- Admin Kampus Bekasi: +62 896-6929-1608\n" +
                "- Admin Kampus Karawang: +62 857-7406-0016\n" +
                "- Kemahasiswaan & Alumni: +62 851-5880-4981\n" +
                "\n" +
                "# KONTAK FAKULTAS\n" +
                "- Fakultas Ekonomi & Bisnis: +62 819-3242-8797\n" +
                "- Fakultas Teknik: +62 895-3211-99169\n" +
                "- Fakultas Agama Islam: +62 812-8722-0806\n" +
                "- FIPHUM: +62 821-1258-8025\n" +
                "\n" +
                "# KONTAK UNIT PENUNJANG\n" +
                "- PMB 1: +62 812-2900-0087\n" +
                "- PMB 2: +62 895-2934-6042\n" +
                "- Tata Usaha: +62 877-3880-4153\n" +
                "- MARCOM: +62 812-9869-1172\n" +
                "- Perpustakaan: +62 877-3880-4132\n" +
                "- SDM: +62 813-9814-6030\n" +
                "- Admin Rektorat: +62 895-1251-6640\n" +
                "- DPPM: +62 878-8701-1827\n" +
                "- Sewa Ruang: +62 857-1145-0474\n" +
                "- Keamanan: +62 813-8940-4111\n" +
                "- Direktorat TI: +62 815-1124-0438\n" +
                "- Sarana & Prasarana: +62 877-9000-6136\n" +
                "- Akademik: +62 838-4123-5812\n" +
                "- DPM: +62 878-5551-8855\n" +
                "- Keuangan: +62 851-5640-9450";
        List<GroqRequest.Message> messages = new ArrayList<>();
        messages.add(new GroqRequest.Message("system", systemPrompt));
        messages.add(new GroqRequest.Message("user", userMessage));

        GroqRequest request = new GroqRequest();
        request.messages = messages;

        GroqClient.getService().chat(request).enqueue(new Callback<GroqResponse>() {
            @Override
            public void onResponse(Call<GroqResponse> call, Response<GroqResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    String responseText = response.body().choices.get(0).message.content;
                    displayBotMessage(responseText);
                } else {
                    displayBotMessage("Maaf, saya tidak dapat menjawab saat ini.");
                }
            }

            @Override
            public void onFailure(Call<GroqResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e("GroqAI", "Error: " + t.getMessage());
                displayBotMessage("Maaf, terjadi kesalahan. Silakan coba lagi.");
            }
        });
    }

    private void displayBotMessage(String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessage(message);
        chatMessage.setDateTime(getCurrentTime());
        chatMessage.setFromAI(true);
        chatMessage.senderId = Constants.AI_ID;
        chatMessage.receiverId = Constants.KEY_SENDER_ID;

        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());
    }
}