package com.livado.workout.ui.main.workout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.reflect.TypeToken;
import com.livado.workout.R;
import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.data.remote.request.WorkoutProgressRequest;
import com.livado.workout.data.remote.response.ApiResponse;
import com.livado.workout.data.remote.response.WorkoutResponse;
import com.livado.workout.ui.main.home.HomeMainActivity;
import com.livado.workout.ui.main.workout.adapter.RoutineWorkoutsAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoutineMainActivity
        extends AppCompatActivity {

    // UI Components
    private ImageView workoutImageView;
    private TextView workoutNameTextView, setsTextView, repsTextView, durationTextView;
    private ImageButton playPauseButton, stopButton, confirmButton;
    private RecyclerView recyclerView;

    // Data Variables
    private ApiService apiService;
    private RoutineWorkoutsAdapter workoutAdapter;
    private List<WorkoutResponse> workouts = new ArrayList<>();
    private Set<String> completedWorkouts = new HashSet<>();  // Track completed workout IDs
    private WorkoutResponse currentWorkout;
    private int selectedRoutineID, userId;
    private boolean isPlaying = false;
    private int remainingSets, remainingTime;
    private int totalWorkoutCount; // Store total workouts at the start
    private int completedWorkoutsCount = 0;  // Track completed workouts

    // Timer Handling
    private Handler handler = new Handler();
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_routine_main);

        initializeApiService();
        initializeUserData();
        initializeUIComponents();
        initializeRecyclerView();

        fetchRoutineWorkouts();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        Set<String> inProgressRoutines = sharedPreferences.getStringSet("IN_PROGRESS_ROUTINES", new HashSet<>());
        Set<String> completedRoutines = sharedPreferences.getStringSet("COMPLETED_ROUTINES", new HashSet<>());

        if (inProgressRoutines.isEmpty() && completedRoutines.isEmpty()) {
            Log.d("RoutineMainActivity", "No routines found. Clearing preferences and fetching fresh data.");
            clearRoutinePreferences();
            restoreWorkoutProgress();
            fetchRoutineWorkouts();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveWorkoutProgress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveWorkoutProgress();
    }

    private void restoreWorkoutProgress() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);

        int lastWorkoutID = sharedPreferences.getInt("lastWorkoutID", -1);
        remainingSets = sharedPreferences.getInt("remainingSets", 0);
        remainingTime = sharedPreferences.getInt("remainingTime", 0);
        isPlaying = sharedPreferences.getBoolean("isPlaying", false);
        completedWorkoutsCount = sharedPreferences.getInt("completedWorkoutsCount", 0);

        if (lastWorkoutID != -1) {
            for (WorkoutResponse workout : workouts) {
                if (workout.getWorkoutId() == lastWorkoutID) {
                    currentWorkout = workout;
                    break;
                }
            }
        }
        if (currentWorkout != null) {
            Log.d("RoutineMainActivity", "Restoring previous workout: " + currentWorkout.getWorkoutName());
            displayWorkout(currentWorkout);

            if (isPlaying) {
                startTimer();
            }
        }
    }

    private void initializeApiService() {
        apiService = RetrofitClient.getClient().create(ApiService.class);
    }

    private void initializeUserData() {
        selectedRoutineID = getIntent().getIntExtra("selectedRoutineID", -1);
        userId = getIntent().getIntExtra("userId", -1);

        Log.d("RoutineMainActivity", "Intent Extras - selectedRoutineID: " + selectedRoutineID + ", userID: " + userId);

        // Retrieve the workout data as JSON
        String workoutJson = getIntent().getStringExtra("workoutJson");
        if (workoutJson != null) {
            workouts = new Gson().fromJson(workoutJson, new TypeToken<List<WorkoutResponse>>() {
            }.getType());
        }

        if (selectedRoutineID == -1) {
            Log.e("RoutineMainActivity", "Failed to receive a valid selectedRoutineID!");
        }
    }

    private void initializeUIComponents() {
        workoutImageView = findViewById(R.id.workoutImage);
        workoutNameTextView = findViewById(R.id.workoutName);
        setsTextView = findViewById(R.id.setsNum);
        repsTextView = findViewById(R.id.repsNum);
        durationTextView = findViewById(R.id.timerText);

        playPauseButton = findViewById(R.id.playButton);
        stopButton = findViewById(R.id.stopButton);
        confirmButton = findViewById(R.id.confirmButton);
    }

    private void initializeRecyclerView() {
        recyclerView = findViewById(R.id.workoutProgressRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void fetchRoutineWorkouts() {
        if (userId == -1 || selectedRoutineID == -1) {
            Log.e("RoutineMainActivity", "Invalid user or routine ID!");
            Toast.makeText(this, "Invalid user or routine ID!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("RoutineMainActivity", "Fetching workouts for selectedRoutineID: " + selectedRoutineID);

        apiService.getRoutineWorkoutsData(userId, selectedRoutineID).enqueue(new Callback<ApiResponse<List<WorkoutResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<WorkoutResponse>>> call, @NonNull Response<ApiResponse<List<WorkoutResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<WorkoutResponse> workoutData = response.body().getData();

                    // Log the raw response for debugging
                    Log.d("RoutineMainActivity", "API Response Raw Data: " + new Gson().toJson(response.body()));

                    // Clear previous data before adding new data
                    workouts.clear();

                    if (workoutData != null && !workoutData.isEmpty()) {
                        workouts.addAll(workoutData);

                        // Initialize total workout count
                        initializeWorkoutCount();

                        Log.d("RoutineMainActivity", "Fetched " + workouts.size() + " workouts.");
                        for (WorkoutResponse workout : workouts) {
                            Log.d("RoutineMainActivity", "Workout ID: " + workout.getWorkoutId() + ", Name: " + workout.getWorkoutName());
                        }
                    } else {
                        Log.e("RoutineMainActivity", "No workouts found!");
                        Toast.makeText(RoutineMainActivity.this, "No workouts available!", Toast.LENGTH_SHORT).show();
                    }

                    // Handle RecyclerView and adapter
                    if (workoutAdapter == null) {
                        workoutAdapter = new RoutineWorkoutsAdapter(workouts, RoutineMainActivity.this, RoutineMainActivity.this::onWorkoutClicked);
                        recyclerView.setAdapter(workoutAdapter);
                    } else {
                        workoutAdapter.updateWorkouts(workouts);
                    }
                } else {
                    Log.e("RoutineMainActivity", "Failed to fetch workouts: " + response.message());
                    Toast.makeText(RoutineMainActivity.this, "Error fetching workouts.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<WorkoutResponse>>> call, @NonNull Throwable t) {
                Log.e("RoutineMainActivity", "API request failed", t);
                Toast.makeText(RoutineMainActivity.this, "Error fetching workouts.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeWorkoutCount() {
        totalWorkoutCount = workouts.size(); // Set the total workout count once workouts are fetched
        Log.d("RoutineMainActivity", "Total Workouts: " + totalWorkoutCount);
    }

    private void onWorkoutClicked(WorkoutResponse workout) {
        if (workout == null) return;
        currentWorkout = workout;

        remainingSets = workout.getSets();
        remainingTime = workout.getDuration();

        displayWorkout(workout);
        setupButtonListeners();
    }

    private void displayWorkout(WorkoutResponse workout) {
        workoutNameTextView.setText(workout.getWorkoutName());
        setsTextView.setText(String.valueOf(remainingSets));
        repsTextView.setText(String.valueOf(workout.getReps()));
        durationTextView.setText(formatTime(remainingTime));

        Glide.with(this).load(workout.getWorkoutImagePath())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(workoutImageView);

        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.pause_circle);
        } else {
            playPauseButton.setImageResource(R.drawable.play_circle);
        }
    }

    private void setupButtonListeners() {
        playPauseButton.setOnClickListener(v -> toggleTimer());
        stopButton.setOnClickListener(v -> stopWorkout());
        confirmButton.setOnClickListener(v -> handleConfirmButtonClick());
    }

    private void toggleTimer() {
        if (currentWorkout == null) {
            Toast.makeText(this, "No workout selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPlaying) {
            handler.removeCallbacks(timerRunnable);
            playPauseButton.setImageResource(R.drawable.play_circle);
        } else {
            startWorkoutProgress();
            startTimer();
            playPauseButton.setImageResource(R.drawable.pause_circle);
        }
        isPlaying = !isPlaying;
    }

    private void startWorkoutProgress() {
        if (currentWorkout == null) return;

        String workoutStatus = "in-progress";

        Log.d("RoutineMainActivity", "Attempting to start workout progress. Selected Workout ID: " + currentWorkout.getWorkoutId());

        if (currentWorkout.getWorkoutId() == 0) {
            Log.e("RoutineMainActivity", "Error: Invalid workout ID. Cannot start workout progress!");
            Toast.makeText(this, "Invalid workout ID!", Toast.LENGTH_SHORT).show();
            return;
        }

        WorkoutProgressRequest request = new WorkoutProgressRequest(userId, selectedRoutineID, currentWorkout.getWorkoutId(), workoutStatus);

        apiService.startWorkoutProgress(userId, selectedRoutineID, currentWorkout.getWorkoutId(), "in-progress").enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                Log.d("RoutineMainActivity", "Response received for workout progress start");

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse responseBody = response.body();
                    Log.d("RoutineMainActivity", "API Success: " + responseBody.isSuccess());
                    Log.d("RoutineMainActivity", "API Message: " + responseBody.getMessage());

                    if (responseBody.isSuccess()) {
                        Log.d("RoutineMainActivity", "Workout started successfully: " + currentWorkout.getWorkoutName());
                    } else {
                        Log.e("RoutineMainActivity", "Failed to start workout progress: " + responseBody.getMessage());
                    }
                } else {
                    Log.e("RoutineMainActivity", "Unexpected API response. Message: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Log.e("RoutineMainActivity", "API request failed", t);
            }
        });
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (remainingTime > 0) {
                    remainingTime--;
                    durationTextView.setText(formatTime(remainingTime));
                    handler.postDelayed(this, 1000);
                } else if (remainingSets > 1) {
                    remainingSets--;
                    remainingTime = currentWorkout.getDuration();
                    setsTextView.setText(String.valueOf(remainingSets));
                    handler.postDelayed(this, 1000);
                } else {
                    stopWorkout();
                }
            }
        };
        handler.post(timerRunnable);
    }

    private void stopWorkout() {
        if (currentWorkout == null) return;

        handler.removeCallbacks(timerRunnable);
        remainingTime = 0;
        remainingSets = 0;
        durationTextView.setText(formatTime(0));
        setsTextView.setText("0");
        isPlaying = false;
        stopButton.setImageResource(R.drawable.stop_circle);

        markWorkoutAsCompleted(currentWorkout);
    }

    private void markWorkoutAsCompleted(WorkoutResponse workout) {
        if (workout == null || workoutAdapter == null) {
            Log.e("RoutineMainActivity", "Error: Current workout or adapter is null!");
            return;
        }

        Log.d("RoutineMainActivity", "Marking workout as completed: " + workout.getWorkoutId());

        try {
            // Create JSON body to send with the request
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", userId);
            jsonObject.put("routine_id", selectedRoutineID);
            jsonObject.put("workout_id", workout.getWorkoutId());
            jsonObject.put("status", "completed");

            // Create request body
            RequestBody requestBody = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json; charset=utf-8"));

            // Make the API call to mark the workout as completed
            apiService.completeWorkoutProgress(requestBody).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Log.d("RoutineMainActivity", "Workout completed: " + workout.getWorkoutName());

                        initializeWorkoutCount();

                        // Mark this workout as completed locally
                        workout.setCompleted(true);

                        // Add the workout to the completed workouts set
                        completedWorkouts.add(String.valueOf(workout.getWorkoutId()));

                        // Update the adapter for this specific completed workout
                        workoutAdapter.updateCompletedWorkouts(workout);

                        // Move the completed workout to the bottom of the list
                        workoutAdapter.moveWorkoutToBottom(workout);

                        // Increment the completed workouts count
                        completedWorkoutsCount++;

                        // Show routine completed dialog if all workouts are completed
                        if (completedWorkoutsCount == workouts.size()) {
                            showRoutineCompletedDialog(); // Show congratulation dialog
                        }

                        // Find position and notify item change to force layout update
                        int position = workouts.indexOf(workout);
                        if (position != -1) {
                            workoutAdapter.notifyItemChanged(position);
                        }

                        // Reset current workout after completion
                        currentWorkout = null;
                    } else {
                        Log.e("RoutineMainActivity", "Failed to complete workout: " + response.message());
                        Toast.makeText(RoutineMainActivity.this, "Failed to complete workout", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                    Log.e("RoutineMainActivity", "API request failed", t);
                    Toast.makeText(RoutineMainActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (JSONException e) {
            Log.e("RoutineMainActivity", "JSON Exception", e);
        }
    }

    private void handleConfirmButtonClick() {
        new android.app.AlertDialog.Builder(RoutineMainActivity.this)
                .setTitle("Finish Routine")
                .setMessage("Are you sure you want to finish all workouts and mark the routine as completed?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // When the user confirms, mark all workouts as completed and update the routine status
                    for (WorkoutResponse workout : workouts) {
                        initializeWorkoutCount();
                        markWorkoutAsCompleted(workout);  // Pass each workout to mark as completed
                    }
                    markRoutineAsCompleted(userId);  // Mark the entire routine as completed
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();  // Close the dialog if "No" is clicked
                })
                .show();
    }

    private boolean isRoutineAlreadyInProgress() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        Set<String> inProgressRoutines = sharedPreferences.getStringSet("IN_PROGRESS_ROUTINES", new HashSet<>());
        return inProgressRoutines.contains(String.valueOf(selectedRoutineID));
    }

    private void navigateToHomeActivity() {
        Intent intent = new Intent(RoutineMainActivity.this, HomeMainActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
        finish();
    }

    private void markRoutineAsCompleted(int userId) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", userId);
            jsonObject.put("routine_id", selectedRoutineID);
            jsonObject.put("status", "completed");

            RequestBody requestBody = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json; charset=utf-8"));
            Log.d("RoutineMainActivity", "Sending completeRoutineProgress request: " + jsonObject.toString());

            apiService.completeRoutineProgress(requestBody).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse responseBody = response.body();
                        Log.d("RoutineMainActivity", "API Success: " + responseBody.isSuccess());
                        Log.d("RoutineMainActivity", "API Message: " + responseBody.getMessage());

                        if (responseBody.isSuccess()) {
                            Log.d("RoutineMainActivity", "Routine marked as completed successfully.");
                            Toast.makeText(RoutineMainActivity.this, "Routine completed!", Toast.LENGTH_SHORT).show();

                            // Now that the routine is successfully marked as completed, update the shared preferences
                            markRoutineAsCompletedInSharedPreferences();

                            // Navigate to home or update UI here
                            navigateToHomeActivity();
                        } else {
                            Log.e("RoutineMainActivity", "Failed to complete routine: " + responseBody.getMessage());
                            Toast.makeText(RoutineMainActivity.this, "Routine completion failed!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("RoutineMainActivity", "Unexpected API response. Message: " + response.message());
                        Toast.makeText(RoutineMainActivity.this, "Failed to complete routine", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                    Log.e("RoutineMainActivity", "API request failed", t);
                    Toast.makeText(RoutineMainActivity.this, "Network error, could not mark routine as completed", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (JSONException e) {
            Log.e("RoutineMainActivity", "JSON Exception", e);
        }
    }

    private void showRoutineCompletedDialog() {
        new android.app.AlertDialog.Builder(RoutineMainActivity.this)
                .setTitle("Routine Completed")
                .setMessage("Congratulations! You've completed the entire routine.")
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    markRoutineAsCompleted(userId);

                    Intent intent = new Intent(RoutineMainActivity.this, HomeMainActivity.class);
                    startActivity(intent);
                    finish();
                }).show();
    }

    private void markRoutineAsCompletedInSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> inProgressRoutines = sharedPreferences.getStringSet("IN_PROGRESS_ROUTINES", new HashSet<>());
        inProgressRoutines.remove(String.valueOf(selectedRoutineID));
        editor.putStringSet("IN_PROGRESS_ROUTINES", inProgressRoutines);

        Set<String> completedRoutines = sharedPreferences.getStringSet("COMPLETED_ROUTINES", new HashSet<>());
        completedRoutines.add(String.valueOf(selectedRoutineID));
        editor.putStringSet("COMPLETED_ROUTINES", completedRoutines);

        editor.apply();
        updateRoutineListUI();
    }


    private void updateRoutineListUI() {
        if (workoutAdapter == null) {
            workoutAdapter = new RoutineWorkoutsAdapter(workouts, RoutineMainActivity.this, RoutineMainActivity.this::onWorkoutClicked);
            recyclerView.setAdapter(workoutAdapter);
        } else {
            workoutAdapter.updateWorkouts(workouts);  // Update adapter if the data changes
        }

        if (recyclerView.getAdapter() == null) {
            if (workoutAdapter != null) {
                Log.e("RoutineMainActivity", "RecyclerView adapter was missing. Attaching...");
                recyclerView.setAdapter(workoutAdapter);
            } else {
                Log.e("RoutineMainActivity", "Cannot attach adapter: workoutAdapter is null!");
            }
        }
    }

    private String formatTime(int totalSeconds) {
        return String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    public void setupBackButton(View view) {
        findViewById(R.id.routineBackButton).setOnClickListener(v -> {

            SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("LAST_SELECTED_ROUTINE", selectedRoutineID);
            editor.apply();

            Intent updateIntent = new Intent("UPDATE_IN_PROGRESS");
            sendBroadcast(updateIntent);

            Intent intent = new Intent(RoutineMainActivity.this, HomeMainActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
            finish();
        });
    }

    private void saveWorkoutProgress() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save the current workout progress
        if (currentWorkout != null) {
            editor.putInt("lastWorkoutID", currentWorkout.getWorkoutId());
            editor.putInt("remainingSets", remainingSets);
            editor.putInt("remainingTime", remainingTime);
            editor.putBoolean("isPlaying", isPlaying);
        }
        editor.putInt("completedWorkoutsCount", completedWorkoutsCount);
        editor.apply();
        Log.d("RoutineMainActivity", "Workout progress saved.");
    }


    private void clearRoutinePreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("LAST_SELECTED_ROUTINE");  // Remove the last selected routine
        editor.remove("IN_PROGRESS_ROUTINES");  // Remove the in-progress routines
        editor.remove("COMPLETED_ROUTINES");  // Remove completed routines
        editor.apply();
        Log.d("RoutineMainActivity", "Cleared routine preferences.");
    }

}