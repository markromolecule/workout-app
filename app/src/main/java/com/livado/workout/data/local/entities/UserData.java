package com.livado.workout.data.local.entities;

import com.google.gson.annotations.SerializedName;

public class UserData {
    @SerializedName("user_fullname")
    private String user_fullname;

    @SerializedName("user_username")
    private String user_username;

    @SerializedName("user_email")
    private String user_email;

    @SerializedName("height_cm")
    private String height_cm;

    @SerializedName("weight_kg")
    private String weight_kg;

    @SerializedName("gender")
    private String gender;

    @SerializedName("userProfile")
    private String userProfile;

    // Getters
    public String getFullname() {
        return user_fullname;
    }

    public String getUsername() {
        return user_username;
    }

    public String getEmail() {
        return user_email;
    }

    public String getHeight() {
        return height_cm;
    }

    public String getWeight() {
        return weight_kg;
    }

    public String getGender() {
        return gender;
    }

    public String getUserProfile() {
        return userProfile;
    }
}