package com.livado.workout.data.remote.response;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.livado.workout.data.local.entities.User;

import java.util.Objects;

public class LoginResponse {
    @SerializedName("success")
    private String success;

    @SerializedName("message")
    private String message;

    @SerializedName("user")
    private User user;

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(success);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @NonNull
    @Override
    public String toString() {
        return "LoginResponse {" +
                "success='" + success + '\'' +
                ", message='" + (message != null ? message : "null") + '\'' +
                ", user=" + (user != null ? user.toString() : "null") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginResponse that = (LoginResponse) o;
        return  Objects.equals(success, that.success) &&
                Objects.equals(message, that.message) &&
                Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, message, user);
    }
}