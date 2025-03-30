package com.livado.workout.ui.main.workout.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.livado.workout.R;
import com.livado.workout.data.remote.response.WorkoutResponse;
import com.livado.workout.ui.main.workout.dialog.UpdateDialog;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    private final List<WorkoutResponse> workouts;
    private final int userId;
    private final int routineId;
    private final Consumer<WorkoutResponse> onWorkoutClick;

    /* Constructor */
    public WorkoutAdapter(List<WorkoutResponse> workouts, int userId, int routineId, Consumer<WorkoutResponse> onWorkoutClick) {
        this.workouts = workouts;
        this.userId = userId;
        this.routineId = routineId;
        this.onWorkoutClick = onWorkoutClick;
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.workout_item, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        WorkoutResponse workout = workouts.get(position);

        Log.d("WorkoutAdapter", "Binding workout: " + workout.getWorkoutId() + ", name: " + workout.getWorkoutName());

        holder.workoutName.setText(workout.getWorkoutName());
        holder.sets.setText(String.format(Locale.getDefault(), "%d SETS", workout.getSets()));
        holder.reps.setText(String.format(Locale.getDefault(), "%d REPS", workout.getReps()));
        holder.duration.setText(String.format(Locale.getDefault(), "%d SECS", workout.getDuration()));

        Glide.with(holder.itemView.getContext())
                .load(workout.getWorkoutImagePath())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.workoutImage);

        holder.itemView.setOnClickListener(v -> {
            int selectedWorkoutId = workout.getWorkoutId();

            if (selectedWorkoutId <= 0) {
                Log.e("WorkoutAdapter", "Invalid workoutId clicked: " + selectedWorkoutId);
                Toast.makeText(holder.itemView.getContext(), "Invalid workout selection!", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("WorkoutAdapter", "Clicked on valid workoutId: " + selectedWorkoutId);

            UpdateDialog updateDialog = new UpdateDialog(holder.itemView.getContext(), userId, routineId, selectedWorkoutId, updatedWorkout -> {
                workouts.set(position, updatedWorkout);
                notifyItemChanged(position);
            });

            updateDialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return workouts.size();
    }

    public static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView workoutName, sets, reps, duration;
        ImageView workoutImage;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            workoutName = itemView.findViewById(R.id.workoutName);
            sets = itemView.findViewById(R.id.sets);
            reps = itemView.findViewById(R.id.reps);
            duration = itemView.findViewById(R.id.duration);
            workoutImage = itemView.findViewById(R.id.workoutImage);
        }
    }
}