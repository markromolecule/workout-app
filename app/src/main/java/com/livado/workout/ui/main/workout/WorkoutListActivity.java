package com.livado.workout.ui.main.workout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.livado.workout.R;
import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.data.remote.request.UpdateRoutineRequest;
import com.livado.workout.data.remote.request.UpdateWorkoutRequest;
import com.livado.workout.data.remote.response.ApiResponse;
import com.livado.workout.data.remote.response.WorkoutResponse;
import com.livado.workout.data.remote.response.WorkoutResponseWrapper;
import com.livado.workout.data.remote.response.WorkoutUpdateResponse;
import com.livado.workout.ui.main.workout.adapter.WorkoutAdapter;
import com.livado.workout.ui.main.workout.dialog.UpdateDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkoutListActivity extends AppCompatActivity implements UpdateDialog.OnWorkoutUpdatedListener {

    private RecyclerView recyclerView;
    private WorkoutAdapter workoutAdapter;
    private ApiService apiService;
    private int routineID, userId;
    private List<WorkoutResponse> workoutList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        userId = getIntent().getIntExtra("userId", -1);
        routineID = getIntent().getIntExtra("routineID", -1);

        Log.d("WorkoutListActivity", "Received routineID: " + routineID + ", userID: " + userId);

        if (userId == -1 || routineID == -1) {
            Toast.makeText(this, "Invalid user or routine!", Toast.LENGTH_LONG).show();
            Log.e("WorkoutListActivity", "Error: Invalid routineID received. Cannot proceed.");
            finish();
            return;
        }

        setupRecyclerView();
        fetchWorkouts();
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.workoutRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        workoutList = new ArrayList<>();
        workoutAdapter = new WorkoutAdapter(workoutList, userId, routineID, this::showUpdateDialog);
        recyclerView.setAdapter(workoutAdapter);
    }

    public void setupBackButton(View view) {
        findViewById(R.id.backButton).setOnClickListener(v -> {
            finish();
        });
    }

    public void setupConfirmButton(View view) {
        findViewById(R.id.confirmButton).setOnClickListener(v -> {
            Log.d("WorkoutListActivity", "Returning updated routine workouts to RoutineListActivity");
            updateRoutineWorkouts();
            navigateToRoutineList();
        });
    }

    private void updateRoutineWorkouts() {
        // Ensure valid routineID and userId
        if (routineID == -1 || userId <= 0) {
            Log.e("WorkoutListActivity", "Error: routineID or userId is invalid. routineID: " + routineID + ", userId: " + userId);
            Toast.makeText(this, "Routine ID or User ID is missing. Cannot update workouts!", Toast.LENGTH_LONG).show();
            return;
        }

        // Retrieve the updated workouts list
        List<UpdateWorkoutRequest> updatedWorkouts = getUpdatedWorkouts();

        // Log the request data
        Log.d("WorkoutListActivity", "Updating routine workouts for routineID: " + routineID);
        Log.d("WorkoutListActivity", "Updated workouts: " + new Gson().toJson(updatedWorkouts));

        // If no workouts need updating, show a message and return
        if (updatedWorkouts.isEmpty()) {
            Log.d("WorkoutListActivity", "No workouts to update.");
            Toast.makeText(this, "No updates to send.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate workout IDs before making the request
        for (UpdateWorkoutRequest workout : updatedWorkouts) {
            if (workout.getWorkoutID() <= 0) {
                Log.e("WorkoutListActivity", "Invalid workoutID detected: " + workout.getWorkoutID());
                Toast.makeText(this, "Invalid workout ID found. Cannot proceed!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Make a single API call with the list of workouts
        apiService.updateRoutineWorkouts(userId, routineID, updatedWorkouts)
                .enqueue(new Callback<ApiResponse<WorkoutUpdateResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<WorkoutUpdateResponse>> call, @NonNull Response<ApiResponse<WorkoutUpdateResponse>> response) {
                        handleAPIResponse(response);
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<WorkoutUpdateResponse>> call, @NonNull Throwable t) {
                        Log.e("WorkoutListActivity", "API request failed", t);
                        Toast.makeText(WorkoutListActivity.this, "Network error occurred", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleAPIResponse(Response<ApiResponse<WorkoutUpdateResponse>> response) {
        // Log full response details for debugging purposes
        Log.d("WorkoutListActivity", "Full API response body: " + new Gson().toJson(response.body()));

        if (response.isSuccessful()) {
            ApiResponse<WorkoutUpdateResponse> apiResponse = response.body();
            if (apiResponse != null && apiResponse.isSuccess()) {
                WorkoutUpdateResponse updateResponse = apiResponse.getData();
                Log.d("WorkoutListActivity", "Updated: " + updateResponse.getUpdated());
                Log.d("WorkoutListActivity", "Inserted: " + updateResponse.getInserted());

                // Log the list of updated workouts received
                List<WorkoutResponse> updatedWorkouts = updateResponse.getWorkouts();
                if (updatedWorkouts != null && !updatedWorkouts.isEmpty()) {
                    // Log each updated workout for verification
                    for (WorkoutResponse workout : updatedWorkouts) {
                        Log.d("WorkoutListActivity", "Updated Workout: " + workout.getWorkoutName() + ", Sets: " + workout.getSets() + ", Reps: " + workout.getReps());
                    }
                    Toast.makeText(WorkoutListActivity.this, "Routine workouts updated successfully!", Toast.LENGTH_SHORT).show();
                    navigateToRoutineList();
                } else {
                    // Handle case where no workouts were updated
                    Log.e("WorkoutListActivity", "No workouts were updated or inserted.");
                    Toast.makeText(WorkoutListActivity.this, "No updates made to the workouts.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Handle failure from API response
                Log.e("WorkoutListActivity", "API Response Failure: " + apiResponse.getMessage());
                Toast.makeText(WorkoutListActivity.this, "Failed to update workouts", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Log the response code and body when the API call fails
            Log.e("WorkoutListActivity", "API Request Failed with code: " + response.code());
            try {
                if (response.errorBody() != null) {
                    String errorBody = response.errorBody().string();
                    Log.e("WorkoutListActivity", "API Error Body: " + errorBody);
                }
            } catch (IOException e) {
                Log.e("WorkoutListActivity", "Error reading error body", e);
            }
            Toast.makeText(WorkoutListActivity.this, "Error updating routine workouts", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToRoutineList() {
        // Store the routine as selected in SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Add the routineID to the selected routines list
        Set<String> selectedRoutines = new HashSet<>(sharedPreferences.getStringSet("LAST_SELECTED_ROUTINES", new HashSet<>()));
        selectedRoutines.add(String.valueOf(routineID));  // Add the routineID as selected
        editor.putStringSet("LAST_SELECTED_ROUTINES", selectedRoutines);
        editor.apply();

        // Log the selected routine for debugging
        Log.d("WorkoutListActivity", "Routine selected with ID: " + routineID);

        // Navigate to RoutineListActivity
        Intent intent = new Intent(WorkoutListActivity.this, RoutineListActivity.class);
        intent.putExtra("userId", userId);  // Pass the userId
        startActivity(intent);
        finish();
    }

    private List<UpdateWorkoutRequest> getUpdatedWorkouts() {
        List<UpdateWorkoutRequest> updatedWorkouts = new ArrayList<>();
        for (WorkoutResponse workout : workoutList) {
            if (workout.getWorkoutId() == 0) {
                Log.e("WorkoutListActivity", "Invalid workoutID detected: " + workout);
                continue;
            }

            Log.d("WorkoutListActivity", "Preparing to update: ID=" + workout.getWorkoutId() +
                    " Sets=" + workout.getSets() + " Reps=" + workout.getReps());

            updatedWorkouts.add(new UpdateWorkoutRequest(
                    userId,       // userID
                    routineID,    // routineID
                    workout.getWorkoutId(),  // workoutID
                    workout.getSets(),       // sets
                    workout.getReps(),       // reps
                    workout.getDuration()    // duration
            ));
        }

        Log.d("WorkoutListActivity", "Final List Sent to API: " + updatedWorkouts);
        return updatedWorkouts;
    }

    private void fetchWorkouts() {
        Log.d("WorkoutListActivity", "Fetching workouts for routineID: " + routineID);

        apiService.getRoutineWorkouts(userId, routineID).enqueue(new Callback<ApiResponse<List<WorkoutResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<WorkoutResponse>>> call,
                                   @NonNull Response<ApiResponse<List<WorkoutResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<WorkoutResponse> workouts = response.body().getData();

                    Log.d("WorkoutListActivity", "Fetched " + workouts.size() + " workouts.");
                    workoutList.clear();
                    if (workouts != null) {
                        workoutList.addAll(workouts);  // Ensure no duplicates are added
                    }

                    workoutAdapter.notifyDataSetChanged();
                } else {
                    handleFetchError(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<WorkoutResponse>>> call, @NonNull Throwable t) {
                Toast.makeText(WorkoutListActivity.this, "Error fetching workouts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleFetchError(@NonNull Response<ApiResponse<List<WorkoutResponse>>> response) {
        try {
            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            Log.e("WorkoutListActivity", "Failed to fetch workouts: " + errorBody);
        } catch (IOException e) {
            Log.e("WorkoutListActivity", "Error parsing error body", e);
        }
    }

    private void showUpdateDialog(WorkoutResponse workout) {
        Log.d("WorkoutListActivity", "Opening UpdateDialog for workout ID: " + workout.getWorkoutId() + ", RoutineID: " + routineID + ", UserID: " + userId);

        UpdateDialog updateDialog = new UpdateDialog(this, userId, routineID, workout.getWorkoutId(), this::onWorkoutUpdated);
        updateDialog.show();
    }

    @Override
    public void onWorkoutUpdated(WorkoutResponse updatedWorkout) {
        Log.d("WorkoutListActivity", "Received updated workout with ID: " + updatedWorkout.getWorkoutId());

        for (int i = 0; i < workoutList.size(); i++) {
            if (workoutList.get(i).getWorkoutId() == updatedWorkout.getWorkoutId()) {
                workoutList.set(i, updatedWorkout);
                workoutAdapter.notifyItemChanged(i);
                break;
            }
        }
    }
}