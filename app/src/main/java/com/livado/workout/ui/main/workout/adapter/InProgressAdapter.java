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
import java.util.Set;

public class InProgressAdapter extends RecyclerView.Adapter<InProgressAdapter.InProgressViewHolder> {

    private final List<RoutineResponse> routines;
    private final int userId;
    private final Context context;
    private final Set<String> inProgressRoutines;

    public InProgressAdapter(List<RoutineResponse> routines, int userId, Context context, Set<String> inProgressRoutines) {
        this.routines = routines;
        this.userId = userId;
        this.context = context;
        this.inProgressRoutines = inProgressRoutines;
    }

    @NonNull
    @Override
    public InProgressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.routine_item, parent, false);
        return new InProgressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InProgressViewHolder holder, int position) {
        RoutineResponse routine = routines.get(position);

        holder.routineName.setText(routine.getRoutineName());
        holder.routineDescription.setText(routine.getRoutineDescription() != null ? routine.getRoutineDescription() : "No description available");
        holder.difficulty.setText("Difficulty: " + (routine.getDifficulty() != null ? routine.getDifficulty() : "Unknown"));
        holder.duration.setText("Duration: " + routine.getDuration() + " min");
        holder.exerciseCount.setText("Exercises: " + routine.getExerciseCount());

        // Set a default background, no alpha manipulation
        holder.itemView.setAlpha(1.0f);
        holder.itemView.setBackgroundResource(R.drawable.default_routine_background);  // Default background for all routines

        // Handle routine click
        holder.itemView.setOnClickListener(v -> {
            // Navigate to RoutineMainActivity
            Intent intent = new Intent(context, RoutineMainActivity.class);
            intent.putExtra("selectedRoutineID", routine.getRoutineID());
            intent.putExtra("userId", userId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return routines.size();
    }

    public static class InProgressViewHolder extends RecyclerView.ViewHolder {
        TextView routineName, routineDescription, duration, difficulty, exerciseCount;

        public InProgressViewHolder(@NonNull View itemView) {
            super(itemView);
            routineName = itemView.findViewById(R.id.routineName);
            routineDescription = itemView.findViewById(R.id.routineDescription);
            duration = itemView.findViewById(R.id.duration);
            difficulty = itemView.findViewById(R.id.difficulty);
            exerciseCount = itemView.findViewById(R.id.exerciseCount);
        }
    }
}