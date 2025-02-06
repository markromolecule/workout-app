package com.livado.workout.ui.main.workout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.Manifest;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.livado.workout.R;
import com.livado.workout.data.remote.api.ApiService;
import com.livado.workout.data.remote.response.LoginResponse;
import com.livado.workout.data.remote.api.RetrofitClient;
import com.livado.workout.utils.FileUtils;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddWorkoutActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private EditText etWorkoutName, etWorkoutDescription, etWorkoutInstrument;
    private Spinner workoutCategorySpinner, workoutDifficultySpinner;
    private Button btnSubmitWorkout;
    private ImageView workoutImage;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_workout);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // Android 9 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }

        etWorkoutName = findViewById(R.id.workoutName);
        etWorkoutDescription = findViewById(R.id.workoutDescription);
        etWorkoutInstrument = findViewById(R.id.workoutInstrument);
        workoutCategorySpinner = findViewById(R.id.workoutCategory);
        workoutDifficultySpinner = findViewById(R.id.workoutDifficulty);
        btnSubmitWorkout = findViewById(R.id.submitWorkout);
        workoutImage = findViewById(R.id.workoutImage);

        setupSpinner(workoutCategorySpinner, R.array.workout_categories);
        setupSpinner(workoutDifficultySpinner, R.array.workout_difficulty);

        workoutImage.setOnClickListener(v -> openImageChooser());

        btnSubmitWorkout.setOnClickListener(v -> uploadWorkout());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Permission granted");
            } else {
                showToast("Permission denied. Cannot access images.");
            }
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                workoutImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Error opening image");
            }
        }
    }

    private void uploadWorkout() {
        String workoutName = etWorkoutName.getText().toString().trim();
        String workoutDescription = etWorkoutDescription.getText().toString().trim();
        String workoutInstrument = etWorkoutInstrument.getText().toString().trim();
        String workoutCategory = workoutCategorySpinner.getSelectedItem().toString();
        String workoutDifficulty = workoutDifficultySpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(workoutName) || TextUtils.isEmpty(workoutDescription) || TextUtils.isEmpty(workoutInstrument) || selectedImageUri == null) {
            showToast("All fields and an image are required");
            return;
        }

        // 🔹 Get a valid file path
        String imagePath = FileUtils.getPath(this, selectedImageUri);
        if (imagePath == null) {
            showToast("Error: Cannot access image file");
            return;
        }

        File imageFile = new File(imagePath);
        Log.d("UploadDebug", "Image file path: " + imageFile.getAbsolutePath());

        if (!imageFile.exists()) {
            showToast("Error: Image file does not exist");
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("workoutImagePath", imageFile.getName(), requestFile);

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.addWorkout(
                RequestBody.create(MediaType.parse("text/plain"), workoutName),
                RequestBody.create(MediaType.parse("text/plain"), workoutCategory),
                RequestBody.create(MediaType.parse("text/plain"), workoutDescription),
                RequestBody.create(MediaType.parse("text/plain"), workoutDifficulty),
                RequestBody.create(MediaType.parse("text/plain"), workoutInstrument),
                imagePart
        ).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showToast("Workout added successfully");
                    finish();
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        showToast("Failed to add workout: " + errorBody);
                    } catch (IOException e) {
                        showToast("Failed to add workout: Unknown error");
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showToast("Error: " + t.getMessage());
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setupSpinner(Spinner spinner, int arrayResId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                arrayResId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
}