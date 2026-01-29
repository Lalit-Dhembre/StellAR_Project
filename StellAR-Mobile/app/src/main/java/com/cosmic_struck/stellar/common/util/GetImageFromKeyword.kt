package com.cosmic_struck.stellar.common.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Fetches educational images based on a keyword using the public Wikipedia API.
 * No API Key required.
 */
class GetImageFromKeyword @Inject constructor() {

    private val client = OkHttpClient.Builder()
        // Wikimedia requires a User-Agent header. Without this, requests (including image loads) return 403 Forbidden.
        .addNetworkInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "StellAR/1.0 (com.cosmic_struck.stellar)")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun getImageUrl(keyword: String): String? = withContext(Dispatchers.IO) {
        try {
            // Properly encode the keyword
            val encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8")

            // Step 1: Search for the page to get the correct title
            val searchUrl = "https://en.wikipedia.org/w/api.php?" +
                    "action=query&" +
                    "list=search&" +
                    "srsearch=$encodedKeyword&" +
                    "format=json&" +
                    "utf8=1&" +
                    "srnamespace=0"

            Log.d("WikiRepo", "Search URL: $searchUrl")

            val searchRequest = Request.Builder()
                .url(searchUrl)
                .build()

            var pageTitle: String? = null

            client.newCall(searchRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("WikiRepo", "Search failed: ${response.code}")
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                Log.d("WikiRepo", "Search Response: ${responseBody.take(200)}")

                val searchJson = gson.fromJson(responseBody, JsonObject::class.java)
                val search = searchJson.getAsJsonObject("query").getAsJsonArray("search")

                if (search.size() > 0) {
                    pageTitle = search.get(0).asJsonObject.get("title").asString
                    Log.d("WikiRepo", "Found page: $pageTitle")
                } else {
                    Log.d("WikiRepo", "No search results for: $keyword")
                    return@withContext null
                }
            }

            if (pageTitle == null) return@withContext null

            // Step 2: Get image from the correct page
            val encodedTitle = java.net.URLEncoder.encode(pageTitle!!, "UTF-8")
            val imageUrl = "https://en.wikipedia.org/w/api.php?" +
                    "action=query&" +
                    "prop=pageimages&" +
                    "format=json&" +
                    "piprop=original&" +
                    "titles=$encodedTitle&" +
                    "pithumbsize=500&" +
                    "utf8=1"

            Log.d("WikiRepo", "Image URL: $imageUrl")

            val imageRequest = Request.Builder()
                .url(imageUrl)
                .build()

            client.newCall(imageRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("WikiRepo", "Image request failed: ${response.code}")
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                Log.d("WikiRepo", "Image Response: ${responseBody.take(300)}")

                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                val query = jsonObject.getAsJsonObject("query")

                if (!query.has("pages")) {
                    Log.d("WikiRepo", "No pages in response")
                    return@withContext null
                }

                val pages = query.getAsJsonObject("pages")
                val firstPageKey = pages.keySet().firstOrNull()

                if (firstPageKey == null) {
                    Log.d("WikiRepo", "No page keys found")
                    return@withContext null
                }

                val page = pages.getAsJsonObject(firstPageKey)
                Log.d("WikiRepo", "Page object: $page")

                // Try original first, then thumbnail
                if (page.has("original")) {
                    val imageUri = page.getAsJsonObject("original").get("source").asString
                    Log.d("WikiRepo", "Found original image: $imageUri")
                    return@withContext imageUri
                } else if (page.has("thumbnail")) {
                    val imageUri = page.getAsJsonObject("thumbnail").get("source").asString
                    Log.d("WikiRepo", "Found thumbnail image: $imageUri")
                    return@withContext imageUri
                } else {
                    Log.d("WikiRepo", "No image found in page object. Keys: ${page.keySet()}")
                    return@withContext null
                }
            }

        } catch (e: Exception) {
            Log.e("WikiRepo", "Error fetching image for $keyword", e)
        }

        return@withContext null
    }
}