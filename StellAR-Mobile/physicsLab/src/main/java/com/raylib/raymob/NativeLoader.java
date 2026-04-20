/*
 *  raymob License (MIT)
 *
 *  Copyright (c) 2023-2024 Le Juez Victor
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.raylib.raymob;  // Don't change the package name (see gradle.properties)

import android.app.NativeActivity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;

import java.util.Locale;

public class NativeLoader extends NativeActivity implements TextToSpeech.OnInitListener {

    public DisplayManager displayManager;
    public SoftKeyboard softKeyboard;
    public boolean initCallback = false;
    private TextToSpeech tts;
    private AINarrator aiNarrator;
    private volatile String pendingAINarration = null;

    // Loading method of your native application
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        displayManager = new DisplayManager(this);
        softKeyboard = new SoftKeyboard(this);
        tts = new TextToSpeech(this, this);
        aiNarrator = new AINarrator(this);
        System.loadLibrary("raymob");   // Load your game library (don't change raymob, see gradle.properties)
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Locale indianEnglish = new Locale("en", "IN");
            int result = tts.setLanguage(indianEnglish);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language en_IN not supported");
                tts.setLanguage(Locale.US);
            }
        } else {
            Log.e("TTS", "Initialization failed");
        }
    }

    public void speakTTS(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, null);
        }
    }

    // Handling loss and regain of application focus
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            displayManager.setImmersiveMode(); // If the app has focus, re-enable immersive mode
        }
    }

    // Callback methods for managing the Android software keyboard
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        softKeyboard.onKeyUpEvent(event);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(initCallback) {
            onAppStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(initCallback) {
            onAppResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(initCallback) {
            onAppPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(initCallback){
            onAppStop();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private native void onAppStart();
    private native void onAppResume();
    private native void onAppPause();
    private native void onAppStop();

    public void requestAINarration(String context) {
        if (aiNarrator != null) {
            aiNarrator.requestNarration(context);
        }
    }

    public void onAINarrationReady(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, null);
        }
        pendingAINarration = text;
    }

    public String pollAINarration() {
        String result = pendingAINarration;
        pendingAINarration = null;
        return result;
    }

}
