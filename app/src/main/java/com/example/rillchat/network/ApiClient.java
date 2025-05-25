package com.example.rillchat.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiClient {
    
    private static Retrofit retrofit = null;
    
    // Update this to your Cloud Functions URL after deployment
    // The format will be: https://us-central1-YOUR_PROJECT_ID.cloudfunctions.net/sendNotification
    // Example: https://us-central1-rillchat.cloudfunctions.net/sendNotification
    private static final String CLOUD_FUNCTION_URL = "https://us-central1-YOUR_PROJECT_ID.cloudfunctions.net/";

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Add logging for debugging
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // Add longer timeouts for Cloud Functions cold start
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();
            
            retrofit = new Retrofit.Builder()
                    .baseUrl(CLOUD_FUNCTION_URL)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }

        return retrofit;
    }
}
