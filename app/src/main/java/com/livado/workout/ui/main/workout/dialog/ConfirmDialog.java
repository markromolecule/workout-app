package com.livado.workout.ui.main.workout.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class ConfirmDialog extends DialogFragment {

    private int selectedRoutineID;
    private ConfirmDialogListener listener;

    /** ✅ Define a callback interface */
    public interface ConfirmDialogListener {
        void onRoutineConfirmed(int routineID);
    }

    /** ✅ Attach listener to parent activity */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ConfirmDialogListener) {
            listener = (ConfirmDialogListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement ConfirmDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            selectedRoutineID = getArguments().getInt("selectedRoutineID", -1);
        }

        return new AlertDialog.Builder(requireActivity())
                .setTitle("Confirm Routine")
                .setMessage("Are you sure you want to start this routine?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (listener != null) {
                        listener.onRoutineConfirmed(selectedRoutineID);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create();
    }
}