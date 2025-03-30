package com.livado.workout.data.remote.response;

import com.google.gson.annotations.SerializedName;

public class UpdateResponse {
    private String success;
    private String message;
    @SerializedName("isCompleted")
    private boolean isCompleted;

    public String getSuccess() { return success; }
    public String getMessage() { return message; }
    public boolean isCompleted() {
        return isCompleted;
    }
}