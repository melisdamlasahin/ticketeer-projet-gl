package com.easyrail.app;

import android.widget.TextView;

import com.google.gson.Gson;

import java.util.Map;

import retrofit2.Response;

public final class ApiErrorUtils {
    private static final Gson GSON = new Gson();

    private ApiErrorUtils() {
    }

    public static ApiErrorResponse parse(Response<?> response) {
        try {
            if (response == null || response.errorBody() == null) {
                return null;
            }
            return GSON.fromJson(response.errorBody().charStream(), ApiErrorResponse.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String messageOrFallback(Response<?> response, String fallback) {
        ApiErrorResponse apiError = parse(response);
        if (apiError != null && hasValue(apiError.getMessage())) {
            return apiError.getMessage();
        }
        return fallback;
    }

    public static boolean applyFieldError(Response<?> response, String fieldName, TextView view) {
        ApiErrorResponse apiError = parse(response);
        if (apiError == null || apiError.getFieldErrors() == null) {
            return false;
        }
        String fieldMessage = apiError.getFieldErrors().get(fieldName);
        if (!hasValue(fieldMessage)) {
            return false;
        }
        view.setError(fieldMessage);
        return true;
    }

    public static String firstFieldError(Response<?> response) {
        ApiErrorResponse apiError = parse(response);
        if (apiError == null || apiError.getFieldErrors() == null || apiError.getFieldErrors().isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> entry : apiError.getFieldErrors().entrySet()) {
            if (hasValue(entry.getValue())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
