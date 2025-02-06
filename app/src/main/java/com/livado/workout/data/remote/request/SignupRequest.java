package com.livado.workout.data.remote.request;

import com.google.gson.annotations.SerializedName;

public class SignupRequest {
    @SerializedName("user_fullname")
    private String fullname;
    @SerializedName("user_username")

    private String username;
    @SerializedName("user_email")

    private String email;
    @SerializedName("user_password")

    private String password;

    public SignupRequest(String fullname, String username, String email, String password) {
        this.fullname = fullname;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "SignUpRequest{" +
                "fullname='" + fullname + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
