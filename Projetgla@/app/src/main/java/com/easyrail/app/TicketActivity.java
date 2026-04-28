package com.easyrail.app;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import java.io.OutputStream;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketActivity extends AppCompatActivity {

    private TextView tvRoute, tvTrain, tvDate, tvPrice, tvLiveInfo, tvPassengerInfo;
    private ImageView imgQrCode;
    private Button btnPdf, btnHome, btnCancel, btnModify, btnSupport;

    private ApiService apiService;
    private SessionManager sessionManager;
    private String billetId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        tvRoute = findViewById(R.id.tvRoute);
        tvTrain = findViewById(R.id.tvTrain);
        tvDate = findViewById(R.id.tvDate);
        tvPrice = findViewById(R.id.tvPrice);
        tvLiveInfo = findViewById(R.id.tvLiveInfo);
        tvPassengerInfo = findViewById(R.id.tvPassengerInfo);
        imgQrCode = findViewById(R.id.imgQrCode);
        btnPdf = findViewById(R.id.btnPdf);
        btnHome = findViewById(R.id.btnHome);
        btnCancel = findViewById(R.id.btnCancel);
        btnModify = findViewById(R.id.btnModify);
        btnSupport = findViewById(R.id.btnSupport);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);
        if (ApiSessionHandler.redirectToLoginIfSessionMissing(
                this,
                sessionManager,
                "Veuillez vous connecter pour consulter votre billet"
        )) {
            return;
        }

        billetId = getIntent().getStringExtra("billetId");

        if (billetId != null && !billetId.isEmpty()) {
            loadBillet(billetId);
        }

        btnPdf.setOnClickListener(v -> downloadPdf(billetId));

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(TicketActivity.this, SearchActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnSupport.setOnClickListener(v -> startActivity(new Intent(this, SupportActivity.class)));
        btnCancel.setOnClickListener(v -> cancelTicket());
        btnModify.setOnClickListener(v -> openModifyDialog());
    }

    private String getAuthToken() {
        return sessionManager.getAuthToken();
    }

    private void loadBillet(String id) {
        String authToken = getAuthToken();

        apiService.getBillet(authToken, id).enqueue(new Callback<TicketApiModel>() {
            @Override
            public void onResponse(Call<TicketApiModel> call, Response<TicketApiModel> response) {
                if (ApiSessionHandler.handleUnauthorized(TicketActivity.this, sessionManager, response)) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {

                    TicketApiModel t = response.body();

                    tvRoute.setText(t.getTrajetResume() != null
                            ? t.getTrajetResume()
                            : t.getVilleDepartNom() + " → " + t.getVilleArriveeNom());
                    if ("ALLER_RETOUR".equals(t.getTypeTrajet())) {
                        tvTrain.setText(t.getTrainNom() + "\nRetour : " + safeText(t.getTrainRetourNom()));
                        tvDate.setText(buildScheduleLabel(t.getDateTrajet(), t.getHeureDepart())
                                + "\nRetour : " + buildScheduleLabel(t.getDateRetour(), t.getHeureRetour()));
                    } else {
                        tvTrain.setText(t.getTrainNom());
                        tvDate.setText(buildScheduleLabel(t.getDateTrajet(), t.getHeureDepart()));
                    }
                    tvPrice.setText(String.format(Locale.FRANCE, "%.2f €", t.getPrixFinal()));
                    tvLiveInfo.setText(buildLiveInfo(t));
                    tvPassengerInfo.setText(buildPassengerInfo(t));
                    tvLiveInfo.setVisibility(View.VISIBLE);
                    tvPassengerInfo.setVisibility(View.VISIBLE);
                    btnCancel.setEnabled(!"ANNULE".equalsIgnoreCase(t.getEtat()));
                    btnModify.setEnabled(!"ANNULE".equalsIgnoreCase(t.getEtat()));

                    if (t.getQrCodeBase64() != null) {
                        byte[] decoded = Base64.decode(t.getQrCodeBase64(), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        imgQrCode.setImageBitmap(bitmap);
                    }

                } else {
                    Toast.makeText(TicketActivity.this, "Erreur chargement billet", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<TicketApiModel> call, Throwable t) {
                Toast.makeText(TicketActivity.this, "Erreur réseau", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void downloadPdf(String id) {
        String authToken = getAuthToken();

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(BuildConfig.API_BASE_URL + "api/billets/" + id + "/pdf");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestProperty("X-Auth-Token", authToken);
                conn.connect();

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    java.io.InputStream input = conn.getInputStream();

                    String fileName = "ticket_" + id + ".pdf";

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                    Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

                    if (uri != null) {
                        OutputStream output = getContentResolver().openOutputStream(uri);

                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = input.read(buffer)) > 0) {
                            output.write(buffer, 0, len);
                        }

                        output.close();
                        input.close();

                        runOnUiThread(() ->
                                Toast.makeText(this, "PDF téléchargé", Toast.LENGTH_LONG).show()
                        );
                    }
                } else if (statusCode == 401 || statusCode == 403) {
                    runOnUiThread(() -> ApiSessionHandler.handleUnauthorizedStatus(TicketActivity.this, sessionManager, statusCode));
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Téléchargement refusé", Toast.LENGTH_LONG).show()
                    );
                }

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Erreur téléchargement PDF", Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private String safeText(String value) {
        return value != null ? value : "-";
    }

    private String buildScheduleLabel(String date, String departureTime) {
        if (departureTime == null || departureTime.trim().isEmpty()) {
            return safeText(date);
        }
        return safeText(date) + " • " + departureTime;
    }

    private String buildLiveInfo(TicketApiModel ticket) {
        String platform = ticket.getVoie() != null ? ticket.getVoie() : "à confirmer";
        int delayMinutes = ticket.getRetardMinutes() != null ? ticket.getRetardMinutes() : 0;
        if (delayMinutes > 0) {
            return "Voie " + platform + " • retard en temps réel : " + delayMinutes + " min";
        }
        return "Voie " + platform + " • train à l'heure";
    }

    private String buildPassengerInfo(TicketApiModel ticket) {
        return "Passager : " + safeText(ticket.getNomPassager())
                + "\nClasse : " + safeText(ticket.getClasseReservation())
                + " • Place : " + safeText(ticket.getNumeroPlace())
                + "\nPaiement : " + safeText(ticket.getMethodePaiement())
                + "\nEmail : " + safeText(ticket.getEmailPassager());
    }

    private void cancelTicket() {
        String authToken = getAuthToken();
        apiService.cancelBillet(authToken, billetId).enqueue(new Callback<TicketApiModel>() {
            @Override
            public void onResponse(Call<TicketApiModel> call, Response<TicketApiModel> response) {
                if (ApiSessionHandler.handleUnauthorized(TicketActivity.this, sessionManager, response)) {
                    return;
                }
                if (response.isSuccessful()) {
                    Toast.makeText(TicketActivity.this, "Billet annulé", Toast.LENGTH_SHORT).show();
                    loadBillet(billetId);
                } else {
                    Toast.makeText(TicketActivity.this, "Annulation impossible", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<TicketApiModel> call, Throwable t) {
                Toast.makeText(TicketActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openModifyDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_modify_ticket, null);
        EditText etName = dialogView.findViewById(R.id.etModifyPassengerName);
        EditText etEmail = dialogView.findViewById(R.id.etModifyPassengerEmail);
        EditText etPhone = dialogView.findViewById(R.id.etModifyPassengerPhone);
        EditText etClass = dialogView.findViewById(R.id.etModifyClass);
        EditText etSeatPref = dialogView.findViewById(R.id.etModifySeatPreference);

        new AlertDialog.Builder(this)
                .setTitle("Modifier le billet")
                .setView(dialogView)
                .setNegativeButton("Fermer", null)
                .setPositiveButton("Enregistrer", (dialog, which) -> submitTicketUpdate(
                        etName.getText().toString().trim(),
                        etEmail.getText().toString().trim(),
                        etPhone.getText().toString().trim(),
                        etClass.getText().toString().trim(),
                        etSeatPref.getText().toString().trim()
                ))
                .show();
    }

    private void submitTicketUpdate(String name, String email, String phone, String bookingClass, String seatPreference) {
        String authToken = getAuthToken();
        String clientId = sessionManager.getClientId();
        AchatBilletRequest request = new AchatBilletRequest(
                null,
                null,
                "STANDARD",
                clientId,
                bookingClass,
                seatPreference,
                name,
                email,
                phone,
                "CARTE"
        );
        apiService.updateBillet(authToken, billetId, request).enqueue(new Callback<TicketApiModel>() {
            @Override
            public void onResponse(Call<TicketApiModel> call, Response<TicketApiModel> response) {
                if (ApiSessionHandler.handleUnauthorized(TicketActivity.this, sessionManager, response)) {
                    return;
                }
                if (response.isSuccessful()) {
                    Toast.makeText(TicketActivity.this, "Billet modifié", Toast.LENGTH_SHORT).show();
                    loadBillet(billetId);
                } else {
                    Toast.makeText(TicketActivity.this, "Modification impossible", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<TicketApiModel> call, Throwable t) {
                Toast.makeText(TicketActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
