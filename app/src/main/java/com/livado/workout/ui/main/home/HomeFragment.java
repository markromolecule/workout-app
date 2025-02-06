package com.livado.workout.ui.main.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.livado.workout.R;
import com.livado.workout.ui.main.timer.TimerActivity;
import com.livado.workout.ui.main.workout.WorkoutActivity;

public class HomeFragment extends Fragment {

    private TextView username_text_view;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        username_text_view = view.findViewById(R.id.homeUsernameText);

        if (getArguments() != null) {
            String username = getArguments().getString("USERNAME", "Guest");
            username_text_view.setText(username);
            Log.d("HomeFragment", "Received Username: " + username);
        } else {
            Log.e("HomeFragment", "getArguments() is NULL");
        }

        setupTimerButton(view);
        setupWorkoutButton(view);

        return view;
    }

    public void setupTimerButton(View view) {
        ImageButton timerButton = view.findViewById(R.id.timerButton);
        timerButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TimerActivity.class);
            startActivity(intent);
        });
    }

    public void setupWorkoutButton(View view) {
        ImageButton timerButton = view.findViewById(R.id.timerButton);
        timerButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), WorkoutActivity.class);
            startActivity(intent);
        });
    }
}