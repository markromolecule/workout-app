package com.livado.workout.data.remote.response;

import com.google.gson.annotations.SerializedName;

public class WorkoutResponse {
    @SerializedName("workoutName")
    private String workoutName;
    @SerializedName("workoutCategory")
    private String workoutCategory;
    @SerializedName("workoutDescription")

    private String workoutDescription;
    @SerializedName("workoutImagePath")

    private String workoutImagePath;
    @SerializedName("workoutDifficulty")

    private String workoutDifficulty;
    @SerializedName("workoutEquipment")

    private String workoutEquipment;

    public String getWorkoutName() {
        return workoutName;
    }

    public String getWorkoutCategory() {
        return workoutCategory;
    }

    public String getWorkoutDescription() {
        return workoutDescription;
    }

    public String getWorkoutImagePath() {
        return workoutImagePath;
    }

    public String getWorkoutDifficulty() {
        return workoutDifficulty;
    }

    public String getWorkoutEquipment() {
        return workoutEquipment;
    }
}