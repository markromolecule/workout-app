package com.livado.workout.ui.main.workout.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.livado.workout.R;
import com.livado.workout.data.remote.response.WorkoutResponse;

import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    private final Context context;
    private final List<WorkoutResponse> workouts;
    private final String[] workoutCategories;

    public WorkoutAdapter(Context context, List<WorkoutResponse> workouts) {
        this.context = context;
        this.workouts = workouts;
        this.workoutCategories = context.getResources().getStringArray(R.array.workout_categories); // Load from resources
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.workout_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutResponse workout = workouts.get(position);

        holder.workoutName.setText(workout.getWorkoutName());

        // Populate Spinner with workout categories
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, workoutCategories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.workoutCategorySpinner.setAdapter(adapter);

        // Set selected category if available
        int categoryIndex = getCategoryIndex(workout.getWorkoutCategory());
        holder.workoutCategorySpinner.setSelection(categoryIndex);

        // Load image using Glide
        Glide.with(context)
                .load(workout.getWorkoutImagePath())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert)
                .into(holder.workoutImage);
    }

    @Override
    public int getItemCount() {
        return workouts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView workoutName;
        Spinner workoutCategorySpinner;
        ImageView workoutImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            workoutName = itemView.findViewById(R.id.workoutName);
            workoutCategorySpinner = itemView.findViewById(R.id.workoutCategory);
            workoutImage = itemView.findViewById(R.id.workoutImage);
        }
    }

    private int getCategoryIndex(String category) {
        for (int i = 0; i < workoutCategories.length; i++) {
            if (workoutCategories[i].equalsIgnoreCase(category)) {
                return i;
            }
        }
        return 0;
    }
}