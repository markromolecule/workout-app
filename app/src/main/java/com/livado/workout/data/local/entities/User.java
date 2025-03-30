package com.livado.workout.data.local.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "auth")
public class User {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    private int userID;
    @ColumnInfo(name = "user_fullname")
    private String fullname;
    @ColumnInfo(name = "user_username")
    private String username;
    @ColumnInfo(name = "user_email")
    private String email;
    @ColumnInfo(name = "user_password")
    private String password;

    // Constructor for creating User from SignupRequest
    @Ignore
    public User(int userID, String fullname, String username, String email, String password) {
        this.userID = userID;
        this.fullname = fullname;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public User(String fullname, String username, String email, String password) {
        this.fullname = fullname;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Getters and setters for all fields
    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
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

    // Override toString for debugging/logging purposes
    @Override
    public String toString() {
        return "User{" +
                "id=" + userID +
                ", fullname='" + fullname + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'';
    }
}
