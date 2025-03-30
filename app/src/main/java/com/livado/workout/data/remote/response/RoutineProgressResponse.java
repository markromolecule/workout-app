package com.livado.workout.data.remote.response;

import com.google.gson.annotations.SerializedName;

public class RoutineProgressResponse
{
    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public RoutineResponse data;


}
