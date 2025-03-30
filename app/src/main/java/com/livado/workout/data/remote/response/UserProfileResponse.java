package com.livado.workout.data.remote.response;

import com.google.gson.annotations.SerializedName;
import com.livado.workout.data.local.entities.UserData;

public class UserProfileResponse {
    @SerializedName("data")
    private UserData data;

    public UserData getData() {
        return data;
    }
}