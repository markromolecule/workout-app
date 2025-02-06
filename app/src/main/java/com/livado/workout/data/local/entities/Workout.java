package com.livado.workout.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "workouts")
public class Workout {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "workoutID")
    private int workoutID;

    @ColumnInfo(name = "workoutName")
    private String workoutName;

    @ColumnInfo(name = "workoutCategory")
    private String workoutCategory;

    @ColumnInfo(name = "workoutDescription")
    private String workoutDescription;

    @ColumnInfo(name = "workoutImagePath")
    private String workoutImagePath;

    @ColumnInfo(name = "workoutDifficulty")
    private String workoutDifficulty;

    @ColumnInfo(name = "workoutEquipment")
    private String workoutEquipment;

    // Default constructor required by Room (must be public)
    public Workout() {
        this.workoutDifficulty = "Beginner";  // Default value
        this.workoutEquipment = "";           // Default value
    }

    // Constructor with ID (this constructor is ignored by Room)
    @Ignore
    public Workout(int workoutID, String workoutName, String workoutCategory, String workoutDescription, String workoutImagePath,
                   String workoutDifficulty, String workoutEquipment) {
        this.workoutID = workoutID;
        this.workoutName = workoutName;
        this.workoutCategory = workoutCategory;
        this.workoutDescription = workoutDescription;
        this.workoutImagePath = workoutImagePath;
        this.workoutDifficulty = workoutDifficulty;
        this.workoutEquipment = workoutEquipment;
    }

    // Constructor without ID (used for adding new workouts)
    public Workout(String workoutName, String workoutCategory, String workoutDescription, String workoutImagePath,
                   String workoutDifficulty, String workoutEquipment) {
        this.workoutName = workoutName;
        this.workoutCategory = workoutCategory;
        this.workoutDescription = workoutDescription;
        this.workoutImagePath = workoutImagePath;
        this.workoutDifficulty = workoutDifficulty != null ? workoutDifficulty : "Beginner"; // Default value if null
        this.workoutEquipment = workoutEquipment != null ? workoutEquipment : ""; // Default value if null
    }

    // Getters and Setters
    public int getWorkoutID() {
        return workoutID;
    }

    public void setWorkoutID(int workoutID) {
        this.workoutID = workoutID;
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

    public void setWorkoutCategory(String workoutCategory) {
        this.workoutCategory = workoutCategory;
    }

    public String getWorkoutDescription() {
        return workoutDescription;
    }

    public void setWorkoutDescription(String workoutDescription) {
        this.workoutDescription = workoutDescription;
    }

    public String getWorkoutImagePath() {
        return workoutImagePath;
    }

    public void setWorkoutImagePath(String workoutImagePath) {
        this.workoutImagePath = workoutImagePath;
    }

    public String getWorkoutDifficulty() {
        return workoutDifficulty;
    }

    public void setWorkoutDifficulty(String workoutDifficulty) {
        this.workoutDifficulty = workoutDifficulty;
    }

    public String getWorkoutEquipment() {
        return workoutEquipment;
    }

    public void setWorkoutEquipment(String workoutEquipment) {
        this.workoutEquipment = workoutEquipment;
    }

    @Override
    public String toString() {
        return "Workout{" +
                "workoutID=" + workoutID +
                ", workoutName='" + workoutName + '\'' +
                ", workoutCategory='" + workoutCategory + '\'' +
                ", workoutDescription='" + workoutDescription + '\'' +
                ", workoutImagePath='" + workoutImagePath + '\'' +
                ", workoutDifficulty='" + workoutDifficulty + '\'' +
                ", workoutEquipment='" + workoutEquipment + '\'' +
                '}';
    }
}