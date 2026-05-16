package com.easyrail.app;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    @GET("api/services")
    Call<List<ServiceApiModel>> getServices();

    @POST("api/achat/tarif")
    Call<TarificationResponseModel> calculerTarif(@Header("X-Auth-Token") String authToken,
                                                  @Body AchatBilletRequest request);

    @POST("api/achat/confirmer")
    Call<AchatBilletResponse> confirmerAchat(@Header("X-Auth-Token") String authToken,
                                             @Body AchatBilletRequest request);

    @GET("api/billets/{id}")
    Call<TicketApiModel> getBillet(@Header("X-Auth-Token") String authToken,
                                   @Path("id") String id);

    @GET("api/billets/client/{clientId}")
    Call<List<TicketApiModel>> getBilletsByClient(@Header("X-Auth-Token") String authToken,
                                                  @Path("clientId") String clientId);

    @POST("api/billets/{id}/cancel")
    Call<TicketApiModel> cancelBillet(@Header("X-Auth-Token") String authToken,
                                      @Path("id") String billetId);

    @PUT("api/billets/{id}")
    Call<TicketApiModel> updateBillet(@Header("X-Auth-Token") String authToken,
                                      @Path("id") String billetId,
                                      @Body AchatBilletRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("api/auth/logout")
    Call<AuthResponse> logout(@Header("X-Auth-Token") String authToken);

    @GET("api/clients/{clientId}/profile")
    Call<ClientProfileResponse> getProfile(@Header("X-Auth-Token") String authToken,
                                           @Path("clientId") String clientId);

    @PUT("api/clients/{clientId}/profile")
    Call<ClientProfileResponse> updateProfile(@Header("X-Auth-Token") String authToken,
                                              @Path("clientId") String clientId,
                                              @Body UpdateProfileRequest request);
}
