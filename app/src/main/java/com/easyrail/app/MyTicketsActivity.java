package com.easyrail.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyTicketsActivity extends AppCompatActivity {

    private RecyclerView recyclerTickets;
    private TextView tvEmpty;
    private View progressBar;
    private BottomNavigationView bottomNavigationView;

    private ApiService apiService;
    private SessionManager sessionManager;
    private final List<TicketApiModel> tickets = new ArrayList<>();
    private MyTicketsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_tickets);

        recyclerTickets = findViewById(R.id.recyclerTickets);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        recyclerTickets.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyTicketsAdapter(this, tickets);
        recyclerTickets.setAdapter(adapter);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);
        if (ApiSessionHandler.redirectToLoginIfSessionMissing(
                this,
                sessionManager,
                "Veuillez vous connecter pour consulter vos billets"
        )) {
            return;
        }

        bottomNavigationView.setSelectedItemId(R.id.nav_tickets);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_search) {
                startActivity(new Intent(MyTicketsActivity.this, SearchActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_tickets) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(MyTicketsActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });

        loadMyTickets();
    }

    private void loadMyTickets() {
        String clientId = sessionManager.getClientId();
        String authToken = sessionManager.getAuthToken();

        if (clientId == null || clientId.trim().isEmpty() || authToken == null || authToken.trim().isEmpty()) {
            Toast.makeText(this, "Veuillez vous connecter pour consulter vos billets", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerTickets.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        apiService.getBilletsByClient(authToken, clientId).enqueue(new Callback<List<TicketApiModel>>() {
            @Override
            public void onResponse(Call<List<TicketApiModel>> call, Response<List<TicketApiModel>> response) {
                progressBar.setVisibility(View.GONE);
                if (ApiSessionHandler.handleUnauthorized(MyTicketsActivity.this, sessionManager, response)) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    tickets.clear();
                    tickets.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (tickets.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerTickets.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerTickets.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvEmpty.setText("Impossible de charger vos billets.");
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerTickets.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<TicketApiModel>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setText("Erreur réseau : " + t.getMessage());
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerTickets.setVisibility(View.GONE);
            }
        });
    }
}
