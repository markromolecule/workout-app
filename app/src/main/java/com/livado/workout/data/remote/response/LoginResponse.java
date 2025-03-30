package com.livado.workout.data.remote.response;

import android.util.Log;

import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;

public class LoginResponse {
    @SerializedName("success")
    private Boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("user")
    private UserData user;

    public boolean isSuccess() {
        Log.d("LOGIN_RESPONSE", "Success field value: " + success);
        return success != null && success;
    }

    public String getMessage() {
        return message != null ? message : "No message provided";
    }

    public UserData getUser() {
        return user;
    }

    public static class UserData {
        @SerializedName("user_id")
        private int userId;

        @SerializedName("user_username")
        private String username;

        @SerializedName("user_email")
        private String email;

        public int getUserId() {
            return userId;
        }

        public String getUsername() {
            return username != null ? username : "Unknown User";
        }

        public String getEmail() {
            return email != null ? email : "No email provided";
        }

        @NonNull
        @Override
        public String toString() {
            return "UserData{" +
                    "userId=" + userId +
                    ", username='" + getUsername() + '\'' +
                    ", email='" + getEmail() + '\'' +
                    '}';
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "LoginResponse {" +
                "success=" + isSuccess() +
                ", message='" + getMessage() + '\'' +
                ", user=" + (user != null ? user.toString() : "null") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginResponse that = (LoginResponse) o;
        return Objects.equals(success, that.success) &&
                Objects.equals(message, that.message) &&
                Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, message, user);
    }
}