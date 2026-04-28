package com.easyrail.app;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String LEGACY_PREFS_NAME = "EasyRailPrefs";
    private static final String PREFS_NAME = "TicketeerPrefs";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        migrateLegacyPrefs(context);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean("isLoggedIn", false)
                && hasValue(getClientId())
                && hasValue(getAuthToken());
    }

    public String getClientId() {
        return prefs.getString("clientId", null);
    }

    public String getAuthToken() {
        return prefs.getString("authToken", null);
    }

    public String getNom() {
        return prefs.getString("nom", "");
    }

    public String getPrenom() {
        return prefs.getString("prenom", "");
    }

    public String getEmail() {
        return prefs.getString("email", "");
    }

    public String getSexe() {
        return prefs.getString("sexe", "");
    }

    public String getDateNaissance() {
        return prefs.getString("dateNaissance", "");
    }

    public String getTelephone() {
        return prefs.getString("telephone", "");
    }

    public void saveAuth(AuthResponse authResponse) {
        prefs.edit()
                .putBoolean("isLoggedIn", true)
                .putString("clientId", authResponse.getClientId())
                .putString("authToken", authResponse.getAuthToken())
                .putString("nom", safe(authResponse.getNom()))
                .putString("prenom", safe(authResponse.getPrenom()))
                .putString("email", safe(authResponse.getEmail()))
                .putString("sexe", safe(authResponse.getSexe()))
                .putString("dateNaissance", safe(authResponse.getDateNaissance()))
                .putString("telephone", safe(authResponse.getTelephone()))
                .apply();
    }

    public void saveProfile(ClientProfileResponse profile) {
        prefs.edit()
                .putString("nom", safe(profile.getNom()))
                .putString("prenom", safe(profile.getPrenom()))
                .putString("email", safe(profile.getEmail()))
                .putString("sexe", safe(profile.getSexe()))
                .putString("dateNaissance", safe(profile.getDateNaissance()))
                .putString("telephone", safe(profile.getTelephone()))
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    private void migrateLegacyPrefs(Context context) {
        if (prefs.contains("clientId") || prefs.contains("authToken") || prefs.contains("isLoggedIn")) {
            return;
        }

        SharedPreferences legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE);
        if (!legacyPrefs.contains("clientId") && !legacyPrefs.contains("authToken") && !legacyPrefs.contains("isLoggedIn")) {
            return;
        }

        prefs.edit()
                .putBoolean("isLoggedIn", legacyPrefs.getBoolean("isLoggedIn", false))
                .putString("clientId", legacyPrefs.getString("clientId", null))
                .putString("authToken", legacyPrefs.getString("authToken", null))
                .putString("nom", legacyPrefs.getString("nom", ""))
                .putString("prenom", legacyPrefs.getString("prenom", ""))
                .putString("email", legacyPrefs.getString("email", ""))
                .putString("sexe", legacyPrefs.getString("sexe", ""))
                .putString("dateNaissance", legacyPrefs.getString("dateNaissance", ""))
                .putString("telephone", legacyPrefs.getString("telephone", ""))
                .apply();
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
