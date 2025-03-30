package com.livado.workout.ui.main.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.livado.workout.R;
import com.livado.workout.data.local.entities.UserData;
import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.data.remote.response.UserProfileResponse;
import com.livado.workout.ui.main.workout.RoutineListActivity;
import com.livado.workout.ui.main.workout.RoutineMainActivity;
import com.livado.workout.ui.main.workout.activity.CompleteActivity;
import com.livado.workout.ui.main.workout.activity.InProgressActivity;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class HomeFragment extends Fragment {
    private TextView homeUsernameText, heightNum, weightNum;
    private ImageButton workoutButton;
    private Button completeButton, inProgressButton;
    private SharedPreferences sharedPreferences;
    private ApiService apiService;
    private int userId;
    private int selectedRoutineID = -1;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("HomeFragment", "Received UPDATE_IN_PROGRESS - Refreshing in-progress routines");
            fetchInProgressRoutines();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        apiService = RetrofitClient.getClient().create(ApiService.class);
        userId = sharedPreferences.getInt("USER_ID", -1);
        Log.d("HomeFragment", "Fetched userId in onCreate: " + userId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initViews(view);
        setupListeners();

        SharedPreferences routinePrefs = requireContext().getSharedPreferences("RoutinePrefs_" + userId, Context.MODE_PRIVATE);
        selectedRoutineID = routinePrefs.getInt("LAST_SELECTED_ROUTINE", -1);

        if (userId == -1) {
            resetToGuest();
        } else {
            fetchUserProfile();
        }

        requireActivity().registerReceiver(updateReceiver, new IntentFilter("UPDATE_IN_PROGRESS"), Context.RECEIVER_NOT_EXPORTED);

        return view;
    }

    private void fetchUserProfile() {
        apiService.getUserProfile(userId).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUserProfile(response.body().getData());
                } else {
                    Log.e("HomeFragment", "Failed to fetch user profile: " + response.message());
                    updateUIFromPreferences();  // Update UI with preferences if API fails
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                Log.e("HomeFragment", "Error fetching user profile", t);
                updateUIFromPreferences();  // Update UI with preferences if there is an error
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        fetchInProgressRoutines();
        fetchCompletedRoutines();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUIFromPreferences();
        fetchInProgressRoutines();
        fetchCompletedRoutines();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("HomeFragment", "Fragment Paused - Saving any temporary states if needed.");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("HomeFragment", "Fragment Stopped - Cleanup actions can be performed here.");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().unregisterReceiver(updateReceiver);
        Log.d("HomeFragment", "Fragment View Destroyed - Receiver Unregistered.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("HomeFragment", "Fragment Destroyed - Final cleanup executed.");
    }

    private void initViews(View view) {
        homeUsernameText = view.findViewById(R.id.homeUsernameText);
        heightNum = view.findViewById(R.id.heightNum);
        weightNum = view.findViewById(R.id.weightNum);
        workoutButton = view.findViewById(R.id.workoutButton);
        completeButton = view.findViewById(R.id.completeButton);
        inProgressButton = view.findViewById(R.id.inProgressButton);
    }

    private void setupListeners() {
        workoutButton.setOnClickListener(v -> {
            if (userId == -1) {
                showToast("Please log in first!");
                return;
            }

            // Store selected routine ID in SharedPreferences
            SharedPreferences routinePrefs = requireContext().getSharedPreferences("RoutinePrefs_" + userId, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = routinePrefs.edit();
            editor.putInt("LAST_SELECTED_ROUTINE", selectedRoutineID); // Store the selected routine ID
            editor.apply(); // Persist the data

            Log.d("HomeFragment", "Workout button clicked");
            Log.d("HomeFragment", "Last Selected Routine ID: " + selectedRoutineID);

            Intent intent = new Intent(getActivity(), RoutineListActivity.class);
            intent.putExtra("userId", userId);
            if (selectedRoutineID != -1) {
                Log.d("HomeFragment", "Passing selectedRoutineID: " + selectedRoutineID);
                intent.putExtra("selectedRoutineID", selectedRoutineID);
            }
            startActivity(intent);
        });

        completeButton.setOnClickListener(v -> {
            // Navigate to the CompleteActivity
            startActivity(new Intent(getActivity(), CompleteActivity.class));
        });

        inProgressButton.setOnClickListener(v -> {
            // Fetch in-progress routines from SharedPreferences
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("RoutinePrefs_" + userId, Context.MODE_PRIVATE);
            Set<String> selectedRoutineIDs = sharedPreferences.getStringSet("IN_PROGRESS_ROUTINES", new HashSet<>());

            if (selectedRoutineIDs.isEmpty()) {
                showToast("No routines in progress!");
                return;
            }

            Intent inProgressIntent = new Intent(getActivity(), InProgressActivity.class);
            inProgressIntent.putStringArrayListExtra("selectedRoutineIDs", new ArrayList<>(selectedRoutineIDs));
            startActivity(inProgressIntent);
        });
    }

    private void fetchInProgressRoutines() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("RoutinePrefs_" + userId, Context.MODE_PRIVATE);
        Set<String> selectedRoutineIDs = sharedPreferences.getStringSet("IN_PROGRESS_ROUTINES", new HashSet<>());

        // Get the count of in-progress routines
        int count = selectedRoutineIDs.size();
        Log.d("HomeFragment", "In-progress routines count: " + count);

        // Update the text of the inProgressButton to just show the count
        inProgressButton.setText(String.valueOf(count));  // Set button text with the count
    }

    private void fetchCompletedRoutines() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("RoutinePrefs_" + userId, Context.MODE_PRIVATE);
        Set<String> selectedCompletedRoutines = sharedPreferences.getStringSet("COMPLETED_ROUTINES", new HashSet<>());

        // Get the count of completed routines
        int count = selectedCompletedRoutines.size();
        Log.d("HomeFragment", "Completed routines count: " + count);

        // Update the text of the completeButton to just show the count
        completeButton.setText(String.valueOf(count));  // Set button text with the count
    }

    private void updateUserProfile(UserData user) {
        Log.d("HomeFragment", "Updating user profile for userId: " + userId);

        homeUsernameText.setText(user.getUsername());
        heightNum.setText(user.getHeight() != null ? user.getHeight() : "0.00");
        weightNum.setText(user.getWeight() != null ? user.getWeight() : "0.00");

        sharedPreferences.edit()
                .putInt("USER_ID", userId)
                .putString("USERNAME", user.getUsername())
                .putString("HEIGHT", user.getHeight())
                .putString("WEIGHT", user.getWeight())
                .apply();
    }

    private void resetToGuest() {
        sharedPreferences.edit()
                .clear()
                .putInt("USER_ID", -1)
                .putString("USERNAME", "Guest")
                .putString("HEIGHT", "0.00")
                .putString("WEIGHT", "0.00")
                .apply();
        updateUIFromPreferences();
    }

    private void updateUIFromPreferences() {
        homeUsernameText.setText(sharedPreferences.getString("USERNAME", "Guest"));
        heightNum.setText(sharedPreferences.getString("HEIGHT", "0.00"));
        weightNum.setText(sharedPreferences.getString("WEIGHT", "0.00"));
    }

    private void showToast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }
}