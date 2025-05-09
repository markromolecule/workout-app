package com.livado.workout.data.remote.request;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("user_username")
    private String username;
    @SerializedName("user_password")
    private String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
