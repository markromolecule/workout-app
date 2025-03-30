package com.livado.workout.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.livado.workout.R;
import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.data.remote.request.LoginRequest;
import com.livado.workout.data.remote.response.LoginResponse;
import com.livado.workout.ui.main.home.HomeMainActivity;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setupSystemBars();
        setupUI();
    }

    /**
     * Initializes UI components and sets click listeners.
     */
    private void setupUI() {
        etUsername = findViewById(R.id.usernameField);
        etPassword = findViewById(R.id.passwordField);
        btnLogin = findViewById(R.id.loginButton);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    /**
     * Attempts login by validating inputs and sending API request.
     */
    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            showToast("Please enter both username and password");
            return;
        }

        LoginRequest loginRequest = new LoginRequest(username, password);
        Log.d("LoginRequest", "Sending: " + new Gson().toJson(loginRequest));

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.loginUser(loginRequest).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                handleLoginResponse(response);
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                showToast("Error: " + t.getMessage());
                Log.e("LoginError", "Request failed: " + t.getMessage());
            }
        });
    }

    /**
     * Handles API response for login attempt.
     */
    private void handleLoginResponse(@NonNull Response<LoginResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            LoginResponse loginResponse = response.body();
            Log.d("API_RESPONSE", new Gson().toJson(loginResponse));

            if (loginResponse.isSuccess()) {
                saveUserData(loginResponse.getUser());
            } else {
                showToast("Invalid username or password");
                Log.e("LOGIN_ERROR", "Server Response: " + new Gson().toJson(loginResponse));
            }
        } else {
            showToast("Invalid username or password");
            try {
                Log.e("LOGIN_ERROR", "Response Error: " + response.errorBody().string());
            } catch (IOException e) {
                Log.e("LOGIN_ERROR", "Error reading error body", e);
            }
        }
    }

    /**
     * Saves user data to SharedPreferences and navigates to HomeMainActivity.
     */
    private void saveUserData(LoginResponse.UserData user) {
        if (user != null) {
            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("USER_ID", user.getUserId());
            editor.putString("USERNAME", user.getUsername());
            editor.apply();

            showToast("Login successful");
            navigateToMain(user.getUsername());
        } else {
            showToast("Invalid response, user data missing");
            Log.e("LOGIN_ERROR", "User object is null in response");
        }
    }

    /**
     * Navigates to HomeMainActivity and passes username.
     */
    private void navigateToMain(String username) {
        Intent intent = new Intent(getApplicationContext(), HomeMainActivity.class);
        intent.putExtra("USERNAME", username);
        startActivity(intent);
        finish();
    }

    /**
     * Displays a short toast message.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Configures system bars for immersive UI.
     */
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.parent_login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Sets up the Sign-Up button and navigates to SignupActivity.
     */
    public void setupSignUpButton(View view) {
        Button signUpButton = findViewById(R.id.signUpButton);
        signUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
            startActivity(intent);
            finish();
        });
    }
}