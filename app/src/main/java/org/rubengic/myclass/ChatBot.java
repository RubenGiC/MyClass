package org.rubengic.myclass;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ChatBot  extends AppCompatActivity {

    private WebView wv_chat;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);
        //accedo al WebView creado
        wv_chat = (WebView) findViewById(R.id.wv_chat);
        //indico que voy a usar javascript
        WebSettings webSettings = wv_chat.getSettings();
        webSettings.setJavaScriptEnabled(true);

        //y le paso la url del dialogflow
        wv_chat.loadUrl("https://console.dialogflow.com/api-client/demo/embedded/e48d954d-2d2e-4deb-9ffb-21e88a10609a");
    }
}
