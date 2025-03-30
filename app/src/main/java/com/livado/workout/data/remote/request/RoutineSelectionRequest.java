package com.livado.workout.data.remote.request;

import com.google.gson.annotations.SerializedName;

public class RoutineSelectionRequest {
    @SerializedName("user_id")

    private int userId;
    @SerializedName("routine_id")
    private int routineId;

    public RoutineSelectionRequest(int userId, int routineId) {
        this.userId = userId;
        this.routineId = routineId;
    }

    public int getUserId() {
        return userId;
    }

    public int getRoutineId() {
        return routineId;
    }
}