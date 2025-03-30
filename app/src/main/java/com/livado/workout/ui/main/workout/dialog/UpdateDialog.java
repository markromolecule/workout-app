package com.livado.workout.ui.main.workout.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.livado.workout.R;
import com.livado.workout.data.remote.response.ApiResponse;
import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.data.remote.response.WorkoutResponse;
import com.livado.workout.data.remote.response.WorkoutResponseWrapper;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateDialog {

    private ApiService apiService;
    private TextView tvWorkoutName, tvDescription, tvDifficulty, tvEquipment;
    private EditText etSets, etReps, etDuration;
    private Button btnConfirm, btnCancel;
    private int workoutId, routineId, userId;
    private OnWorkoutUpdatedListener updateListener;
    private WorkoutResponse updatedWorkout;
    private Context context;

    public interface OnWorkoutUpdatedListener {
        void onWorkoutUpdated(WorkoutResponse updatedWorkout);
    }

    public UpdateDialog(Context context, int userId, int routineId, int workoutId, OnWorkoutUpdatedListener listener) {
        this.context = context;
        this.userId = userId;
        this.routineId = routineId;
        this.workoutId = workoutId;
        this.updateListener = listener;
        apiService = RetrofitClient.getClient().create(ApiService.class);
        Log.d("UpdateDialog", "Initialized with userId: " + userId + ", routineId: " + routineId + ", workoutId: " + workoutId);

    }

    public void show() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_workout_input, null);
        tvWorkoutName = view.findViewById(R.id.workoutName);
        tvDescription = view.findViewById(R.id.workoutDescription);
        tvDifficulty = view.findViewById(R.id.workoutDifficulty);
        tvEquipment = view.findViewById(R.id.workoutEquipment);
        etSets = view.findViewById(R.id.inputSets);
        etReps = view.findViewById(R.id.inputReps);
        etDuration = view.findViewById(R.id.inputDuration);
        btnConfirm = view.findViewById(R.id.continueButton);
        btnCancel = view.findViewById(R.id.cancelButton);

        updatedWorkout = null;

        // Fetch workout details to display in the dialog
        fetchWorkoutDetails();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            try {
                int sets = Integer.parseInt(etSets.getText().toString().trim());
                int reps = Integer.parseInt(etReps.getText().toString().trim());
                int duration = Integer.parseInt(etDuration.getText().toString().trim());

                if (updatedWorkout != null) {
                    updatedWorkout.setSets(sets);
                    updatedWorkout.setReps(reps);
                    updatedWorkout.setDuration(duration);

                    // Store workout changes in SharedPreferences
                    saveWorkoutToPreferences(updatedWorkout);

                    if (updateListener != null) {
                        updateListener.onWorkoutUpdated(updatedWorkout);
                    }

                    Log.d("UpdateDialog", "Workout updated successfully!");
                    Toast.makeText(context, "Workout updated successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("UpdateDialog", "Error: No workout details loaded.");
                    Toast.makeText(context, "Failed to update workout details!", Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();  // Dismiss the dialog

            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid input values", Toast.LENGTH_SHORT).show();
                Log.e("UpdateDialog", "Error parsing input values: ", e);
            }
        });
    }

    private void fetchWorkoutDetails() {
        Log.d("UpdateDialog", "Fetching workout details for workoutId: " + workoutId + ", userId: " + userId + ", routineId: " + routineId);

        apiService.getWorkoutByID(workoutId, userId, routineId).enqueue(new Callback<ApiResponse<WorkoutResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<WorkoutResponse>> call, Response<ApiResponse<WorkoutResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    handleErrorResponse(response);
                    return;
                }

                WorkoutResponse workout = response.body().getData();
                if (response.body().isSuccess() && workout != null) {
                    updateUIWithWorkoutDetails(workout);
                } else {
                    Log.e("UpdateDialog", "API Response Failed: " + response.body().getMessage());
                    showToast("No workout details found");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<WorkoutResponse>> call, Throwable t) {
                Log.e("UpdateDialog", "Network Error: Could not fetch workout details", t);
                showToast("Network Error: Failed to load workouts");
            }
        });
    }

    private void updateUIWithWorkoutDetails(WorkoutResponse workout) {
        updatedWorkout = workout;
        tvWorkoutName.setText(workout.getWorkoutName());
        tvDescription.setText(workout.getWorkoutDescription());
        tvDifficulty.setText(workout.getWorkoutDifficulty());
        tvEquipment.setText(workout.getWorkoutEquipment());
        etSets.setText(String.valueOf(workout.getSets()));
        etReps.setText(String.valueOf(workout.getReps()));
        etDuration.setText(String.valueOf(workout.getDuration()));

        Log.d("UpdateDialog", "Workout details updated successfully");
    }

    private void handleErrorResponse(Response<ApiResponse<WorkoutResponse>> response) {
        try {
            String rawResponse = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            Log.e("UpdateDialog", "Failed to fetch workout details: " + rawResponse);
        } catch (IOException e) {
            Log.e("UpdateDialog", "Error parsing error body", e);
        }
        showToast("Failed to load workout details");
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private void saveWorkoutToPreferences(WorkoutResponse workout) {
        SharedPreferences prefs = context.getSharedPreferences("WorkoutUpdates_" + userId, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt("sets_" + workout.getWorkoutId(), workout.getSets());
        editor.putInt("reps_" + workout.getWorkoutId(), workout.getReps());
        editor.putInt("duration_" + workout.getWorkoutId(), workout.getDuration());

        editor.apply(); // Save changes asynchronously
    }
}