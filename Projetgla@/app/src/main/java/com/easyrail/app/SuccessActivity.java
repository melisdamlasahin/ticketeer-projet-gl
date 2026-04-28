package com.easyrail.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class SuccessActivity extends AppCompatActivity {

    private TextView tvDescription;
    private Button btnViewTicket;
    private Button btnBackHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        tvDescription = findViewById(R.id.tvDescription);
        btnViewTicket = findViewById(R.id.btnViewTicket);
        btnBackHome = findViewById(R.id.btnBackHome);

        String billetId = getIntent().getStringExtra("billetId");
        String message = getIntent().getStringExtra("message");
        double prixFinal = getIntent().getDoubleExtra("prixFinal", 0);

        String departure = getIntent().getStringExtra("departure");
        String destination = getIntent().getStringExtra("destination");
        String trainName = getIntent().getStringExtra("trainName");
        String date = getIntent().getStringExtra("date");
        String returnDeparture = getIntent().getStringExtra("returnDeparture");
        String returnDestination = getIntent().getStringExtra("returnDestination");
        String returnDate = getIntent().getStringExtra("returnDate");
        String passengerEmail = getIntent().getStringExtra("passengerEmail");

        String displayText;

        if (message != null && !message.isEmpty()) {
            displayText = message + "\n";
        } else {
            displayText = "Votre billet a été généré avec succès.\n";
        }

        displayText += String.format(Locale.FRANCE, "Prix final : %.2f €", prixFinal);
        if (passengerEmail != null && !passengerEmail.isEmpty()) {
            displayText += "\nConfirmation envoyée à : " + passengerEmail;
        }
        if (returnDeparture != null && !returnDeparture.isEmpty()) {
            displayText += "\nAller-retour : " + departure + " → " + destination
                    + " / " + returnDeparture + " → " + returnDestination
                    + "\nRetour : " + returnDate;
        }
        tvDescription.setText(displayText);

        btnViewTicket.setOnClickListener(v -> {
            Intent intent = new Intent(SuccessActivity.this, TicketActivity.class);
            intent.putExtra("billetId", billetId);
            intent.putExtra("departure", departure);
            intent.putExtra("destination", destination);
            intent.putExtra("trainName", trainName);
            intent.putExtra("date", date);
            intent.putExtra("prixFinal", prixFinal);
            startActivity(intent);
        });

        btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(SuccessActivity.this, SearchActivity.class);
            startActivity(intent);
        });
    }
}
