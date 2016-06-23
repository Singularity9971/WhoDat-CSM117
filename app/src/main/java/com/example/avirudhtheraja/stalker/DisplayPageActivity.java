package com.example.avirudhtheraja.stalker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class DisplayPageActivity extends AppCompatActivity {

    private WebView webView;
    private final static String URL = "https://www.linkedin.com/in/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_page);
        webView = (WebView)findViewById(R.id.webView);
        webView.setWebChromeClient(new WebChromeClient());
        String id = getIntent().getExtras().getString("id");
        webView.loadUrl(URL+id);
    }
}
