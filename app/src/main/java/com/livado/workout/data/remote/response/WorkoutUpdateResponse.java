package com.livado.workout.data.remote.response;

import java.util.List;

public class WorkoutUpdateResponse {
    private int updated;
    private int inserted;
    private List<WorkoutResponse> workouts;

    public int getUpdated() {
        return updated;
    }

    public int getInserted() {
        return inserted;
    }

    public List<WorkoutResponse> getWorkouts() {
        return workouts;
    }
}
