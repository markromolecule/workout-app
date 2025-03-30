package com.livado.workout.ui.main.workout.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.livado.workout.R;
import com.livado.workout.data.remote.response.RoutineResponse;
import com.livado.workout.ui.main.workout.WorkoutListActivity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder> {

    private final List<RoutineResponse> routines;
    private final int userId;
    private final Context context;
    private final RoutineClickListener clickListener;
    private Set<String> selectedRoutines;
    private final Set<String> inProgressRoutines;

    public interface RoutineClickListener {
        void onRoutineClick(int routineID);
    }

    public RoutineAdapter(List<RoutineResponse> routines, int userId, Context context,
                          RoutineClickListener clickListener,
                          Set<String> inProgressRoutines,
                          Set<String> selectedRoutines) {
        this.routines = routines;
        this.userId = userId;
        this.context = context;
        this.clickListener = clickListener;
        this.inProgressRoutines = inProgressRoutines;
        this.selectedRoutines = selectedRoutines;
    }

    @NonNull
    @Override
    public RoutineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.routine_item, parent, false);
        return new RoutineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoutineViewHolder holder, int position) {
        RoutineResponse routine = routines.get(position);
        String routineID = String.valueOf(routine.getRoutineID());

        holder.routineName.setText(routine.getRoutineName());

        if (routine.getRoutineDescription() != null && !routine.getRoutineDescription().isEmpty()) {
            holder.routineDescription.setText(routine.getRoutineDescription());
        } else {
            holder.routineDescription.setText("No description available"); // Handle missing descriptions
        }

        if (routine.getDifficulty() != null) {
            holder.difficulty.setText("Difficulty: " + routine.getDifficulty());
        } else {
            holder.difficulty.setText("Difficulty: Unknown");
        }

        holder.duration.setText("Duration: " + routine.getDuration() + " min");

        holder.exerciseCount.setText("Exercises: " + routine.getExerciseCount());

        boolean isSelected = selectedRoutines.contains(routineID);
        boolean isInProgress = inProgressRoutines.contains(routineID);

        if (isInProgress) {
            holder.itemView.setAlpha(0.5f);
            holder.itemView.setEnabled(false);
        } else {
            holder.itemView.setAlpha(1.0f);
            holder.itemView.setEnabled(true);
            holder.itemView.setBackgroundResource(isSelected ?
                    R.drawable.selected_routine_background :
                    R.drawable.default_routine_background);

            // Handle routine selection/deselection
            holder.itemView.setOnClickListener(v -> {
                if (isSelected) {
                    // Deselect the routine if already selected
                    selectedRoutines.remove(routineID);
                    Log.d("RoutineAdapter", "Routine deselected: " + routineID);
                    notifyDataSetChanged();  // Refresh RecyclerView
                } else {
                    // Select the routine and open the WorkoutListActivity
                    selectedRoutines.add(routineID);
                    Log.d("RoutineAdapter", "Routine selected: " + routineID);

                    // Pass the userId and routineID to WorkoutListActivity
                    Intent intent = new Intent(context, WorkoutListActivity.class);
                    intent.putExtra("userId", userId);  // Pass the userId
                    intent.putExtra("routineID", routine.getRoutineID());  // Pass the selected routine ID
                    context.startActivity(intent);
                }
            });
        }
    }

    public void updateSelectedRoutines(Set<String> selectedRoutines) {
        this.selectedRoutines = selectedRoutines;
        notifyDataSetChanged(); // Notify the adapter to refresh the list
    }

    public void updateInProgressRoutines(Set<String> newInProgressRoutines) {
        this.inProgressRoutines.clear();
        this.inProgressRoutines.addAll(newInProgressRoutines);
        notifyDataSetChanged(); // Refresh RecyclerView
    }

    @Override
    public int getItemCount() {
        return routines.size();
    }

    public static class RoutineViewHolder extends RecyclerView.ViewHolder {
        TextView routineName, routineDescription, duration, difficulty, exerciseCount;

        public RoutineViewHolder(@NonNull View itemView) {
            super(itemView);
            routineName = itemView.findViewById(R.id.routineName);
            routineDescription = itemView.findViewById(R.id.routineDescription);
            duration = itemView.findViewById(R.id.duration);
            difficulty = itemView.findViewById(R.id.difficulty);
            exerciseCount = itemView.findViewById(R.id.exerciseCount);
        }
    }
}
