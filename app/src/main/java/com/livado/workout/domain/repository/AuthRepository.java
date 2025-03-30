package com.livado.workout.domain.repository;

import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.data.remote.request.SignupRequest;
import com.livado.workout.data.remote.response.LoginResponse;

import retrofit2.Call;

public class AuthRepository {
    private final ApiService apiService;

    public AuthRepository() {
        this.apiService = RetrofitClient.getClient().create(ApiService.class);
    }
    public Call<LoginResponse> signUp(String fullName, String username, String email, String password) {
        SignupRequest request = new SignupRequest(fullName, username, email, password);
        return apiService.signUp(request);
    }
}
