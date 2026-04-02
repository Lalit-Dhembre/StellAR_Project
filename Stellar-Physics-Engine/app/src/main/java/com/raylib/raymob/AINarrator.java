package com.raylib.raymob;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

public class AINarrator {
    private static final String TAG = "AINarrator";
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemma-3-4b-it:generateContent?key=" + API_KEY;

    private static final String SYSTEM_PROMPT =
        "You are a fun, concise physics teacher narrating a solar system simulation. " +
        "STRICT RULES: " +
        "1. Respond with exactly ONE short sentence, maximum 15 words. " +
        "2. Always complete your thought in that single sentence. Never leave sentences unfinished. " +
        "3. Name the specific planets involved. " +
        "4. Use simple numbers, and scientific notation. " +
        "5. No emojis, asterisks, markdown, or special characters. Plain spoken text only.";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final NativeLoader activity;

    public AINarrator(NativeLoader activity) {
        this.activity = activity;
    }

    public void requestNarration(final String contextPrompt) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Requesting narration: " + contextPrompt.substring(0, Math.min(80, contextPrompt.length())));
                String response = callGeminiAPI(contextPrompt);
                if (response != null && !response.isEmpty()) {
                    Log.d(TAG, "Got AI response: " + response);
                    mainHandler.post(() -> activity.onAINarrationReady(response));
                } else {
                    Log.w(TAG, "Empty or null response from Gemini");
                }
            } catch (Exception e) {
                Log.e(TAG, "AI narration failed: " + e.getMessage(), e);
            }
        });
    }

    private String callGeminiAPI(String userPrompt) {
        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(API_URL);
            conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);

            // Prepend system prompt to user content since Gemma doesn't support system_instruction natively
            String combinedPrompt = SYSTEM_PROMPT + "\n\nSimulation Event:\n" + userPrompt;

            // User content
            JSONObject userPart = new JSONObject();
            userPart.put("text", combinedPrompt);
            JSONObject userContent = new JSONObject();
            userContent.put("role", "user");
            userContent.put("parts", new JSONArray().put(userPart));

            JSONArray contents = new JSONArray();
            contents.put(userContent);

            // Generation config
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("maxOutputTokens", 100);
            generationConfig.put("temperature", 0.7);

            // Request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contents);
            requestBody.put("generationConfig", generationConfig);

            String bodyStr = requestBody.toString();
            Log.d(TAG, "Request body length: " + bodyStr.length());

            byte[] input = bodyStr.getBytes(StandardCharsets.UTF_8);
            OutputStream os = conn.getOutputStream();
            os.write(input);
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "API response code: " + responseCode);

            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject responseJson = new JSONObject(sb.toString());
                JSONArray candidates = responseJson.optJSONArray("candidates");
                if (candidates != null && candidates.length() > 0) {
                    JSONObject candidate = candidates.getJSONObject(0);
                    if (candidate.has("content")) {
                        JSONObject content = candidate.getJSONObject("content");
                        if (content.has("parts")) {
                            JSONArray parts = content.getJSONArray("parts");
                            StringBuilder resultText = new StringBuilder();
                            for (int i = 0; i < parts.length(); i++) {
                                JSONObject part = parts.getJSONObject(i);
                                if (part.has("text")) {
                                    resultText.append(part.getString("text"));
                                }
                            }
                            if (resultText.length() > 0) {
                                return resultText.toString().trim();
                            }
                        }
                    }
                    if (candidate.has("finishReason")) {
                        Log.w(TAG, "Generation finished early. finishReason: " + candidate.getString("finishReason"));
                    }
                }
                return "The simulation data is complex, let's observe what happens next.";
            } else {
                // Read error body for debugging
                BufferedReader errReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder errSb = new StringBuilder();
                String errLine;
                while ((errLine = errReader.readLine()) != null) errSb.append(errLine);
                errReader.close();
                Log.e(TAG, "API error " + responseCode + ": " + errSb.toString());
                
                if (responseCode == 429) {
                    return "My cognitive circuits are overloaded by data streams. Please wait a moment before modifying the simulation further.";
                } else {
                    return "There was an anomaly processing the simulation telemetry data.";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "API call exception: " + e.getMessage(), e);
            return "Solar interference detected. Unable to analyze planetary data.";
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
