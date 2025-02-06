package com.livado.workout.data.remote.request;

public class WorkoutRequest {
    private String workoutName;
    private String workoutCategory;
    private String workoutDifficulty;
    private String workoutDescription;
    private String workoutEquipment;

    private String imageUrl;

    public WorkoutRequest(String workoutName, String workoutCategory, String workoutDifficulty,
                          String workoutEquipment, String workoutDescription, String imageUrl) {
        this.workoutName = workoutName;
        this.workoutCategory = workoutCategory;
        this.workoutDifficulty = "Beginner";
        this.workoutEquipment = workoutEquipment;
        this.workoutDescription = workoutDescription;
        this.imageUrl = imageUrl;
    }

    public String getWorkoutName() {
        return workoutName;
    }

    public void setWorkoutName(String workoutName) {
        this.workoutName = workoutName;
    }

    public String getWorkoutCategory() {
        return workoutCategory;
    }

    public String getWorkoutDifficulty() {
        return workoutDifficulty;
    }

    public void setWorkoutDifficulty(String workoutDifficulty) {
        this.workoutDifficulty = workoutDifficulty;
    }

    public void setWorkoutCategory(String workoutCategory) {
        this.workoutCategory = workoutCategory;
    }

    public String getWorkoutDescription() {
        return workoutDescription;
    }

    public void setWorkoutDescription(String workoutDescription) {
        this.workoutDescription = workoutDescription;
    }

    public String getWorkoutEquipment() {
        return workoutEquipment;
    }

    public void setWorkoutEquipment(String workoutEquipment) {
        this.workoutEquipment = workoutEquipment;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}

