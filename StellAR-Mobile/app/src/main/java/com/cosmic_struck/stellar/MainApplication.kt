package com.cosmic_struck.stellar

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.cosmic_struck.stellar.common.work.CleanupWorker
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()


        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient {
                    OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            val originalRequest = chain.request()
                            val newRequest = originalRequest.newBuilder()
                                .header("User-Agent", "Mozilla/5.0 (Android)")
                                .build()
                            chain
                                .withConnectTimeout(30, TimeUnit.SECONDS)
                                .withReadTimeout(30, TimeUnit.SECONDS)
                                .proceed(newRequest)
                        }
                        .build()
                }
                .memoryCache {
                    MemoryCache.Builder(applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(applicationContext.cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.02)
                        .build()
                }
                .build()
        )
    }
    private fun scheduleCleanupWork() {
        val cleanupRequest = OneTimeWorkRequestBuilder<CleanupWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "cleanup_work",
            ExistingWorkPolicy.KEEP,
            cleanupRequest
        )
        Log.d("CleanupWork", "Cleanup work enqueued")
    }
}