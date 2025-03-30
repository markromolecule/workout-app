package com.livado.workout.ui.main.workout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.livado.workout.R;
import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.data.remote.request.UpdateWorkoutRequest;
import com.livado.workout.data.remote.response.ApiResponse;
import com.livado.workout.data.remote.response.RoutineListResponse;
import com.livado.workout.data.remote.response.RoutineResponse;
import com.livado.workout.data.remote.response.WorkoutResponse;
import com.livado.workout.data.remote.response.WorkoutResponseWrapper;
import com.livado.workout.ui.main.home.HomeMainActivity;
import com.livado.workout.ui.main.workout.adapter.RoutineAdapter;
import com.livado.workout.utils.RecyclerViewItemSpacing;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoutineListActivity
        extends AppCompatActivity
        implements RoutineAdapter.RoutineClickListener {

    private RecyclerView recyclerView;
    private RoutineAdapter routineAdapter;
    private ApiService apiService;
    private Set<String> selectedRoutineIDs = new HashSet<>();
    private Set<String> inProgressRoutines = new HashSet<>();
    private static final int REQUEST_CONFIRM_ROUTINE = 101;
    private boolean isReceiverRegistered = false;

    private int userId;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_routine);

        userId = getIntent().getIntExtra("userId", -1);
        apiService = RetrofitClient.getClient().create(ApiService.class);

        initializeComponents();
        setupRecyclerView();
        fetchInProgressRoutines();
        registerRoutineUpdateReceiver();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isReceiverRegistered) {
            unregisterReceiver(updateReceiver);
            isReceiverRegistered = false;
        }

        clearRoutinePreferences();
        resetRoutineStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInProgressRoutines();  // Load saved in-progress routines
        // Removed automatic navigation logic
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerRoutineUpdateReceiver(); // Ensure receiver is registered only once
        fetchInProgressRoutines(); // Fetch actual data from the database
        fetchRoutines(); // Load all available routines
    }

    private void initializeComponents() {
        recyclerView = findViewById(R.id.workoutContainerRoutine);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecyclerViewItemSpacing(24));
    }

    private void setupRecyclerView() {
        routineAdapter = new RoutineAdapter(new ArrayList<>(), userId, this, this, new HashSet<>(), new HashSet<>());
        recyclerView.setAdapter(routineAdapter);
    }

    private void fetchInProgressRoutines() {
        Log.d("RoutineListActivity", "Fetching in-progress routines for userId: " + userId);

        apiService.getInProgressRoutines(userId).enqueue(new Callback<ApiResponse<List<RoutineResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<RoutineResponse>>> call, @NonNull Response<ApiResponse<List<RoutineResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    inProgressRoutines.clear(); // Ensure previous data is removed

                    // ✅ Convert RoutineResponse objects to String IDs and store them
                    for (RoutineResponse routine : response.body().getData()) {
                        inProgressRoutines.add(String.valueOf(routine.getRoutineID())); // Extract ID as String
                    }

                    Log.d("RoutineListActivity", "Fetched In-Progress Routines: " + inProgressRoutines.size());

                    saveInProgressRoutinesToSharedPreferences(); // Save routine IDs to SharedPreferences
                    updateRoutineListUI();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<RoutineResponse>>> call, @NonNull Throwable t) {
                Log.e("RoutineListActivity", "Error fetching in-progress routines", t);
            }
        });
    }

    private void saveInProgressRoutinesToSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putStringSet("IN_PROGRESS_ROUTINES", inProgressRoutines);
        editor.apply();
        Log.d("RoutineListActivity", "In-progress routines saved to SharedPreferences.");
    }

    private void updateRoutineListUI() {
        if (routineAdapter != null) {
            routineAdapter.updateInProgressRoutines(inProgressRoutines);
            routineAdapter.notifyDataSetChanged();
        }
    }

    private void loadInProgressRoutines() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        inProgressRoutines = sharedPreferences.getStringSet("IN_PROGRESS_ROUTINES", new HashSet<>());
        Log.d("RoutineListActivity", "In-Progress Routines: " + inProgressRoutines);
    }

    private int getLastSelectedRoutine() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        return sharedPreferences.getInt("LAST_SELECTED_ROUTINE", -1);
    }

    private void saveLastSelectedRoutine(int routineID) {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("LAST_SELECTED_ROUTINE", routineID);
        editor.apply();
    }
    private void clearRoutinePreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("LAST_SELECTED_ROUTINES");
        editor.remove("IN_PROGRESS_ROUTINES");
        editor.apply();
        Log.d("RoutineListActivity", "Cleared routine preferences on destroy.");
    }

    private void resetRoutineStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("IN_PROGRESS_ROUTINES", new HashSet<>());
        editor.apply();
        Log.d("RoutineListActivity", "Reset routine in-progress status.");
    }

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fetchRoutines();
        }
    };

    private void registerRoutineUpdateReceiver() {
        if (!isReceiverRegistered) {
            // Use simplified IntentFilter with the action "UPDATE_ROUTINE_LIST"
            IntentFilter filter = new IntentFilter("UPDATE_ROUTINE_LIST");
            // Register the receiver with the RECEIVER_EXPORTED flag to allow other apps to receive it
            registerReceiver(updateReceiver, filter, Context.RECEIVER_EXPORTED);
            isReceiverRegistered = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CONFIRM_ROUTINE && resultCode == RESULT_OK) {
            int confirmedRoutineID = data.getIntExtra("confirmedRoutineID", -1);

            if (confirmedRoutineID != -1) {
                Log.d("RoutineListActivity", "Routine confirmed: " + confirmedRoutineID);

                SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                Set<String> selectedRoutines = new HashSet<>(sharedPreferences.getStringSet("LAST_SELECTED_ROUTINES", new HashSet<>()));

                selectedRoutines.add(String.valueOf(confirmedRoutineID));
                editor.putStringSet("LAST_SELECTED_ROUTINES", selectedRoutines);
                editor.apply();

                // Refresh UI properly
                Intent refreshIntent = new Intent(this, RoutineListActivity.class);
                refreshIntent.putExtra("userId", userId);
                startActivity(refreshIntent);
                finish();
            }
        }
    }

    public void setupConfirmButton(View view) {
        findViewById(R.id.confirmButton).setOnClickListener(v -> handleConfirmButtonClick());
    }

    private void handleConfirmButtonClick() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Fetch the set of selected routine IDs
        Set<String> selectedRoutineIDs = sharedPreferences.getStringSet("LAST_SELECTED_ROUTINES", new HashSet<>());

        if (selectedRoutineIDs.isEmpty()) {
            return;
        }

        // Add selected routines to in-progress routines set
        for (String routineID : selectedRoutineIDs) {
            if (!inProgressRoutines.contains(routineID)) {
                inProgressRoutines.add(routineID);  // Add the routine to in-progress routines
                Log.d("RoutineListActivity", "Routine " + routineID + " added to in-progress.");
            }
        }

        // Save the updated in-progress routines to SharedPreferences
        editor.putStringSet("IN_PROGRESS_ROUTINES", inProgressRoutines);
        editor.apply();
        Log.d("RoutineListActivity", "Updated in-progress routines saved to SharedPreferences");

        // Declare a variable to store the last selected routine ID
        int lastSelectedRoutineID = -1;

        // Now iterate over selected routines and process each one
        for (String routineID : selectedRoutineIDs) {
            int selectedRoutineID = Integer.parseInt(routineID);

            // Store selected routine in SharedPreferences
            storeSelectedRoutinesInSharedPreferences(sharedPreferences, selectedRoutineID);

            // Update routine status in the database
            completeRoutineInDatabase(selectedRoutineID);

            // Save last selected routine to SharedPreferences
            saveLastSelectedRoutine(selectedRoutineID);

            // Disable routine selection to avoid re-selection
            disableRoutineSelection(selectedRoutineID);

            // Start routine progress (this will reflect the 'in-progress' status)
            startRoutineProgress(selectedRoutineID);

            // Set the last selected routine ID (this will be passed when navigating)
        }

        // After processing all selected routines, navigate to the RoutineMainActivity with the last selected routine ID
        navigateToRoutineMain();
    }

    private void storeSelectedRoutinesInSharedPreferences(SharedPreferences sharedPreferences, int routineID) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> selectedRoutines = new HashSet<>(sharedPreferences.getStringSet("LAST_SELECTED_ROUTINES", new HashSet<>()));
        selectedRoutines.add(String.valueOf(routineID));
        editor.putStringSet("LAST_SELECTED_ROUTINES", selectedRoutines);
        editor.apply();

        Log.d("RoutineListActivity", "Stored selected routine " + routineID + " in SharedPreferences");
    }

    private void completeRoutineInDatabase(int selectedRoutineID) {
        // Create the JSON data for the request body
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", userId);  // userId
            jsonObject.put("routine_id", selectedRoutineID);  // routineId
            jsonObject.put("status", "completed");  // status to completed

            // Create the request body
            RequestBody requestBody = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json; charset=utf-8"));

            // Call the backend to mark the routine as completed
            apiService.completeRoutineProgress(requestBody).enqueue(new Callback<ApiResponse>() { // Change to ApiResponse (no Void)
                @Override
                public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse responseBody = response.body();
                        if (responseBody.isSuccess()) {
                            Log.d("RoutineListActivity", "Routine marked as completed in database.");
                            Toast.makeText(RoutineListActivity.this, "Routine completed!", Toast.LENGTH_SHORT).show();
                            sendRoutineUpdateBroadcast(selectedRoutineID);  // Broadcast completion to other components
                        } else {
                            Log.e("RoutineListActivity", "Failed to complete routine in database: " + responseBody.getMessage());
                            Toast.makeText(RoutineListActivity.this, "Failed to complete routine", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                    Log.e("RoutineListActivity", "Error completing routine in database", t);
                    Toast.makeText(RoutineListActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (JSONException e) {
            Log.e("RoutineListActivity", "Error creating JSON for request body", e);
        }
    }

    private void disableRoutineSelection(int routineID) {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> inProgressRoutines = new HashSet<>(sharedPreferences.getStringSet("IN_PROGRESS_ROUTINES", new HashSet<>()));
        inProgressRoutines.add(String.valueOf(routineID));
        editor.putStringSet("IN_PROGRESS_ROUTINES", inProgressRoutines);
        editor.apply();

        fetchRoutines(); // Refresh UI
    }

    private Integer getSelectedRoutineID(SharedPreferences sharedPreferences) {
        Set<String> selectedRoutineIDs = sharedPreferences.getStringSet("LAST_SELECTED_ROUTINES", new HashSet<>());

        if (selectedRoutineIDs.isEmpty()) {
            Toast.makeText(this, "No routine selected!", Toast.LENGTH_SHORT).show();
            return null;
        }

        try {
            return Integer.parseInt(new ArrayList<>(selectedRoutineIDs).get(0));
        } catch (NumberFormatException e) {
            Log.e("RoutineListActivity", "Invalid routine ID format", e);
            return null;
        }
    }

    private void startRoutineProgress(int selectedRoutineID) {
        Log.d("RoutineListActivity", "Starting routine progress for routineID: " + selectedRoutineID);

        apiService.startRoutineProgress(userId, selectedRoutineID).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                int statusCode = response.code();
                Log.d("RoutineListActivity", "HTTP Status: " + statusCode);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> responseBody = response.body();
                    Log.d("RoutineListActivity", "API Success: " + responseBody.isSuccess());
                    Log.d("RoutineListActivity", "API Message: " + responseBody.getMessage());

                    if (responseBody.isSuccess()) {
                        Log.d("RoutineListActivity", "Routine started successfully");
                        Toast.makeText(RoutineListActivity.this, "Routine started!", Toast.LENGTH_SHORT).show();

                        updateWorkoutInRoutine(selectedRoutineID);
                        clearSelectedRoutines();
                        fetchRoutines();
                        sendRoutineUpdateBroadcast(selectedRoutineID);
                        navigateToRoutineMain();
                    } else {
                        Log.e("RoutineListActivity", "API reported failure: " + responseBody.getMessage());
                        Toast.makeText(RoutineListActivity.this, "Failed to start routine: " + responseBody.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "No error body available";
                        Log.e("RoutineListActivity", "API Failure: " + errorMsg);
                        Toast.makeText(RoutineListActivity.this, "Failed to start routine: " + response.message(), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Log.e("RoutineListActivity", "Error reading response body", e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Log.e("RoutineListActivity", "API error: ", t);
                Toast.makeText(RoutineListActivity.this, "Network error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendRoutineUpdateBroadcast(int selectedRoutineID) {
        Intent broadcastIntent = new Intent("UPDATE_IN_PROGRESS");
        broadcastIntent.putStringArrayListExtra("selectedRoutineIDs", new ArrayList<>(Collections.singletonList(String.valueOf(selectedRoutineID))));
        sendBroadcast(broadcastIntent);
    }

    private void navigateToRoutineMain() {
        int lastSelectedRoutineID = getLastSelectedRoutine();
        if (lastSelectedRoutineID != -1) {
            Intent intent = new Intent(RoutineListActivity.this, RoutineMainActivity.class);
            intent.putExtra("selectedRoutineID", lastSelectedRoutineID);  // Pass the last selected routine ID
            intent.putExtra("userId", userId);  // Pass the user ID
            startActivity(intent);
        } else {
            Toast.makeText(this, "No routine selected", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Clear the selected routines in SharedPreferences for a fresh start.
     */
    private void clearSelectedRoutines() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        sharedPreferences.edit().remove("LAST_SELECTED_ROUTINES").apply();
        Log.d("RoutineListActivity", "Cleared selected routines for a fresh start.");
    }

    private void fetchRoutines() {
        apiService.getRoutines(userId).enqueue(new Callback<RoutineListResponse>() {
            @Override
            public void onResponse(Call<RoutineListResponse> call, Response<RoutineListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    List<RoutineResponse> routines = response.body().data.routines;

                    if (routines.isEmpty()) {
                        Toast.makeText(RoutineListActivity.this, "No routines found", Toast.LENGTH_SHORT).show();
                    }

                    SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
                    Set<String> inProgressRoutines = sharedPreferences.getStringSet("IN_PROGRESS_ROUTINES", new HashSet<>());
                    Set<String> selectedRoutines = sharedPreferences.getStringSet("LAST_SELECTED_ROUTINES", new HashSet<>());

                    // Pass the selectedRoutines set to the adapter
                    routineAdapter = new RoutineAdapter(routines, userId, RoutineListActivity.this, RoutineListActivity.this, inProgressRoutines, selectedRoutines);
                    recyclerView.setAdapter(routineAdapter);
                } else {
                    Log.e("RoutineListActivity", "Failed to fetch routines: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<RoutineListResponse> call, Throwable t) {
                Log.e("RoutineListActivity", "Error fetching routines", t);
                Toast.makeText(RoutineListActivity.this, "Failed to load routines", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRoutineClick(int routineID) {
        Log.d("RoutineListActivity", "Routine Clicked - ID: " + routineID);

        // Retrieve SharedPreferences to store the selected routines
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> selectedRoutines = new HashSet<>(sharedPreferences.getStringSet("LAST_SELECTED_ROUTINES", new HashSet<>()));

        if (selectedRoutines.contains(String.valueOf(routineID))) {
            // Deselect the routine
            selectedRoutines.remove(String.valueOf(routineID));
            Log.d("RoutineListActivity", "Routine deselected: " + routineID);
        } else {
            // Select the routine
            selectedRoutines.add(String.valueOf(routineID));
            Log.d("RoutineListActivity", "Routine selected: " + routineID);
        }

        // Save the updated set of selected routines
        editor.putStringSet("LAST_SELECTED_ROUTINES", selectedRoutines);
        editor.apply();

        // Update the adapter to reflect changes
        routineAdapter.updateSelectedRoutines(selectedRoutines);

        // If routine was deselected, don't open WorkoutListActivity
        if (!selectedRoutines.contains(String.valueOf(routineID))) {
            return; // Routine was deselected, so no need to open workout list
        }

        // If routine was selected, proceed to open WorkoutListActivity
        Intent intent = new Intent(RoutineListActivity.this, WorkoutListActivity.class);
        intent.putExtra("userId", userId);  // Pass the userId
        intent.putExtra("routineID", routineID);  // Pass the selected routine ID

        // Start WorkoutListActivity
        startActivity(intent);
    }

    private void updateWorkoutInRoutine(int routineID) {
        // Send the updated workout data to the backend
        // Create a list of updated workouts
        List<WorkoutResponse> updatedWorkouts = getUpdatedWorkouts();  // Assume this function returns the updated workouts list

        for (WorkoutResponse workout : updatedWorkouts) {
            UpdateWorkoutRequest request = new UpdateWorkoutRequest(userId, routineID, workout.getWorkoutId(), workout.getSets(), workout.getReps(), workout.getDuration());

            // Call the API to update each workout in the routine
            apiService.updateWorkout(request).enqueue(new Callback<WorkoutResponseWrapper>() {
                @Override
                public void onResponse(@NonNull Call<WorkoutResponseWrapper> call, @NonNull Response<WorkoutResponseWrapper> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(RoutineListActivity.this, "Workout updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RoutineListActivity.this, "Failed to update workout", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<WorkoutResponseWrapper> call, @NonNull Throwable t) {
                    Toast.makeText(RoutineListActivity.this, "Error updating workout", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private List<WorkoutResponse> getUpdatedWorkouts() {
        return new ArrayList<>();
    }

    public void setupBackButton(View view) {
        findViewById(R.id.backButton).setOnClickListener(v -> {
            Intent intent = new Intent(RoutineListActivity.this, HomeMainActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
            finish();
        });
    }
}