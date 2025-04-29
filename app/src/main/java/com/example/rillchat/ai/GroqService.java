package com.example.rillchat.ai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface GroqService {

    @Headers({
            "Authorization: Bearer {YOUR_GROQ_API}",
            "Content-Type: application/json"
    })
    @POST("chat/completions")
    Call<GroqResponse> chat(@Body GroqRequest request);
}
