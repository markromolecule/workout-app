package com.livado.workout.ui.main.workout.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.livado.workout.R;
import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.data.remote.response.ApiResponse;
import com.livado.workout.data.remote.response.RoutineResponse;
import com.livado.workout.ui.main.home.HomeMainActivity;
import com.livado.workout.ui.main.workout.RoutineMainActivity;
import com.livado.workout.ui.main.workout.adapter.InProgressAdapter;
import com.livado.workout.utils.RecyclerViewItemSpacing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InProgressActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ApiService apiService;
    private InProgressAdapter inProgressAdapter;
    private List<RoutineResponse> inProgressRoutines = new ArrayList<>();
    private Set<String> fetchedRoutineIDs = new HashSet<>();
    private int userId;
    private LocalBroadcastManager localBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_in_progress);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("USER_ID", -1);

        if (userId == -1) {
            Log.e("InProgressActivity", "Error: userId not found!");
            Toast.makeText(this, "User not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeComponents();
        setupRecyclerView();
        fetchInProgressRoutinesFromServer(); // Always fetch from API first
        registerBroadcastReceiver();
        setupBackButton();
    }

    private void initializeComponents() {
        recyclerView = findViewById(R.id.inProgressRoutineContainer);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecyclerViewItemSpacing(24));
        apiService = RetrofitClient.getClient().create(ApiService.class);
    }

    private void setupRecyclerView() {
        inProgressAdapter = new InProgressAdapter(inProgressRoutines, userId, this, fetchedRoutineIDs);
        recyclerView.setAdapter(inProgressAdapter);
    }

    private void fetchInProgressRoutinesFromServer() {
        apiService.getInProgressRoutines(userId).enqueue(new Callback<ApiResponse<List<RoutineResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<RoutineResponse>>> call, @NonNull Response<ApiResponse<List<RoutineResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    inProgressRoutines.clear();
                    fetchedRoutineIDs.clear();

                    // Add full routine details (not just routine IDs)
                    for (RoutineResponse routine : response.body().getData()) {
                        inProgressRoutines.add(routine);
                        fetchedRoutineIDs.add(String.valueOf(routine.getRoutineID()));
                    }

                    Log.d("InProgressActivity", "Fetched In-Progress Routines: " + inProgressRoutines.size());

                    saveInProgressRoutinesToSharedPreferences();
                    inProgressAdapter.notifyDataSetChanged(); // Ensure UI is updated
                } else {
                    Log.e("InProgressActivity", "API Error: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<RoutineResponse>>> call, @NonNull Throwable t) {
                Log.e("InProgressActivity", "API request failed", t);
            }
        });
    }

    private void saveInProgressRoutinesToSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> routineIDs = new HashSet<>();
        for (RoutineResponse routine : inProgressRoutines) {
            routineIDs.add(String.valueOf(routine.getRoutineID()));
        }

        editor.putStringSet("IN_PROGRESS_ROUTINES", routineIDs);
        editor.apply();
        Log.d("InProgressActivity", "Saved In-Progress Routines to SharedPreferences.");
    }

    private void clearInProgressRoutines() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("IN_PROGRESS_ROUTINES");
        editor.apply();
        Log.d("InProgressActivity", "Cleared in-progress routines from SharedPreferences.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchInProgressRoutinesFromServer();
    }

    private void registerBroadcastReceiver() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter("UPDATE_IN_PROGRESS");
        localBroadcastManager.registerReceiver(updateReceiver, filter);
    }

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.hasExtra("selectedRoutineIDs")) {
                List<String> routineIDs = intent.getStringArrayListExtra("selectedRoutineIDs");
                if (routineIDs != null) {
                    saveSelectedRoutines(routineIDs);
                    fetchInProgressRoutinesFromServer(); // Refresh UI after update
                }
            }
        }
    };

    private void saveSelectedRoutines(List<String> routineIDs) {
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("IN_PROGRESS_ROUTINES", new HashSet<>(routineIDs));
        editor.apply();
        Log.d("InProgressActivity", "Updated selected routines in SharedPreferences.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(updateReceiver);
    }

    private void setupBackButton() {
        findViewById(R.id.backButton).setOnClickListener(v -> {
            Intent updateIntent = new Intent("UPDATE_IN_PROGRESS");
            updateIntent.putStringArrayListExtra("selectedRoutineIDs", new ArrayList<>(fetchedRoutineIDs));
            sendBroadcast(updateIntent);

            Intent intent = new Intent(InProgressActivity.this, HomeMainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}