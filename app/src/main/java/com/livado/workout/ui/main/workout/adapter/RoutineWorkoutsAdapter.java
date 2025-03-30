package com.livado.workout.ui.main.workout.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.livado.workout.R;
import com.livado.workout.data.remote.response.WorkoutResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoutineWorkoutsAdapter extends RecyclerView.Adapter<RoutineWorkoutsAdapter.WorkoutViewHolder> {

    private List<WorkoutResponse> workouts;
    private final Context context;
    private final OnWorkoutClickListener listener;
    private Set<String> completedWorkouts;

    public interface OnWorkoutClickListener {
        void onWorkoutClick(WorkoutResponse workout);
    }

    public RoutineWorkoutsAdapter(List<WorkoutResponse> workouts, Context context, OnWorkoutClickListener listener) {
        this.workouts = workouts;
        this.context = context;
        this.listener = listener;
        this.completedWorkouts = new HashSet<>();
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.workout_progress_item, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        WorkoutResponse workout = workouts.get(position);

        if (holder.routineName != null) {
            holder.routineName.setText(workout.getWorkoutName() != null ? workout.getWorkoutName() : "Workout Name Unavailable");
        }

        boolean isCompleted = completedWorkouts.contains(String.valueOf(workout.getWorkoutId()));

        holder.itemView.setAlpha(isCompleted ? 0.5f : 1.0f);
        holder.itemView.setEnabled(!isCompleted);

        if (holder.imageState != null) {
            holder.imageState.setVisibility(isCompleted ? View.VISIBLE : View.GONE);
        }

        if (!isCompleted) {
            holder.itemView.setOnClickListener(v -> listener.onWorkoutClick(workout));
        } else {
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return workouts != null ? workouts.size() : 0;
    }

    public void moveWorkoutToBottom(WorkoutResponse workout) {
        int oldPosition = workouts.indexOf(workout);
        if (oldPosition == -1) return;

        workouts.remove(oldPosition);
        workouts.add(workout);

        notifyItemMoved(oldPosition, workouts.size() - 1);
        notifyItemChanged(workouts.size() - 1);
    }

    public void updateCompletedWorkouts(WorkoutResponse completedWorkout) {
        for (int i = 0; i < workouts.size(); i++) {
            if (workouts.get(i).getWorkoutId() == completedWorkout.getWorkoutId()) {
                workouts.get(i).setCompleted(true);
                completedWorkouts.add(String.valueOf(completedWorkout.getWorkoutId()));

                notifyDataSetChanged();
                break;
            }
        }
    }

    /**
     * Updates the workouts list and refreshes the RecyclerView.
     */
    public void updateWorkouts(List<WorkoutResponse> updatedWorkouts) {
        this.workouts = updatedWorkouts;
        notifyDataSetChanged(); // Refresh RecyclerView
    }

    public static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView routineName;
        ImageView imageState;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);

            routineName = itemView.findViewById(R.id.routineName);
            imageState = itemView.findViewById(R.id.imageState);
        }
    }
}