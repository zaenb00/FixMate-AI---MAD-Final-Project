package com.fixmateai.di

import com.fixmateai.data.remote.gemini.GeminiApiService
import com.fixmateai.data.remote.groq.GroqApiService
import com.fixmateai.data.remote.places.PlacesApiService
import com.fixmateai.utils.Constants
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Provides networking dependencies (OkHttp + Retrofit + API services).
 *
 * We need two Retrofit instances with different base URLs (Gemini and Places),
 * so we tag them with qualifier annotations to tell them apart.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Qualifier @Retention(AnnotationRetention.BINARY) annotation class GeminiRetrofit
    @Qualifier @Retention(AnnotationRetention.BINARY) annotation class GroqRetrofit
    @Qualifier @Retention(AnnotationRetention.BINARY) annotation class PlacesRetrofit

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // Log request/response bodies only in debug builds.
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.fixmateai.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)   // Gemini can be slow on large images
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @GeminiRetrofit
    fun provideGeminiRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(Constants.GEMINI_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @GroqRetrofit
    fun provideGroqRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(Constants.GROQ_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @PlacesRetrofit
    fun providePlacesRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(Constants.PLACES_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideGeminiApiService(@GeminiRetrofit retrofit: Retrofit): GeminiApiService =
        retrofit.create(GeminiApiService::class.java)

    @Provides
    @Singleton
    fun provideGroqApiService(@GroqRetrofit retrofit: Retrofit): GroqApiService =
        retrofit.create(GroqApiService::class.java)

    @Provides
    @Singleton
    fun providePlacesApiService(@PlacesRetrofit retrofit: Retrofit): PlacesApiService =
        retrofit.create(PlacesApiService::class.java)
}
