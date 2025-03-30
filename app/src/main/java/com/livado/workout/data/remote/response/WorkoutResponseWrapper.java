package com.livado.workout.data.remote.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WorkoutResponseWrapper {
    @SerializedName("workoutID")
    private int workoutId;
    @SerializedName("success")
    private boolean success;
    @SerializedName("message")
    private String message;
    @SerializedName("data")
    private List<WorkoutResponse> data;  // Change to a list

    public int getWorkoutId() { return workoutId; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<WorkoutResponse> getData() { return data; }  // Return the list
}
