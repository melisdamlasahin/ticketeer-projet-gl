package com.easyrail.app;

import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Response;

public final class ApiSessionHandler {

    private ApiSessionHandler() {
    }

    public static boolean handleUnauthorized(AppCompatActivity activity,
                                             SessionManager sessionManager,
                                             Response<?> response) {
        if (response == null) {
            return false;
        }
        return handleUnauthorizedStatus(activity, sessionManager, response.code());
    }

    public static boolean handleUnauthorizedStatus(AppCompatActivity activity,
                                                   SessionManager sessionManager,
                                                   int statusCode) {
        if (!isUnauthorizedStatus(statusCode)) {
            return false;
        }
        sessionManager.clear();
        Toast.makeText(activity, "Votre session a expire. Veuillez vous reconnecter.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        return true;
    }

    public static boolean redirectToLoginIfSessionMissing(AppCompatActivity activity,
                                                          SessionManager sessionManager,
                                                          String message) {
        if (sessionManager.isLoggedIn()) {
            return false;
        }
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        return true;
    }

    public static boolean isUnauthorizedStatus(int statusCode) {
        return statusCode == 401 || statusCode == 403;
    }
}
