package com.example.expressify;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("generate-response")  // Replace with your actual endpoint
    Call<BotResponse> sendMessage(@Body UserMessage userMessage);
}