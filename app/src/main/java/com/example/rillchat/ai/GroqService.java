package com.example.rillchat.ai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface GroqService {

    @Headers({
            "Authorization: Bearer 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",
            "Content-Type: application/json"
    })
    @POST("chat/completions")
    Call<GroqResponse> chat(@Body GroqRequest request);
}