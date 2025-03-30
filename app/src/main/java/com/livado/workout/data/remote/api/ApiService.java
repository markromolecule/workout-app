package com.livado.workout.data.remote.api;

import com.livado.workout.data.remote.request.LoginRequest;
import com.livado.workout.data.remote.request.RoutineSelectionRequest;
import com.livado.workout.data.remote.request.SignupRequest;
import com.livado.workout.data.remote.request.UpdateRoutineRequest;
import com.livado.workout.data.remote.request.UpdateWorkoutRequest;
import com.livado.workout.data.remote.response.ApiResponse;
import com.livado.workout.data.remote.response.LoginResponse;
import com.livado.workout.data.remote.response.RoutineListResponse;
import com.livado.workout.data.remote.response.RoutineProgressResponse;
import com.livado.workout.data.remote.response.RoutineResponse;
import com.livado.workout.data.remote.response.UpdateResponse;
import com.livado.workout.data.remote.response.UserProfileResponse;
import com.livado.workout.data.remote.response.WorkoutResponse;
import com.livado.workout.data.remote.response.WorkoutResponseWrapper;
import com.livado.workout.data.remote.response.WorkoutUpdateResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Handles user authentication
    @POST("controller/LoginController.php")
    @Headers("Content-Type: application/json")
    Call<LoginResponse> loginUser(
            @Body LoginRequest loginRequest);

    @POST("controller/SignupController.php")
    @Headers("Content-Type: application/json")
    Call<LoginResponse> signUp(
            @Body SignupRequest signUpRequest);

    // Fetch workout and routine details
    @GET("controller/WorkoutController.php")
    @Headers("Content-Type: application/json")
    Call<ApiResponse<WorkoutResponse>> getWorkoutByID(
            @Query("workout_id") int workoutID,
            @Query("user_id") int userId,
            @Query("routine_id") int routineId);

    @GET("controller/RoutineController.php")
    Call<RoutineProgressResponse> getRoutineByID(
            @Query("user_id") int userID,
            @Query("routineID") int routineID);


    @GET("controller/RoutineController.php")
    @Headers("Content-Type: application/json")
    Call<RoutineListResponse> getRoutines(
            @Query("user_id") int userId);

    @POST("controller/WorkoutController.php")
    Call<WorkoutResponseWrapper> updateWorkout(
            @Body UpdateWorkoutRequest request);

    @PUT("controller/WorkoutController.php")
    Call<ApiResponse<WorkoutUpdateResponse>> updateRoutineWorkouts(
            @Query("user_id") int userId,
            @Query("routine_id") int routineId,
            @Body List<UpdateWorkoutRequest> request
    );

    @FormUrlEncoded
    @POST("controller/WorkoutProgressController.php")
    Call<ApiResponse> startWorkoutProgress(
            @Field("user_id") int userId,
            @Field("routine_id") int routineId,
            @Field("workout_id") int workoutId,
            @Field("status") String status);

    @FormUrlEncoded
    @POST("controller/WorkoutProgressController.php")
    Call<ApiResponse<Void>> startRoutineProgress(
            @Field("user_id") int userId,
            @Field("routine_id") int routineId
    );

    @GET("controller/WorkoutProgressController.php")
    Call<ApiResponse<List<RoutineResponse>>> getInProgressRoutines(
            @Query("user_id") int userId);
    @PUT("controller/WorkoutProgressController.php")
    @Headers("Content-Type: application/json")
    Call<ApiResponse> completeWorkoutProgress(
            @Body RequestBody requestBody);

    @PUT("controller/WorkoutProgressController.php")
    @Headers("Content-Type: application/json")
    Call<ApiResponse> completeRoutineProgress(
            @Body RequestBody requestBody
    );

    @GET("controller/RoutineController.php")
    Call<ApiResponse<List<RoutineResponse>>> getCompletedRoutines(
            @Query("user_id") int userId
    );

    @GET("controller/WorkoutController.php")
    Call<ApiResponse<List<WorkoutResponse>>> getRoutineWorkouts(
            @Query("user_id") int userId,
            @Query("routine_id") int routineId
    );

    @GET("controller/WorkoutController.php")
    Call<ApiResponse<List<WorkoutResponse>>> getRoutineWorkoutsData(
            @Query("user_id") int userId,
            @Query("routine_id") int routineId
    );


    // Handles user profile updates
    @GET("controller/UserController.php")
    Call<UserProfileResponse> getUserProfile(
            @Query("id") int userId);

    @Multipart
    @POST("controller/UserController.php")
    Call<UpdateResponse> updateUserProfile(
            @Part("user_id") RequestBody userId,
            @Part("user_fullname") RequestBody fullName,
            @Part("user_username") RequestBody username,
            @Part("user_email") RequestBody email,
            @Part("height_cm") RequestBody height,
            @Part("weight_kg") RequestBody weight,
            @Part("gender") RequestBody gender,
            @Part MultipartBody.Part profileImage
    );
}