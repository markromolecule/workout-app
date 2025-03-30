package com.livado.workout.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.livado.workout.R;
import com.livado.workout.data.remote.response.LoginResponse;
import com.livado.workout.domain.repository.AuthRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private EditText etFullname, etUsername, etEmail, etPassword;
    private Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        setupUI();
    }

    /**
     * Initializes UI components and sets click listeners.
     */
    private void setupUI() {
        etFullname = findViewById(R.id.fullNameField);
        etUsername = findViewById(R.id.usernameField);
        etEmail = findViewById(R.id.emailField);
        etPassword = findViewById(R.id.passwordField);
        btnSignUp = findViewById(R.id.signupButton);

        btnSignUp.setOnClickListener(v -> registerUser());
    }

    /**
     * Validates input fields and attempts user registration.
     */
    private void registerUser() {
        String fullname = etFullname.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!isValidInput(fullname, username, email, password)) {
            return;
        }

        AuthRepository authRepository = new AuthRepository();
        authRepository.signUp(fullname, username, email, password).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                handleSignupResponse(response);
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                showToast("Request failed. Please check your connection.");
            }
        });
    }

    /**
     * Validates user input fields.
     */
    private boolean isValidInput(String fullname, String username, String email, String password) {
        if (TextUtils.isEmpty(fullname) || TextUtils.isEmpty(username) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast("All fields must be filled");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email");
            return false;
        }

        if (password.length() < 6) {
            showToast("Password must be at least 6 characters");
            return false;
        }
        return true;
    }

    /**
     * Handles API response for signup attempt.
     */
    private void handleSignupResponse(@NonNull Response<LoginResponse> response) {
        if (response.isSuccessful()) {
            LoginResponse loginResponse = response.body();
            Log.d("SignupActivity", "API Response: " + loginResponse);

            if (loginResponse != null && loginResponse.isSuccess()) {
                showToast("Sign-up successful");
                navigateToLogin();
            } else {
                showToast(loginResponse != null ? loginResponse.getMessage() : "Signup failed. Unknown error.");
            }
        } else {
            showToast("Signup failed. Server error.");
        }
    }

    /**
     * Displays a short toast message.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Navigates to LoginActivity.
     */
    private void navigateToLogin() {
        Log.d("SignupActivity", "Navigating to login");
        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Handles back button navigation to LoginActivity.
     */
    public void setupBackButton(View view) {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        });
    }
}