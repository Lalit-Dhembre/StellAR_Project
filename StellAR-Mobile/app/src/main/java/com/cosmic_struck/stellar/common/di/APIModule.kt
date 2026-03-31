package com.cosmic_struck.stellar.common.di

import com.cosmic_struck.stellar.classroom.data.service.ClassroomModuleService
import com.cosmic_struck.stellar.create_module.data.service.ModelGenerationService
import com.cosmic_struck.stellar.stellar.scantext.data.remote.ScanService
import com.cosmic_struck.stellar.stellar.pdfar.data.remote.PdfArService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object APIModule {
//    val baseUrl = "https://chun-nonimpulsive-nondeficiently.ngrok-free.dev"
    val baseUrl = "http://192.168.1.4:5000"
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            // Increase timeouts for heavy AI operations
            .connectTimeout(60, TimeUnit.MINUTES) // Time to establish connection
            .readTimeout(60, TimeUnit.MINUTES)    // Time waiting for data (Server processing)
            .writeTimeout(60, TimeUnit.MINUTES)   // Time sending data (Uploading image)
            .build()
    }
    @Provides
    @Singleton
    fun provideScanService(okHttpClient: OkHttpClient): ScanService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ScanService::class.java)
    }

    @Provides
    @Singleton
    fun provideClassroomModuleService(okHttpClient: OkHttpClient): ClassroomModuleService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ClassroomModuleService::class.java)
    }

    @Provides
    @Singleton
    fun provideGenerateModuleService(): ModelGenerationService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ModelGenerationService::class.java)
    }

    @Provides
    @Singleton
    fun providePdfArService(okHttpClient: OkHttpClient): PdfArService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PdfArService::class.java)
    }
}