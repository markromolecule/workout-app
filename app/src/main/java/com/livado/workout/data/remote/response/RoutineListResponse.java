package com.livado.workout.data.remote.response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RoutineListResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public Data data;
    public static class Data {
        @SerializedName("routines")
        public List<RoutineResponse> routines;
    }
}
