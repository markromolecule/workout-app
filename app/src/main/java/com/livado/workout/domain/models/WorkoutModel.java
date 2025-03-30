package com.livado.workout.domain.models;

import com.livado.workout.data.remote.response.WorkoutResponse;
import java.util.List;

public class WorkoutModel {

    private final String id;
    private final String name;
    private final String description;
    private final String imagepath;
    private final List<WorkoutResponse> workouts;

    public WorkoutModel(String id, String name, String description, String imagepath, List<WorkoutResponse> workouts) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imagepath = imagepath;
        this.workouts = workouts;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImagepath() {
        return imagepath;
    }

    public List<WorkoutResponse> getWorkouts() {
        return workouts;
    }
}