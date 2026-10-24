package com.example.data.remote

import android.content.Context
import com.example.data.auth.GoogleAuthManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object GoogleCalendarApiClient {

    private const val BASE_URL = "https://www.googleapis.com/calendar/v3/"

    fun create(context: Context): GoogleCalendarApiService {
        val authManager = GoogleAuthManager.getInstance(context)

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val token = authManager.accessToken
            val newRequest = if (!token.isNullOrBlank()) {
                originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Accept", "application/json")
                    .build()
            } else {
                originalRequest.newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
            }
            chain.proceed(newRequest)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(GoogleCalendarApiService::class.java)
    }
}
