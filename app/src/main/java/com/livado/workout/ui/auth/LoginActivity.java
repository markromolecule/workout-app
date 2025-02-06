package com.livado.workout.ui.auth;

import android.content.Intent;
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
import com.livado.workout.ui.main.home.MainActivity;

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

        etUsername = findViewById(R.id.usernameField);
        etPassword = findViewById(R.id.passwordField);
        btnLogin = findViewById(R.id.loginButton);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            showToast("Please enter both username and password");
            return;
        }

        // Create a LoginRequest object
        LoginRequest loginRequest = new LoginRequest(username, password);

        // Log the request body
        Log.d("LoginRequest", "Sending: " + new Gson().toJson(loginRequest));

        // Make the API call
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.loginUser(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess()) {
                        showToast("Login successful");
                        navigateToMain(username);
                    } else {
                        showToast("Invalid username or password");
                        Log.d("LoginError", "Server Response: " + new Gson().toJson(loginResponse));
                    }
                } else {
                    showToast("Invalid username or password");
                    Log.d("LoginError", "Response Error: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showToast("Error: " + t.getMessage());
                Log.d("LoginError", "Request failed: " + t.getMessage());
            }
        });
    }

    private void navigateToMain(String username) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("USERNAME", username);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.parent_login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void setupSignUpButton(View view) {
        Button signUpButton = findViewById(R.id.signUpButton);
        signUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
