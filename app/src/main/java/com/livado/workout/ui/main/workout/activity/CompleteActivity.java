package com.livado.workout.ui.main.workout.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.livado.workout.R;
import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.data.remote.response.ApiResponse;
import com.livado.workout.data.remote.response.RoutineResponse;
import com.livado.workout.ui.main.home.HomeMainActivity;
import com.livado.workout.ui.main.workout.adapter.CompleteAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompleteActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ApiService apiService;
    private List<RoutineResponse> completedRoutines = new ArrayList<>();
    private CompleteAdapter completeAdapter;
    private int userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_complete);
        setupSystemBars();

        recyclerView = findViewById(R.id.completeRoutineContainer);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Retrieve userID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userID = sharedPreferences.getInt("USER_ID", -1);

        if (userID == -1) {
            Toast.makeText(this, "User ID not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = RetrofitClient.getClient().create(ApiService.class);
        fetchCompletedRoutines();
    }

    private void fetchCompletedRoutines() {
        Log.d("CompleteActivity", "Fetching completed routines for userId: " + userID);

        apiService.getCompletedRoutines(userID).enqueue(new Callback<ApiResponse<List<RoutineResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<RoutineResponse>>> call,
                                   @NonNull Response<ApiResponse<List<RoutineResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {

                    List<RoutineResponse> fetchedRoutines = response.body().getData();
                    completedRoutines.clear();

                    if (fetchedRoutines.isEmpty()) {
                        Log.d("CompleteActivity", "No completed routines found.");
                        clearCompletedRoutinesFromSharedPreferences();
                        Toast.makeText(CompleteActivity.this, "No completed routines available.", Toast.LENGTH_SHORT).show();
                    } else {
                        completedRoutines.addAll(fetchedRoutines);
                        Log.d("CompleteActivity", "Fetched Completed Routines: " + completedRoutines.size());
                    }
                    updateRoutineListUI();
                } else {
                    Log.e("CompleteActivity", "Error fetching completed routines: " + response.message());
                    Toast.makeText(CompleteActivity.this, "Failed to load completed routines.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<RoutineResponse>>> call, @NonNull Throwable t) {
                Log.e("CompleteActivity", "Error fetching completed routines", t);
                Toast.makeText(CompleteActivity.this, "Failed to fetch completed routines.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRoutineListUI() {
        if (completedRoutines.isEmpty()) {
            Log.d("CompleteActivity", "Clearing adapter - No completed routines.");
            if (completeAdapter != null) {
                completeAdapter.clearData();
            }
            return;
        }

        if (completeAdapter == null) {
            completeAdapter = new CompleteAdapter(completedRoutines, userID, CompleteActivity.this);
            recyclerView.setAdapter(completeAdapter);
        } else {
            completeAdapter.updateData(completedRoutines);
        }
    }

    public void setupBackButton(View view) {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), HomeMainActivity.class));
            finish();
        });
    }

    private void setupSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("CompleteActivity", "onStart called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("CompleteActivity", "onResume called");
        fetchCompletedRoutines();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("CompleteActivity", "onPause called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("CompleteActivity", "onStop called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("CompleteActivity", "onDestroy called");
        clearCompletedRoutinesFromSharedPreferences();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("CompleteActivity", "onRestart called");
    }

    // Method to clear completed routines from SharedPreferences when the database is truncated
    private void clearCompletedRoutinesFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("COMPLETED_ROUTINES");
        editor.remove("IN_PROGRESS_ROUTINES");
        editor.apply();
        Log.d("CompleteActivity", "Cleared completed and in-progress routines from SharedPreferences.");
    }

}