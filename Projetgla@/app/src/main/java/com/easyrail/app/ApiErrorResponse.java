package com.easyrail.app;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ApiErrorResponse {
    @SerializedName("status")
    private Integer status;

    @SerializedName("error")
    private String error;

    @SerializedName("message")
    private String message;

    @SerializedName("fieldErrors")
    private Map<String, String> fieldErrors;

    public Integer getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
