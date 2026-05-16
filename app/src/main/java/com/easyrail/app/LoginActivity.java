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

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        btnLogin.setOnClickListener(v -> doLogin());

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void doLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Veuillez saisir votre email");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Veuillez saisir votre mot de passe");
            return;
        }

        LoginRequest request = new LoginRequest(email, password);

        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();

                    boolean loginOk =
                            authResponse.isSuccess()
                                    && authResponse.getClientId() != null
                                    && !authResponse.getClientId().isEmpty()
                                    && authResponse.getAuthToken() != null
                                    && !authResponse.getAuthToken().isEmpty();

                    if (loginOk) {
                        sessionManager.saveAuth(authResponse);

                        Toast.makeText(LoginActivity.this,
                                "Connexion réussie",
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, SearchActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                authResponse.getMessage() != null ? authResponse.getMessage() : "Connexion refusée",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    boolean handled = ApiErrorUtils.applyFieldError(response, "email", etEmail);
                    handled = ApiErrorUtils.applyFieldError(response, "motDePasse", etPassword) || handled;
                    Toast.makeText(LoginActivity.this,
                            handled
                                    ? ApiErrorUtils.messageOrFallback(response, "Veuillez corriger les champs")
                                    : ApiErrorUtils.messageOrFallback(response, "Échec de connexion"),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this,
                        "Erreur réseau : " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

}
