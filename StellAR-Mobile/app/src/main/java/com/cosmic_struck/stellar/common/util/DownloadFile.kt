package com.cosmic_struck.stellar.common.util

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class DownloadFile @Inject constructor(
    val context: Application,
    val httpClient: OkHttpClient  // Add OkHttp to your dependencies
) {

    operator fun invoke(url: String, title: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        Log.d("Checking URL and Title", "$url $title")
        try {
            // Check network connectivity first
            if (!isNetworkAvailable()) {
                emit(Resource.Error("No network connection available"))
                return@flow
            }

            Log.d("Download File", "Network available, starting download")

            // Create app's private cache directory (no permissions needed, always available)
            val appPrivateDir = File(context.cacheDir, "StellarModels")
            if (!appPrivateDir.exists()) {
                appPrivateDir.mkdirs()
            }
            val fileName = buildFileName(url,title)
            val targetFile = File(appPrivateDir, fileName)

            // Check if file already exists
            if (targetFile.exists() && targetFile.length() > 0) {
                Log.d("Download File", "Model already cached: ${targetFile.absolutePath}")
                emit(Resource.Success(targetFile.absolutePath))
                return@flow
            }

            Log.d("Download File", "Download destination: ${targetFile.absolutePath}")

            // Use OkHttp for direct download to private storage
            val request = Request.Builder()
                .url(url)
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(Resource.Error("Download failed with status: ${response.code}"))
                return@flow
            }

            val body = response.body

            // Stream directly to file (memory efficient for large files)
            val contentLength = body?.contentLength()
            var downloadedBytes = 0L

            FileOutputStream(targetFile).use { fileOutput ->
                body?.byteStream().use { inputStream ->
                    val buffer = ByteArray(8192) // 8KB chunks
                    var bytesRead: Int

                    while (inputStream?.read(buffer).also { bytesRead = it ?: 0 } != -1) {
                        fileOutput.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // Emit progress
                        if (contentLength != null) {
                            if (contentLength > 0) {
                                val progress = (downloadedBytes * 100 / contentLength).toInt()
                                Log.d("Download File", "Download progress: $progress%")
                            }
                        }
                    }
                }
            }

            // Verify download completed successfully
            if (targetFile.exists() && targetFile.length() > 0) {
                Log.d(
                    "Download File",
                    "Download complete. File: ${targetFile.absolutePath}, Size: ${targetFile.length()} bytes"
                )
                emit(Resource.Success(targetFile.absolutePath))
            } else {
                emit(Resource.Error("File verification failed"))
            }

        } catch (e: Exception) {
            Log.e("Download File", "Download failed: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)  // Run on IO thread

    private fun buildFileName(url: String, title: String): String {
        // If title already has an extension, respect it
        if (title.contains('.') && title.substringAfterLast('.').length in 2..5) {
            return title
        }

        // Try extracting extension from URL
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val extension = cleanUrl.substringAfterLast('.', missingDelimiterValue = "")

        return if (extension.isNotEmpty() && extension.length in 2..5) {
            "$title.$extension"
        } else {
            title // fallback: no extension found
        }
    }

    private fun isNetworkAvailable(): Boolean {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Log.d("Download File", "Connected via WiFi")
                    true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    Log.d("Download File", "Connected via Cellular")
                    true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    Log.d("Download File", "Connected via Ethernet")
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e("Download File", "Error checking network: ${e.message}")
            return false
        }
    }
}