package com.livado.workout.domain.repository;

import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.data.remote.response.LoginResponse;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;

public class WorkoutRepository {
    private final ApiService apiService;

    public WorkoutRepository() {
        this.apiService = RetrofitClient.getClient().create(ApiService.class);
    }

    public Call<LoginResponse> createWorkout(String workout, String category, String description,
                                             String equipment, String difficulty, String imageUrl) {
        // Create the RequestBody instances
        RequestBody workoutName = RequestBody.create(MediaType.parse("text/plain"), workout);
        RequestBody workoutCategory = RequestBody.create(MediaType.parse("text/plain"), category);
        RequestBody workoutDescription = RequestBody.create(MediaType.parse("text/plain"), description);
        RequestBody workoutDifficulty = RequestBody.create(MediaType.parse("text/plain"), difficulty);
        RequestBody workoutEquipment = RequestBody.create(MediaType.parse("text/plain"), equipment);

        // Convert image URL to MultipartBody.Part if there's an image to upload
        MultipartBody.Part workoutImage = null;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            File imageFile = new File(imageUrl);
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), imageFile);
            workoutImage = MultipartBody.Part.createFormData("workoutImage", imageFile.getName(), requestBody);
        }

        // Call the API with the properly formatted parameters
        return apiService.addWorkout(workoutName, workoutCategory, workoutDescription,
                workoutDifficulty, workoutEquipment, workoutImage);
    }
}