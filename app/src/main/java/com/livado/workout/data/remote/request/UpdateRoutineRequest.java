package com.livado.workout.data.remote.request;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UpdateRoutineRequest {

    @SerializedName("routine_id")
    private int routineID;

    @SerializedName("workouts")
    private List<UpdateWorkoutRequest> workouts;

    public UpdateRoutineRequest(int routineID, List<UpdateWorkoutRequest> workouts) {
        this.routineID = routineID;
        this.workouts = workouts;
    }

    // Getters and Setters
    public int getRoutineID() {
        return routineID;
    }

    public void setRoutineID(int routineID) {
        this.routineID = routineID;
    }

    public List<UpdateWorkoutRequest> getWorkouts() {
        return workouts;
    }

    public void setWorkouts(List<UpdateWorkoutRequest> workouts) {
        this.workouts = workouts;
    }
}

