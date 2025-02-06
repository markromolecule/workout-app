package com.livado.workout.ui.main.workout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.livado.workout.R;
import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.data.remote.response.WorkoutResponse;
import com.livado.workout.ui.main.workout.adapter.WorkoutAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkoutActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WorkoutAdapter workoutAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout);

        recyclerView = findViewById(R.id.workoutRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadWorkouts();
    }

    private void loadWorkouts() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        apiService.getWorkouts().enqueue(new Callback<List<WorkoutResponse>>() {
            @Override
            public void onResponse(Call<List<WorkoutResponse>> call, Response<List<WorkoutResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<WorkoutResponse> workouts = response.body();
                    workoutAdapter = new WorkoutAdapter(WorkoutActivity.this, workouts);
                    recyclerView.setAdapter(workoutAdapter);
                } else {
                    Toast.makeText(WorkoutActivity.this, "Failed to load workouts", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<WorkoutResponse>> call, Throwable t) {
                Log.e("WorkoutActivity", "Error: " + t.getMessage());
                Toast.makeText(WorkoutActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}