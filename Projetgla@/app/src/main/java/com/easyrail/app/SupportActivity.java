package com.easyrail.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        Button btnEmail = findViewById(R.id.btnSupportEmail);
        Button btnPhone = findViewById(R.id.btnSupportPhone);

        btnEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@ticketeer.app"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Assistance Ticketeer");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Aucune application e-mail disponible", Toast.LENGTH_SHORT).show();
            }
        });

        btnPhone.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+33180001234"));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Aucune application d'appel disponible", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
