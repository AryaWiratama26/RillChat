package com.example.rillchat.ai;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GroqClient {
    private static final String BASE_URL = "https://api.groq.com/openai/v1/";
    private static GroqService service;

    public static GroqService getService() {
        if (service == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            service = retrofit.create(GroqService.class);
        }
        return service;
    }
}