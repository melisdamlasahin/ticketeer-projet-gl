package com.easyrail.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfirmationActivity extends AppCompatActivity {

    private TextView tvRoute;
    private TextView tvTrainName;
    private TextView tvDate;
    private TextView tvBasePrice;
    private TextView tvTotalPrice;
    private TextView tvReturnSummary;
    private Spinner spinnerTarif;
    private Spinner spinnerClass;
    private Spinner spinnerSeatPreference;
    private Spinner spinnerPaymentMethod;
    private EditText etPassengerName;
    private EditText etPassengerEmail;
    private EditText etPassengerPhone;
    private EditText etCardNumber;
    private Button btnConfirm;

    private ApiService apiService;
    private SessionManager sessionManager;

    private String serviceId = "";
    private String departure = "";
    private String destination = "";
    private String trainName = "";
    private String date = "";
    private String departureTime = "";
    private String initialPrice = "0,00 €";
    private String returnServiceId = "";
    private String returnDeparture = "";
    private String returnDestination = "";
    private String returnTrainName = "";
    private String returnDate = "";
    private String returnDepartureTime = "";
    private String returnPrice = "";

    private boolean spinnerInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        tvRoute = findViewById(R.id.tvRoute);
        tvTrainName = findViewById(R.id.tvTrainName);
        tvDate = findViewById(R.id.tvDate);
        tvBasePrice = findViewById(R.id.tvBasePrice);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvReturnSummary = findViewById(R.id.tvReturnSummary);
        spinnerTarif = findViewById(R.id.spinnerTarif);
        spinnerClass = findViewById(R.id.spinnerClass);
        spinnerSeatPreference = findViewById(R.id.spinnerSeatPreference);
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod);
        etPassengerName = findViewById(R.id.etPassengerName);
        etPassengerEmail = findViewById(R.id.etPassengerEmail);
        etPassengerPhone = findViewById(R.id.etPassengerPhone);
        etCardNumber = findViewById(R.id.etCardNumber);
        btnConfirm = findViewById(R.id.btnConfirm);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);
        if (ApiSessionHandler.redirectToLoginIfSessionMissing(
                this,
                sessionManager,
                "Veuillez vous connecter pour poursuivre votre reservation"
        )) {
            return;
        }

        String serviceIdTemp = getIntent().getStringExtra("serviceId");
        String depTemp = getIntent().getStringExtra("departure");
        String destTemp = getIntent().getStringExtra("destination");
        String trainTemp = getIntent().getStringExtra("trainName");
        String dateTemp = getIntent().getStringExtra("date");
        String departureTimeTemp = getIntent().getStringExtra("departureTime");
        String priceTemp = getIntent().getStringExtra("price");
        String returnServiceTemp = getIntent().getStringExtra("returnServiceId");
        String returnDepartureTemp = getIntent().getStringExtra("returnDeparture");
        String returnDestinationTemp = getIntent().getStringExtra("returnDestination");
        String returnTrainTemp = getIntent().getStringExtra("returnTrainName");
        String returnDateTemp = getIntent().getStringExtra("returnDate");
        String returnDepartureTimeTemp = getIntent().getStringExtra("returnDepartureTime");
        String returnPriceTemp = getIntent().getStringExtra("returnPrice");

        serviceId = serviceIdTemp != null ? serviceIdTemp : "";
        departure = depTemp != null ? depTemp : "";
        destination = destTemp != null ? destTemp : "";
        trainName = trainTemp != null ? trainTemp : "";
        date = dateTemp != null ? dateTemp : "";
        departureTime = departureTimeTemp != null ? departureTimeTemp : "";
        initialPrice = priceTemp != null ? priceTemp : "0,00 €";
        returnServiceId = returnServiceTemp != null ? returnServiceTemp : "";
        returnDeparture = returnDepartureTemp != null ? returnDepartureTemp : "";
        returnDestination = returnDestinationTemp != null ? returnDestinationTemp : "";
        returnTrainName = returnTrainTemp != null ? returnTrainTemp : "";
        returnDate = returnDateTemp != null ? returnDateTemp : "";
        returnDepartureTime = returnDepartureTimeTemp != null ? returnDepartureTimeTemp : "";
        returnPrice = returnPriceTemp != null ? returnPriceTemp : "";

        tvRoute.setText(departure + " → " + destination);
        tvTrainName.setText(trainName);
        tvDate.setText(buildScheduleLabel(date, departureTime));
        tvBasePrice.setText("Prix de base : " + buildBasePriceSummary());
        tvTotalPrice.setText("Prix total : " + buildBasePriceSummary());
        bindReturnSummary();
        prefillPassengerInfo();

        setupTarifSpinner();
        setupStaticSpinner(spinnerClass, new String[]{"Seconde", "Première"});
        setupStaticSpinner(spinnerSeatPreference, new String[]{"Fenêtre", "Couloir", "Espace calme"});
        setupStaticSpinner(spinnerPaymentMethod, new String[]{"Carte", "Apple Pay", "PayPal"});
        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerTarif.getSelectedItem() != null) {
                    calculerTarif(serviceId, spinnerTarif.getSelectedItem().toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnConfirm.setOnClickListener(v -> {
            String selectedTarif = spinnerTarif.getSelectedItem() != null
                    ? spinnerTarif.getSelectedItem().toString()
                    : "Adulte";

            if (!validateBookingForm()) {
                return;
            }

            confirmerAchat(serviceId, returnServiceId, selectedTarif, departure, destination, trainName, date);
        });
    }

    private void setupTarifSpinner() {
        String[] tarifs = {
                "Adulte",
                "Étudiant",
                "Senior",
                "Enfant",
                "Handicap"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                tarifs
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTarif.setAdapter(adapter);

        spinnerTarif.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTarif = spinnerTarif.getSelectedItem().toString();

                if (!spinnerInitialized) {
                    spinnerInitialized = true;
                }

                calculerTarif(serviceId, selectedTarif);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private String mapProfilTarifaire(String selectedTarif) {
        if (selectedTarif == null) return "STANDARD";

        switch (selectedTarif.trim().toLowerCase(Locale.ROOT)) {
            case "étudiant":
            case "etudiant":
                return "ETUDIANT_DECLARE";
            case "senior":
                return "SENIOR_65_PLUS";
            case "enfant":
                return "ENFANT_MOINS_7";
            case "handicap":
                return "HANDICAP_DECLARE";
            case "adulte":
            default:
                return "STANDARD";
        }
    }

    private String getAuthToken() {
        return sessionManager.getAuthToken();
    }

    private void calculerTarif(String serviceId, String selectedTarif) {
        if (serviceId == null || serviceId.trim().isEmpty()) {
            return;
        }

        String clientId = sessionManager.getClientId();
        String authToken = sessionManager.getAuthToken();

        String profilTarifaire = mapProfilTarifaire(selectedTarif);
        AchatBilletRequest request = buildBookingRequest(serviceId, returnServiceId, profilTarifaire, clientId);

        apiService.calculerTarif(authToken, request).enqueue(new Callback<TarificationResponseModel>() {
            @Override
            public void onResponse(Call<TarificationResponseModel> call,
                                   Response<TarificationResponseModel> response) {
                if (ApiSessionHandler.handleUnauthorized(ConfirmationActivity.this, sessionManager, response)) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    TarificationResponseModel tarif = response.body();

                    if (tarif.getPrixBase() != null) {
                        tvBasePrice.setText(
                                String.format(Locale.FRANCE, "Prix de base : %.2f €", tarif.getPrixBase())
                        );
                    }

                    if (tarif.getPrixFinal() != null) {
                        tvTotalPrice.setText(
                                String.format(Locale.FRANCE, "Prix total : %.2f €", tarif.getPrixFinal())
                        );
                    }
                } else {
                    String message = ApiErrorUtils.messageOrFallback(
                            response,
                            "Impossible de calculer le tarif"
                    );
                    Toast.makeText(
                            ConfirmationActivity.this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<TarificationResponseModel> call, Throwable t) {
                Toast.makeText(
                        ConfirmationActivity.this,
                        "Erreur réseau tarif : " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void confirmerAchat(String serviceId,
                                String returnServiceId,
                                String selectedTarif,
                                String departure,
                                String destination,
                                String trainName,
                                String date) {

        if (serviceId == null || serviceId.trim().isEmpty()) {
            Toast.makeText(this, "Service introuvable", Toast.LENGTH_LONG).show();
            return;
        }

        String clientId = sessionManager.getClientId();
        String authToken = sessionManager.getAuthToken();

        String profilTarifaire = mapProfilTarifaire(selectedTarif);
        AchatBilletRequest request = buildBookingRequest(serviceId, returnServiceId, profilTarifaire, clientId);

        btnConfirm.setEnabled(false);

        apiService.confirmerAchat(authToken, request).enqueue(new Callback<AchatBilletResponse>() {
            @Override
            public void onResponse(Call<AchatBilletResponse> call, Response<AchatBilletResponse> response) {
                btnConfirm.setEnabled(true);
                if (ApiSessionHandler.handleUnauthorized(ConfirmationActivity.this, sessionManager, response)) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    AchatBilletResponse res = response.body();

                    Intent intent = new Intent(ConfirmationActivity.this, SuccessActivity.class);
                    intent.putExtra("billetId", res.getBilletId());
                    intent.putExtra("prixFinal", res.getPrixFinal() != null ? res.getPrixFinal() : 0.0);
                    intent.putExtra("message", res.getMessage());
                    intent.putExtra("departure", departure);
                    intent.putExtra("destination", destination);
                    intent.putExtra("trainName", trainName);
                    intent.putExtra("date", date);
                    intent.putExtra("returnDeparture", ConfirmationActivity.this.returnDeparture);
                    intent.putExtra("returnDestination", ConfirmationActivity.this.returnDestination);
                    intent.putExtra("returnTrainName", ConfirmationActivity.this.returnTrainName);
                    intent.putExtra("returnDate", ConfirmationActivity.this.returnDate);
                    intent.putExtra("passengerEmail", etPassengerEmail.getText().toString().trim());
                    startActivity(intent);
                } else {
                    boolean handled = ApiErrorUtils.applyFieldError(response, "emailPassager", etPassengerEmail);
                    handled = ApiErrorUtils.applyFieldError(response, "telephonePassager", etPassengerPhone) || handled;
                    handled = ApiErrorUtils.applyFieldError(response, "serviceId", etPassengerName) || handled;
                    handled = ApiErrorUtils.applyFieldError(response, "clientId", etPassengerName) || handled;
                    String message = handled
                            ? ApiErrorUtils.messageOrFallback(response, "Veuillez corriger les champs")
                            : ApiErrorUtils.messageOrFallback(response, "Erreur serveur");
                    Toast.makeText(
                            ConfirmationActivity.this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<AchatBilletResponse> call, Throwable t) {
                btnConfirm.setEnabled(true);

                Toast.makeText(
                        ConfirmationActivity.this,
                        "Erreur réseau : " + t.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void setupStaticSpinner(Spinner spinner, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void bindReturnSummary() {
        if (blankToNull(returnServiceId) == null) {
            tvReturnSummary.setVisibility(View.GONE);
            return;
        }

        String summary = "Retour : " + returnDeparture + " → " + returnDestination
                + "\n" + returnTrainName
                + "\n" + buildScheduleLabel(returnDate, returnDepartureTime)
                + (blankToNull(returnPrice) != null ? "\nPrix retour : " + returnPrice : "");
        tvReturnSummary.setText(summary);
        tvReturnSummary.setVisibility(View.VISIBLE);
    }

    private String buildScheduleLabel(String date, String departureTime) {
        if (blankToNull(departureTime) == null) {
            return date;
        }
        return date + " • " + departureTime;
    }

    private String buildBasePriceSummary() {
        if (blankToNull(returnPrice) == null) {
            return initialPrice;
        }
        return initialPrice + " + " + returnPrice;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AchatBilletRequest buildBookingRequest(String serviceId,
                                                   String returnServiceId,
                                                   String profilTarifaire,
                                                   String clientId) {
        return new AchatBilletRequest(
                serviceId,
                blankToNull(returnServiceId),
                profilTarifaire,
                clientId,
                mapClass(spinnerClass.getSelectedItem()),
                spinnerSeatPreference.getSelectedItem() != null ? spinnerSeatPreference.getSelectedItem().toString() : null,
                etPassengerName.getText().toString().trim(),
                etPassengerEmail.getText().toString().trim(),
                etPassengerPhone.getText().toString().trim(),
                spinnerPaymentMethod.getSelectedItem() != null ? spinnerPaymentMethod.getSelectedItem().toString().toUpperCase(Locale.ROOT) : "CARTE"
        );
    }

    private String mapClass(Object selectedClass) {
        if (selectedClass == null) {
            return "SECONDE";
        }
        String value = selectedClass.toString().toLowerCase(Locale.ROOT);
        return value.contains("prem") ? "PREMIERE" : "SECONDE";
    }

    private boolean validateBookingForm() {
        if (blankToNull(etPassengerName.getText().toString()) == null) {
            etPassengerName.setError("Nom du passager requis");
            return false;
        }
        String email = etPassengerEmail.getText().toString().trim();
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etPassengerEmail.setError("Email valide requis");
            return false;
        }
        if (blankToNull(etPassengerPhone.getText().toString()) == null) {
            etPassengerPhone.setError("Téléphone requis");
            return false;
        }
        if ("Carte".equals(spinnerPaymentMethod.getSelectedItem()) && etCardNumber.getText().toString().trim().length() < 4) {
            etCardNumber.setError("Saisissez au moins 4 chiffres");
            return false;
        }
        return true;
    }

    private void prefillPassengerInfo() {
        String firstName = sessionManager.getPrenom();
        String lastName = sessionManager.getNom();
        String fullName = (firstName + " " + lastName).trim();
        etPassengerName.setText(fullName);
        etPassengerEmail.setText(sessionManager.getEmail());
        etPassengerPhone.setText(sessionManager.getTelephone());
    }
}
