package com.easyrail.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private Button btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        boolean isLoggedIn = sessionManager.isLoggedIn();

        if (isLoggedIn) {
            Intent intent = new Intent(WelcomeActivity.this, SearchActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        btnStart = findViewById(R.id.btnStart);

        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }
}
