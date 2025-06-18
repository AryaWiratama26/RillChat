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