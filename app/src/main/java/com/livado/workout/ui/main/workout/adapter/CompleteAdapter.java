package com.livado.workout.ui.main.workout.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.livado.workout.R;
import com.livado.workout.data.remote.response.RoutineResponse;
import com.livado.workout.ui.main.workout.RoutineMainActivity;

import java.util.List;

public class CompleteAdapter extends RecyclerView.Adapter<CompleteAdapter.CompleteViewHolder> {

    private final List<RoutineResponse> routines;
    private final int userId;
    private final Context context;

    public CompleteAdapter(List<RoutineResponse> routines, int userId, Context context) {
        this.routines = routines;
        this.userId = userId;
        this.context = context;
    }

    @NonNull
    @Override
    public CompleteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.routine_item, parent, false);
        return new CompleteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompleteViewHolder holder, int position) {
        RoutineResponse routine = routines.get(position);

        holder.routineName.setText(routine.getRoutineName());
        holder.routineDescription.setText(routine.getRoutineDescription() != null ? routine.getRoutineDescription() : "No description available");
        holder.difficulty.setText("Difficulty: " + (routine.getDifficulty() != null ? routine.getDifficulty() : "Unknown"));
        holder.duration.setText("Duration: " + routine.getDuration() + " min");
        holder.exerciseCount.setText("Exercises: " + routine.getExerciseCount());

        // Handle routine click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RoutineMainActivity.class);
            intent.putExtra("selectedRoutineID", routine.getRoutineID());
            intent.putExtra("userId", userId);
            context.startActivity(intent);
        });
    }

    public void updateData(List<RoutineResponse> newCompletedRoutines) {
        this.routines.clear();
        this.routines.addAll(newCompletedRoutines);  // Add the new data
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return routines.size();
    }

    public void clearData() {
        routines.clear();
        notifyDataSetChanged();
    }

    public static class CompleteViewHolder extends RecyclerView.ViewHolder {
        TextView routineName, routineDescription, duration, difficulty, exerciseCount;

        public CompleteViewHolder(@NonNull View itemView) {
            super(itemView);
            routineName = itemView.findViewById(R.id.routineName);
            routineDescription = itemView.findViewById(R.id.routineDescription);
            duration = itemView.findViewById(R.id.duration);
            difficulty = itemView.findViewById(R.id.difficulty);
            exerciseCount = itemView.findViewById(R.id.exerciseCount);
        }
    }
}