package com.livado.workout.ui.main.timer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.livado.workout.R;
import com.livado.workout.ui.main.home.HomeMainActivity;
import com.github.ybq.android.spinkit.SpinKitView;
import com.github.ybq.android.spinkit.style.Circle;

import java.util.Locale;

public class TimerActivity extends AppCompatActivity {

    private TextView timerTextView;
    private EditText inputTimeEditText; // NEW: User input field
    private Button startStopButton;
    private SpinKitView progressBar;
    private Handler handler;
    private Runnable timerRunnable;
    private boolean isRunning = false;
    private int remainingTime = 0;  // Time in seconds (updated dynamically)
    private boolean isTimerStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_timer);

        // Initialize the views
        timerTextView = findViewById(R.id.timerTextView);
        inputTimeEditText = findViewById(R.id.inputTimeEditText); // NEW
        startStopButton = findViewById(R.id.startStopButton);
        progressBar = findViewById(R.id.progressBar);
        handler = new Handler();

        // Set up window insets for edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up the start/stop button functionality
        startStopButton.setOnClickListener(v -> {
            if (isRunning) {
                stopTimer();  // Stop timer if it's running
            } else {
                startTimer();  // Start timer if it's not running
            }
        });
    }

    private void startTimer() {
        String inputTime = inputTimeEditText.getText().toString();

        if (!isTimerStarted) { // If timer hasn't started, fetch the time from input
            if (TextUtils.isEmpty(inputTime)) {
                inputTimeEditText.setError("Enter a time in seconds");
                return;
            }
            remainingTime = Integer.parseInt(inputTime);
            isTimerStarted = true;
        }

        isRunning = true;
        startStopButton.setText("STOP");

        // Start the Circle progress animation for the progress bar
        Circle circle = new Circle();
        progressBar.setIndeterminateDrawable(circle);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (remainingTime > 0) {
                    remainingTime--;
                    updateUI();
                    handler.postDelayed(this, 1000); // Update every second
                } else {
                    stopTimer();  // Stop the timer when it reaches zero
                }
            }
        };

        // Start the timer
        handler.post(timerRunnable);
    }

    private void stopTimer() {
        isRunning = false;
        startStopButton.setText("START");
        handler.removeCallbacks(timerRunnable); // Stop the timer

        // Reset UI
        isTimerStarted = false;
    }

    private void updateUI() {
        // Update the timer display
        String time = formatTime(remainingTime);
        timerTextView.setText(time);
    }

    private String formatTime(int seconds) {
        return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
    }

    public void setupBackButton(View view) {
        findViewById(R.id.backButton).setOnClickListener(v -> {
            Intent intent = new Intent(TimerActivity.this, HomeMainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}