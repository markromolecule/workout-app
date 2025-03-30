package com.livado.workout.data.remote.request;

import com.google.gson.annotations.SerializedName;

public class UpdateWorkoutRequest {
    @SerializedName("user_id")
    private int userID;

    @SerializedName("routine_id")
    private int routineID;

    @SerializedName("workout_id")
    private int workoutID;

    @SerializedName("sets")
    private int sets;

    @SerializedName("reps")
    private int reps;

    @SerializedName("duration")
    private int duration;

    public UpdateWorkoutRequest(int userID, int routineID, int workoutID, int sets, int reps, int duration) {
        this.userID = userID;
        this.routineID = routineID;
        this.workoutID = workoutID;
        this.sets = sets;
        this.reps = reps;
        this.duration = duration;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public int getRoutineID() {
        return routineID;
    }

    public void setRoutineID(int routineID) {
        this.routineID = routineID;
    }

    public int getWorkoutID() {
        return workoutID;
    }

    public void setWorkoutID(int workoutID) {
        this.workoutID = workoutID;
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}