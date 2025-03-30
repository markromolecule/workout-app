package com.livado.workout.data.remote.response;

import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

public class WorkoutResponse {
    @SerializedName("workoutID")
    private int workoutId;
    @SerializedName("workoutName")
    private String workoutName;
    @SerializedName("workoutDescription")
    private String workoutDescription;
    @SerializedName("workoutDifficulty")
    private String workoutDifficulty;
    @SerializedName("workoutEquipment")
    private String workoutEquipment;
    @SerializedName("sets")
    private int sets;
    @SerializedName("reps")
    private int reps;
    @SerializedName("duration")
    private int duration;
    @SerializedName("workoutImageURL")
    private String workoutImagePath;
    @SerializedName("isCompleted")
    private boolean isCompleted;

    public WorkoutResponse(int workoutId,
                           String workoutName,
                           String workoutDescription,
                           String workoutDifficulty,
                           String workoutEquipment,
                           String workoutImagePath,
                           int sets,
                           int reps,
                           int duration,
                           boolean isCompleted) {

        this.workoutId = workoutId;
        this.workoutName = workoutName;
        this.workoutDescription = workoutDescription;
        this.workoutDifficulty = workoutDifficulty;
        this.workoutEquipment = workoutEquipment;
        this.sets = sets;
        this.reps = reps;
        this.duration = duration;
        this.workoutImagePath = workoutImagePath;
        this.isCompleted = isCompleted;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
    public void setCompleted(boolean isCompleted) { this.isCompleted = isCompleted; }
    // setters

    public void setSets(int sets) { this.sets = sets; }
    public void setReps(int reps) { this.reps = reps; }
    public void setDuration(int duration) { this.duration = duration; }

    // Getters
    public int getWorkoutId() { return workoutId; }
    public String getWorkoutName() { return workoutName; }
    public String getWorkoutDescription() { return workoutDescription; }
    public String getWorkoutDifficulty() { return workoutDifficulty; }
    public String getWorkoutEquipment() { return workoutEquipment; }
    public int getSets() { return sets; }
    public int getReps() { return reps; }
    public int getDuration() { return duration; }
    public String getWorkoutImagePath() { return workoutImagePath; }

    @NonNull
    @Override
    public String toString() {
        return "WorkoutResponse{" +
                "workoutId=" + workoutId +
                ", workoutName='" + workoutName + '\'' +
                ", workoutDescription='" + workoutDescription + '\'' +
                ", workoutDifficulty='" + workoutDifficulty + '\'' +
                ", workoutEquipment='" + workoutEquipment + '\'' +
                ", sets=" + sets +
                ", reps=" + reps +
                ", duration=" + duration +
                ", workoutImagePath='" + workoutImagePath + '\'' +
                '}';
    }
}