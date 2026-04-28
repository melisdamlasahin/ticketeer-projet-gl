package com.easyrail.app;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultsActivity extends AppCompatActivity {

    private RecyclerView recyclerResults;
    private TextView tvSubtitle;
    private TextView tvTitle;

    private ApiService apiService;
    private ServiceResultAdapter adapter;

    private final List<ServiceResult> allResults = new ArrayList<>();
    private List<ServiceResult> filteredResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        recyclerResults = findViewById(R.id.recyclerResults);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvTitle = findViewById(R.id.tvTitle);

        recyclerResults.setLayoutManager(new LinearLayoutManager(this));

        apiService = RetrofitClient.getClient().create(ApiService.class);

        String departureInput = getIntent().getStringExtra("departure");
        String destinationInput = getIntent().getStringExtra("destination");
        String dateInput = getIntent().getStringExtra("date");
        String timeInput = getIntent().getStringExtra("time");
        boolean roundTrip = getIntent().getBooleanExtra("roundTrip", false);
        String returnDateInput = getIntent().getStringExtra("returnDate");
        boolean selectingReturnTrip = getIntent().getBooleanExtra("selectingReturnTrip", false);
        String outboundServiceId = getIntent().getStringExtra("outboundServiceId");
        String outboundDeparture = getIntent().getStringExtra("outboundDeparture");
        String outboundDestination = getIntent().getStringExtra("outboundDestination");
        String outboundTrainName = getIntent().getStringExtra("outboundTrainName");
        String outboundDate = getIntent().getStringExtra("outboundDate");
        String outboundDepartureTime = getIntent().getStringExtra("outboundDepartureTime");
        String outboundPrice = getIntent().getStringExtra("outboundPrice");

        if (departureInput == null) departureInput = "";
        if (destinationInput == null) destinationInput = "";
        if (dateInput == null) dateInput = "";
        if (returnDateInput == null) returnDateInput = "";

        loadServicesFromApi(
                departureInput,
                destinationInput,
                dateInput,
                timeInput,
                roundTrip,
                returnDateInput,
                selectingReturnTrip,
                outboundServiceId,
                outboundDeparture,
                outboundDestination,
                outboundTrainName,
                outboundDate,
                outboundDepartureTime,
                outboundPrice
        );
    }

    private void loadServicesFromApi(String departureInput,
                                     String destinationInput,
                                     String dateInput,
                                     String timeInput,
                                     boolean roundTrip,
                                     String returnDateInput,
                                     boolean selectingReturnTrip,
                                     String outboundServiceId,
                                     String outboundDeparture,
                                     String outboundDestination,
                                     String outboundTrainName,
                                     String outboundDate,
                                     String outboundDepartureTime,
                                     String outboundPrice) {
        apiService.getServices().enqueue(new Callback<List<ServiceApiModel>>() {
            @Override
            public void onResponse(Call<List<ServiceApiModel>> call, Response<List<ServiceApiModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allResults.clear();

                    for (ServiceApiModel item : response.body()) {
                        String formattedPrice = String.format(Locale.FRANCE, "%.2f €", item.getPrixBase());

                        allResults.add(new ServiceResult(
                                item.getServiceId(),
                                item.getVilleDepartNom(),
                                item.getVilleArriveeNom(),
                                item.getTrainNom(),
                                item.getDateTrajet(),
                                item.getHeureDepart(),
                                formattedPrice,
                                item.getVoie(),
                                item.getRetardMinutes()
                        ));
                    }

                    if (selectingReturnTrip) {
                        filteredResults = filterResults(allResults, destinationInput, departureInput, returnDateInput, timeInput);
                    } else {
                        filteredResults = filterResults(allResults, departureInput, destinationInput, dateInput, timeInput);
                    }

                    if (filteredResults.isEmpty()) {
                        tvTitle.setText(selectingReturnTrip ? "Choisissez votre retour" : "Vos services ferroviaires");
                        tvSubtitle.setText(selectingReturnTrip
                                ? "Aucun trajet retour trouvé pour votre recherche"
                                : "Aucun trajet trouvé pour votre recherche");
                    } else {
                        if (selectingReturnTrip) {
                            tvTitle.setText("Choisissez votre retour");
                            tvSubtitle.setText("Sélectionnez maintenant le trajet retour");
                        } else if (roundTrip) {
                            tvTitle.setText("Choisissez votre aller");
                            tvSubtitle.setText("Sélectionnez d'abord le trajet aller");
                        } else {
                            tvTitle.setText("Vos services ferroviaires");
                            tvSubtitle.setText("Choisissez un trajet pour poursuivre votre réservation");
                        }
                    }

                    adapter = new ServiceResultAdapter(
                            ResultsActivity.this,
                            filteredResults,
                            roundTrip,
                            selectingReturnTrip,
                            outboundDeparture != null ? outboundDeparture : departureInput,
                            outboundDestination != null ? outboundDestination : destinationInput,
                            outboundDate != null ? outboundDate : dateInput,
                            returnDateInput,
                            outboundServiceId,
                            outboundTrainName,
                            outboundDepartureTime,
                            outboundPrice
                    );
                    recyclerResults.setAdapter(adapter);

                } else {
                    Toast.makeText(ResultsActivity.this, "Réponse serveur invalide", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<ServiceApiModel>> call, Throwable t) {
                Toast.makeText(ResultsActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private List<ServiceResult> filterResults(List<ServiceResult> allResults,
                                              String departureInput,
                                              String destinationInput,
                                              String dateInput,
                                              String timeInput) {
        List<ServiceResult> results = new ArrayList<>();

        String dep = departureInput.toLowerCase(Locale.ROOT).trim();
        String dest = destinationInput.toLowerCase(Locale.ROOT).trim();
        String date = dateInput.toLowerCase(Locale.ROOT).trim();
        String time = timeInput != null ? timeInput.toLowerCase(Locale.ROOT).trim() : "";

        for (ServiceResult item : allResults) {
            boolean matchesDeparture = item.getDeparture().toLowerCase(Locale.ROOT).contains(dep);
            boolean matchesDestination = item.getDestination().toLowerCase(Locale.ROOT).contains(dest);
            boolean matchesDate = item.getDate().toLowerCase(Locale.ROOT).contains(date);
            boolean matchesTime = time.isEmpty()
                    || (item.getDepartureTime() != null
                    && item.getDepartureTime().toLowerCase(Locale.ROOT).contains(time));

            if (matchesDeparture && matchesDestination && matchesDate && matchesTime) {
                results.add(item);
            }
        }

        return results;
    }
}
