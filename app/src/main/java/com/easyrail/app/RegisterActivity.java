package com.easyrail.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNom;
    private EditText etPrenom;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNom = findViewById(R.id.etNom);
        etPrenom = findViewById(R.id.etPrenom);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        btnRegister.setOnClickListener(v -> doRegister());

        tvGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void doRegister() {
        String nom = etNom.getText().toString().trim();
        String prenom = etPrenom.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (nom.isEmpty()) {
            etNom.setError("Veuillez saisir votre nom");
            return;
        }

        if (prenom.isEmpty()) {
            etPrenom.setError("Veuillez saisir votre prénom");
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Veuillez saisir votre email");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Veuillez saisir votre mot de passe");
            return;
        }

        RegisterRequest request = new RegisterRequest(nom, prenom, email, password);

        apiService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();

                    boolean registerOk =
                            authResponse.isSuccess()
                                    && authResponse.getClientId() != null
                                    && !authResponse.getClientId().isEmpty()
                                    && authResponse.getAuthToken() != null
                                    && !authResponse.getAuthToken().isEmpty();

                    if (registerOk) {
                        sessionManager.saveAuth(authResponse);

                        Toast.makeText(RegisterActivity.this,
                                "Inscription réussie",
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(RegisterActivity.this, SearchActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                authResponse.getMessage() != null ? authResponse.getMessage() : "Inscription refusée",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    boolean handled = ApiErrorUtils.applyFieldError(response, "nom", etNom);
                    handled = ApiErrorUtils.applyFieldError(response, "prenom", etPrenom) || handled;
                    handled = ApiErrorUtils.applyFieldError(response, "email", etEmail) || handled;
                    handled = ApiErrorUtils.applyFieldError(response, "motDePasse", etPassword) || handled;
                    Toast.makeText(RegisterActivity.this,
                            handled
                                    ? ApiErrorUtils.messageOrFallback(response, "Veuillez corriger les champs")
                                    : ApiErrorUtils.messageOrFallback(response, "Échec de l'inscription"),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this,
                        "Erreur réseau : " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

}
