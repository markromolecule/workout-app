package com.livado.workout.data.remote.api;

import com.livado.workout.data.remote.request.LoginRequest;
import com.livado.workout.data.remote.request.SignupRequest;
import com.livado.workout.data.remote.response.LoginResponse;
import com.livado.workout.data.remote.response.WorkoutResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

// End-point Connection
public interface ApiService {
    @POST("login.php")
    @Headers("Content-Type: application/json")
    Call<LoginResponse> loginUser(@Body LoginRequest loginRequest);

    @POST("signup.php")
    @Headers("Content-Type: application/json")
    Call<LoginResponse> signUp(@Body SignupRequest signUpRequest);

    @Multipart
    @POST("add_workout.php")
    Call<LoginResponse> addWorkout(
            @Part("workoutName") RequestBody workoutName,
            @Part("workoutCategory") RequestBody workoutCategory,
            @Part("workoutDescription") RequestBody workoutDescription,
            @Part("workoutDifficulty") RequestBody workoutDifficulty,
            @Part("workoutEquipment") RequestBody workoutEquipment,
            @Part MultipartBody.Part workoutImage
    );

    @GET("add_workout.php")
    Call<List<WorkoutResponse>> getWorkouts();
}
