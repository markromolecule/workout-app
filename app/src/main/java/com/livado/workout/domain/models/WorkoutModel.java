package com.livado.workout.domain.models;

public class WorkoutModel {

    private String id;
    private String name;
    private String description;
    private String imagepath;

    public WorkoutModel(String id, String name, String description, String imagepath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imagepath = imagepath;
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
}
