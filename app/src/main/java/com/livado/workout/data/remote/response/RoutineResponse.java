package com.livado.workout.data.remote.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RoutineResponse {

    @SerializedName("routineID")
    private int routineID;
    @SerializedName("routineName")
    private String routineName;
    @SerializedName("routineDescription")
    private String routineDescription;
    @SerializedName("routineDuration")
    private int routineDuration;
    @SerializedName("routineDifficulty")
    private String routineDifficulty;
    @SerializedName("exerciseCount")
    private int exerciseCount;
    @SerializedName("workouts")
    private List<WorkoutResponse> workouts;

    private boolean isSelected;

    public RoutineResponse(int routineID,
                           String routineName,
                           String routineDescription,
                           int routineDuration,
                           String routineDifficulty,
                           int exerciseCount,
                           List<WorkoutResponse> workouts) {

        this.routineID = routineID;
        this.routineName = routineName;
        this.routineDescription = routineDescription;
        this.routineDuration = routineDuration;
        this.routineDifficulty = routineDifficulty;
        this.exerciseCount = exerciseCount;
        this.workouts = workouts;
        this.isSelected = false;
    }

    // ✅ Getters
    public int getRoutineID() { return routineID; }
    public String getRoutineName() { return routineName; }
    public String getRoutineDescription() { return routineDescription; }
    public int getDuration() { return routineDuration; }
    public String getDifficulty() { return routineDifficulty; }
    public int getExerciseCount() { return exerciseCount; }
    public List<WorkoutResponse> getWorkouts() { return workouts; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { this.isSelected = selected; }

    @Override
    public String toString() {
        return "RoutineResponse{" +
                "routineID=" + routineID +
                ", routineName='" + routineName + '\'' +
                ", routineDescription='" + routineDescription + '\'' +
                ", duration=" + routineDuration +
                ", difficulty='" + routineDifficulty + '\'' +
                ", exerciseCount=" + exerciseCount +
                ", workouts=" + (workouts != null ? workouts.toString() : "null") +
                '}';
    }
}