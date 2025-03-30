package com.livado.workout.ui.main.profile;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import java.text.DecimalFormat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.livado.workout.R;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.response.UpdateResponse;
import com.livado.workout.data.remote.response.UserProfileResponse;
import com.livado.workout.ui.auth.LoginActivity;
import com.livado.workout.ui.main.home.HomeMainActivity;
import com.livado.workout.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView profileImage;
    private EditText fullNameField, usernameField, emailField, heightField, weightField;
    private Spinner genderSpinner;
    private Button saveButton;
    private Uri imageUri;
    private ApiService apiService;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setListeners();

        // Retrieve user ID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("USER_ID", -1);

        if (userId == -1) {
            showToast("User ID not found!");
            finish();
            return;
        }

        apiService = RetrofitClient.getClient().create(ApiService.class);
        fetchUserProfile();
    }

    /**
     * Initializes UI components.
     */
    private void initViews() {
        profileImage = findViewById(R.id.changePhotoButton);
        fullNameField = findViewById(R.id.fullNameField);
        usernameField = findViewById(R.id.usernameField);
        emailField = findViewById(R.id.emailField);
        heightField = findViewById(R.id.heightField);
        weightField = findViewById(R.id.weightField);
        genderSpinner = findViewById(R.id.genderSpinner);
        saveButton = findViewById(R.id.continueButton);

        // Set up gender spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.gender_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);
    }

    /**
     * Sets event listeners for buttons.
     */
    private void setListeners() {
        profileImage.setOnClickListener(v -> openImagePicker());
        saveButton.setOnClickListener(v -> updateUserProfile());
    }

    /**
     * Fetches user profile data from the server.
     */
    private void fetchUserProfile() {
        apiService.getUserProfile(userId).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    populateProfileFields(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                Log.e("ProfileActivity", "Error fetching profile", t);
            }
        });
    }

    /**
     * Populates user profile fields with retrieved data.
     */
    private void populateProfileFields(UserProfileResponse user) {
        fullNameField.setText(user.getData().getFullname());
        usernameField.setText(user.getData().getUsername());
        emailField.setText(user.getData().getEmail());
        heightField.setText(user.getData().getHeight());
        weightField.setText(user.getData().getWeight());

        if (user.getData().getGender() != null) {
            String gender = user.getData().getGender().toLowerCase();
            genderSpinner.setSelection(gender.equals("male") ? 0 : gender.equals("female") ? 1 : 2);
        }

        Glide.with(ProfileActivity.this)
                .load(user.getData().getUserProfile())
                .into(profileImage);
    }

    /**
     * Opens the image picker.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * Handles image selection result.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            if (imageUri != null) {
                profileImage.setImageURI(imageUri);
            } else {
                showToast("Failed to select image");
            }
        }
    }

    /**
     * Updates user profile by making an API call.
     */
    private void updateUserProfile() {
        DecimalFormat df = new DecimalFormat("0.00");

        final String formattedHeight = heightField.getText().toString().trim().isEmpty()
                ? "0.00" : df.format(Double.parseDouble(heightField.getText().toString().trim()));

        final String formattedWeight = weightField.getText().toString().trim().isEmpty()
                ? "0.00" : df.format(Double.parseDouble(weightField.getText().toString().trim()));

        String selectedGender = genderSpinner.getSelectedItem() != null
                ? genderSpinner.getSelectedItem().toString().trim()
                : "";

        apiService.updateUserProfile(
                RequestBody.create(MediaType.parse("text/plain"), String.valueOf(userId)),
                createTextPart(fullNameField),
                createTextPart(usernameField),
                createTextPart(emailField),
                RequestBody.create(MediaType.parse("text/plain"), formattedHeight),
                RequestBody.create(MediaType.parse("text/plain"), formattedWeight),
                RequestBody.create(MediaType.parse("text/plain"), selectedGender),
                createImagePart()
        ).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<UpdateResponse> call, @NonNull Response<UpdateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showToast(response.body().getMessage());

                    // Save formatted height and weight in SharedPreferences
                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("USERNAME", usernameField.getText().toString().trim());
                    editor.putString("HEIGHT", formattedHeight);
                    editor.putString("WEIGHT", formattedWeight);
                    editor.apply();

                    // Inform HomeFragment of the update
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updated", true);
                    setResult(Activity.RESULT_OK, resultIntent);

                    finish();
                } else {
                    Log.e("API_ERROR", "Error Response: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(@NonNull Call<UpdateResponse> call, @NonNull Throwable t) {
                Log.e("ProfileActivity", "Error updating profile", t);
            }
        });
    }

    /**
     * Creates a text-based RequestBody.
     */
    private RequestBody createTextPart(EditText editText) {
        return RequestBody.create(MediaType.parse("text/plain"), editText.getText().toString().trim());
    }

    /**
     * Creates an image-based MultipartBody part.
     */
    private MultipartBody.Part createImagePart() {
        if (imageUri == null) return null;
        String imagePath = FileUtils.getPath(this, imageUri);
        if (imagePath == null) return null;

        File file = new File(imagePath);
        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(imageUri)), file);
        return MultipartBody.Part.createFormData("userProfile", file.getName(), requestFile);
    }

    /**
     * Displays a short toast message.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Handles back button navigation to HomeMainActivity.
     */
    public void setupBackButton(View view) {
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), HomeMainActivity.class));
            finish();
        });
    }

    /**
     * Logs out the user, clears session data, and navigates to LoginActivity.
     */
    public void setupExitButton(View view) {
        ImageButton exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> {
            // Clear all stored preferences
            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            sharedPreferences.edit().clear().apply(); // This clears all preferences stored under "UserPrefs"

            // Redirect to LoginActivity
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears the activity stack
            startActivity(intent);
            finish(); // Ensure current activity is finished
        });
    }
}