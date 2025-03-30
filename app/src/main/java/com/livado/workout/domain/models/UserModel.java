package com.livado.workout.domain.models;

public class UserModel {
    private String id;
    private String fullName;
    private String username;
    private String email;

    public UserModel(String id, String fullName, String username, String email) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}
