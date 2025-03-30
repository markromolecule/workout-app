package com.livado.workout.data.remote.request;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a request to update workout progress in the system.
 */
public class WorkoutProgressRequest {

    @SerializedName("user_id")
    private int userId;
    @SerializedName("routine_id")

    private int routineId;
    @SerializedName("workout_id")

    private int workoutId;
    @SerializedName("status")

    private String status;

    @Nullable
    private String startTime;

    @Nullable
    private String completionTime;

    // ✅ Constructor for starting a workout (Defaults handled in API)
    public WorkoutProgressRequest(int userId, int routineId, int workoutId, String status) {
        this.userId = userId;
        this.routineId = routineId;
        this.workoutId = workoutId;
        this.status = status;
        this.startTime = null;  // Will be handled in API
        this.completionTime = null;
    }

    // ✅ Constructor for logging the start of a workout
    public WorkoutProgressRequest(int userId, int routineId, int workoutId, String status, @Nullable String startTime) {
        this.userId = userId;
        this.routineId = routineId;
        this.workoutId = workoutId;
        this.status = status;
        this.startTime = startTime;
        this.completionTime = null;
    }

    // ✅ Constructor for completing a workout
    public WorkoutProgressRequest(int userId, int routineId, int workoutId, String status, @Nullable String startTime, @Nullable String completionTime) {
        this.userId = userId;
        this.routineId = routineId;
        this.workoutId = workoutId;
        this.status = status;
        this.startTime = startTime;
        this.completionTime = completionTime;
    }

    // 🔹 Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getRoutineId() { return routineId; }
    public void setRoutineId(int routineId) { this.routineId = routineId; }

    public int getWorkoutId() { return workoutId; }
    public void setWorkoutId(int workoutId) { this.workoutId = workoutId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Nullable
    public String getStartTime() { return startTime; }
    public void setStartTime(@Nullable String startTime) { this.startTime = startTime; }

    @Nullable
    public String getCompletionTime() { return completionTime; }
    public void setCompletionTime(@Nullable String completionTime) { this.completionTime = completionTime; }

    @Override
    public String toString() {
        return "WorkoutProgressRequest{" +
                "userId=" + userId +
                ", routineId=" + routineId +
                ", workoutId=" + workoutId +
                ", status='" + status + '\'' +
                ", startTime=" + (startTime != null ? "'" + startTime + "'" : "null") +
                ", completionTime=" + (completionTime != null ? "'" + completionTime + "'" : "null") +
                '}';
    }
}