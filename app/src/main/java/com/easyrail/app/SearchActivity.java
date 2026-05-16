package com.easyrail.app;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private AutoCompleteTextView etDeparture;
    private AutoCompleteTextView etDestination;
    private EditText etDate;
    private EditText etTime;
    private EditText etReturnDate;
    private CheckBox checkRoundTrip;
    private Button btnSearch;
    private BottomNavigationView bottomNavigationView;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etDeparture = findViewById(R.id.etDeparture);
        etDestination = findViewById(R.id.etDestination);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etReturnDate = findViewById(R.id.etReturnDate);
        checkRoundTrip = findViewById(R.id.checkRoundTrip);
        btnSearch = findViewById(R.id.btnSearch);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        apiService = RetrofitClient.getClient().create(ApiService.class);

        bindDatePicker(etDate);
        bindDatePicker(etReturnDate);
        bindTimePicker(etTime);
        loadStations();

        checkRoundTrip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etReturnDate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                etReturnDate.setText("");
            }
        });

        btnSearch.setOnClickListener(v -> {
            boolean roundTrip = checkRoundTrip.isChecked();
            String returnDate = etReturnDate.getText().toString().trim();

            if (roundTrip && returnDate.isEmpty()) {
                Toast.makeText(this, "Saisissez une date de retour", Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(SearchActivity.this, ResultsActivity.class);
            intent.putExtra("departure", etDeparture.getText().toString().trim());
            intent.putExtra("destination", etDestination.getText().toString().trim());
            intent.putExtra("date", etDate.getText().toString().trim());
            intent.putExtra("time", etTime.getText().toString().trim());
            intent.putExtra("roundTrip", roundTrip);
            intent.putExtra("returnDate", returnDate);
            startActivity(intent);
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_search);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_search) {
                return true;
            } else if (id == R.id.nav_tickets) {
                startActivity(new Intent(SearchActivity.this, MyTicketsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(SearchActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });
    }

    private void loadStations() {
        apiService.getServices().enqueue(new Callback<List<ServiceApiModel>>() {
            @Override
            public void onResponse(Call<List<ServiceApiModel>> call, Response<List<ServiceApiModel>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                Set<String> stations = new LinkedHashSet<>();
                for (ServiceApiModel service : response.body()) {
                    if (service.getVilleDepartNom() != null) stations.add(service.getVilleDepartNom());
                    if (service.getVilleArriveeNom() != null) stations.add(service.getVilleArriveeNom());
                }
                ArrayAdapter<String> stationAdapter = new ArrayAdapter<>(
                        SearchActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        new ArrayList<>(stations)
                );
                etDeparture.setAdapter(stationAdapter);
                etDestination.setAdapter(stationAdapter);
            }

            @Override
            public void onFailure(Call<List<ServiceApiModel>> call, Throwable t) {
            }
        });
    }

    private void bindDatePicker(EditText target) {
        target.setFocusable(false);
        target.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(
                    SearchActivity.this,
                    (view, year, month, dayOfMonth) -> target.setText(String.format(Locale.FRANCE, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void bindTimePicker(EditText target) {
        target.setFocusable(false);
        target.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new TimePickerDialog(
                    SearchActivity.this,
                    (view, hourOfDay, minute) -> target.setText(String.format(Locale.FRANCE, "%02d:%02d", hourOfDay, minute)),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
            ).show();
        });
    }
}
