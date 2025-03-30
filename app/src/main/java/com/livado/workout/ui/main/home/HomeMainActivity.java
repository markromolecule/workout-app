package com.livado.workout.ui.main.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.livado.workout.R;
import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.data.remote.response.ApiResponse;
import com.livado.workout.data.remote.response.RoutineResponse;
import com.livado.workout.ui.main.profile.ProfileActivity;
import com.livado.workout.ui.main.timer.TimerActivity;
import com.livado.workout.ui.main.workout.RoutineListActivity;
import com.livado.workout.ui.main.workout.RoutineMainActivity;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeMainActivity extends AppCompatActivity {
    private static final int PROFILE_UPDATE_REQUEST = 100;
    private BottomNavigationView bottomNavigationView;

    private ImageButton profileButton;
    private SharedPreferences sharedPreferences;
    private int userId;
    private int selectedRoutineID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(this::navigateToActivity);
        setupSystemBars();

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("USER_ID", -1);

        if (userId == -1) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.putInt("USER_ID", -1);
            editor.putString("USERNAME", "Guest");
            editor.apply();
        }

        profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeMainActivity.this, ProfileActivity.class);
            startActivityForResult(intent, PROFILE_UPDATE_REQUEST);
        });

        checkInProgressRoutines();

        if (savedInstanceState == null) {
            String username = sharedPreferences.getString("USERNAME", "Guest");

            HomeFragment homeFragment = new HomeFragment();
            Bundle args = new Bundle();
            args.putString("USERNAME", username);
            homeFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainerView, homeFragment).commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("HomeMainActivity", "onStart() called");
        // Add any actions to be taken when the activity becomes visible
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if a routine is in progress and save selected routine ID
        SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
        selectedRoutineID = sharedPreferences.getInt("LAST_SELECTED_ROUTINE", -1);

        if (selectedRoutineID != -1) {
            Log.d("HomeMainActivity", "Last selected routine ID: " + selectedRoutineID);
        }
        checkInProgressRoutines();
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d("HomeMainActivity", "onPause() called");
        // Add any actions to be taken when the activity is no longer visible
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("HomeMainActivity", "onStop() called");
        // Add any actions to be taken when the activity is no longer visible
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("HomeMainActivity", "onDestroy() called");
        // Release resources here if necessary
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PROFILE_UPDATE_REQUEST && resultCode == RESULT_OK) {
            refreshUserProfile();
        }
    }

    private void refreshUserProfile() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String updatedUsername = sharedPreferences.getString("USERNAME", "Guest");

        TextView usernameTextView = findViewById(R.id.homeUsernameText);
        usernameTextView.setText(updatedUsername);
    }

    private boolean navigateToActivity(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        Intent intent = null;

        if (itemId == R.id.nav_home) {
            intent = new Intent(this, HomeMainActivity.class);
        } else if (itemId == R.id.nav_workout) {
            // Retrieve user data and check for in-progress routines
            SharedPreferences sharedPreferences = getSharedPreferences("RoutinePrefs_" + userId, MODE_PRIVATE);
            Set<String> inProgressRoutines = sharedPreferences.getStringSet("IN_PROGRESS_ROUTINES", new HashSet<>());

            int selectedRoutineID = sharedPreferences.getInt("LAST_SELECTED_ROUTINE", -1);

            // Log the selected routine ID and in-progress routines for debugging
            Log.d("HomeMainActivity", "In-Progress Routines: " + inProgressRoutines);
            Log.d("HomeMainActivity", "Last Selected Routine ID: " + selectedRoutineID);

            if (!inProgressRoutines.isEmpty()) {
                // If there are in-progress routines, navigate to RoutineMainActivity with the selected routine
                intent = new Intent(this, RoutineMainActivity.class);
                intent.putExtra("userId", userId);
                if (selectedRoutineID != -1) {
                    intent.putExtra("selectedRoutineID", selectedRoutineID);
                }
            } else {
                // If no routine is in progress, show the RoutineListActivity
                intent = new Intent(this, RoutineListActivity.class);
                intent.putExtra("userId", userId);
            }
        } else if (itemId == R.id.nav_timer) {
            intent = new Intent(this, TimerActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
            overridePendingTransition(0, 0);  // Optional: If you want no transition animation
            return true;
        }

        return false;
    }

    private void checkInProgressRoutines() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        // Fetch full routine details (not just IDs)
        apiService.getInProgressRoutines(userId).enqueue(new Callback<ApiResponse<List<RoutineResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<RoutineResponse>>> call, @NonNull Response<ApiResponse<List<RoutineResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Set<String> inProgressRoutines = new HashSet<>();

                    // Iterate through the full routine data (not just IDs) and add the IDs to the set
                    for (RoutineResponse routine : response.body().getData()) {
                        inProgressRoutines.add(String.valueOf(routine.getRoutineID()));  // Assuming RoutineResponse has a method getRoutineID
                    }

                    // Save the updated in-progress routines to SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putStringSet("IN_PROGRESS_ROUTINES", inProgressRoutines);
                    editor.apply();

                    Log.d("HomeMainActivity", "Updated in-progress routines: " + inProgressRoutines);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<RoutineResponse>>> call, @NonNull Throwable t) {
                Log.e("HomeMainActivity", "Error fetching in-progress routines", t);
            }
        });
    }

    private void setupSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.parent_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}