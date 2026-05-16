package com.easyrail.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private EditText etNom;
    private EditText etPrenom;
    private EditText etEmail;
    private AutoCompleteTextView etSexe;
    private EditText etDateNaissance;
    private EditText etTelephone;
    private Button btnSaveProfile;
    private Button btnLogout;
    private Button btnSupport;
    private BottomNavigationView bottomNavigationView;

    private ApiService apiService;
    private SessionManager sessionManager;
    private String clientId;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etNom = findViewById(R.id.etNom);
        etPrenom = findViewById(R.id.etPrenom);
        etEmail = findViewById(R.id.etEmail);
        etSexe = findViewById(R.id.etSexe);
        etDateNaissance = findViewById(R.id.etDateNaissance);
        etTelephone = findViewById(R.id.etTelephone);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnSupport = findViewById(R.id.btnSupport);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);
        if (ApiSessionHandler.redirectToLoginIfSessionMissing(
                this,
                sessionManager,
                "Veuillez vous connecter"
        )) {
            return;
        }
        clientId = sessionManager.getClientId();
        authToken = sessionManager.getAuthToken();

        ArrayAdapter<String> sexeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"Femme", "Homme", "Autre"}
        );
        etSexe.setAdapter(sexeAdapter);

        etEmail.setEnabled(false);

        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
        btnSupport.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, SupportActivity.class)));

        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_search) {
                startActivity(new Intent(ProfileActivity.this, SearchActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_tickets) {
                startActivity(new Intent(ProfileActivity.this, MyTicketsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }

            return false;
        });

        loadProfile();
    }

    private boolean isMeaningful(String value) {
        if (value == null) return false;
        String v = value.trim();
        if (v.isEmpty()) return false;
        return !v.equalsIgnoreCase("Utilisateur");
    }

    private void loadProfile() {
        if (clientId == null || clientId.trim().isEmpty() || authToken == null || authToken.trim().isEmpty()) {
            Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        apiService.getProfile(authToken, clientId).enqueue(new Callback<ClientProfileResponse>() {
            @Override
            public void onResponse(Call<ClientProfileResponse> call, Response<ClientProfileResponse> response) {
                if (ApiSessionHandler.handleUnauthorized(ProfileActivity.this, sessionManager, response)) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    ClientProfileResponse profile = response.body();

                    etNom.setText(isMeaningful(profile.getNom()) ? profile.getNom() : "");
                    etPrenom.setText(isMeaningful(profile.getPrenom()) ? profile.getPrenom() : "");
                    etEmail.setText(safe(profile.getEmail()));
                    etSexe.setText(isMeaningful(profile.getSexe()) ? profile.getSexe() : "", false);
                    etDateNaissance.setText(isMeaningful(profile.getDateNaissance()) ? profile.getDateNaissance() : "");
                    etTelephone.setText(isMeaningful(profile.getTelephone()) ? profile.getTelephone() : "");

                    sessionManager.saveProfile(profile);
                } else {
                    loadFromPrefsAsFallback();
                }
            }

            @Override
            public void onFailure(Call<ClientProfileResponse> call, Throwable t) {
                loadFromPrefsAsFallback();
            }
        });
    }

    private void loadFromPrefsAsFallback() {
        etNom.setText(sessionManager.getNom());
        etPrenom.setText(sessionManager.getPrenom());
        etEmail.setText(sessionManager.getEmail());
        etSexe.setText(sessionManager.getSexe(), false);
        etDateNaissance.setText(sessionManager.getDateNaissance());
        etTelephone.setText(sessionManager.getTelephone());
    }

    private void saveProfile() {
        String nom = etNom.getText().toString().trim();
        String prenom = etPrenom.getText().toString().trim();
        String sexe = etSexe.getText().toString().trim();
        String dateNaissance = etDateNaissance.getText().toString().trim();
        String telephone = etTelephone.getText().toString().trim();

        if (TextUtils.isEmpty(nom)) {
            etNom.setError("Veuillez saisir votre nom");
            return;
        }

        if (TextUtils.isEmpty(prenom)) {
            etPrenom.setError("Veuillez saisir votre prénom");
            return;
        }

        if (!TextUtils.isEmpty(dateNaissance) && !dateNaissance.matches("\\d{4}-\\d{2}-\\d{2}")) {
            etDateNaissance.setError("Format attendu : YYYY-MM-DD");
            return;
        }

        UpdateProfileRequest request = new UpdateProfileRequest(
                nom,
                prenom,
                sexe,
                dateNaissance,
                telephone
        );

        apiService.updateProfile(authToken, clientId, request).enqueue(new Callback<ClientProfileResponse>() {
            @Override
            public void onResponse(Call<ClientProfileResponse> call, Response<ClientProfileResponse> response) {
                if (ApiSessionHandler.handleUnauthorized(ProfileActivity.this, sessionManager, response)) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    ClientProfileResponse updated = response.body();
                    sessionManager.saveProfile(updated);
                    Toast.makeText(ProfileActivity.this, "Profil mis à jour", Toast.LENGTH_SHORT).show();
                } else {
                    boolean handled = ApiErrorUtils.applyFieldError(response, "telephone", etTelephone);
                    String message = handled
                            ? ApiErrorUtils.messageOrFallback(response, "Veuillez corriger les champs")
                            : ApiErrorUtils.messageOrFallback(response, "Impossible de mettre à jour le profil");
                    Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ClientProfileResponse> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void logout() {
        if (authToken == null || authToken.trim().isEmpty()) {
            finishLogoutLocally();
            return;
        }

        apiService.logout(authToken).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (ApiSessionHandler.handleUnauthorized(ProfileActivity.this, sessionManager, response)) {
                    return;
                }
                finishLogoutLocally();
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                finishLogoutLocally();
            }
        });
    }

    private void finishLogoutLocally() {
        sessionManager.clear();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
