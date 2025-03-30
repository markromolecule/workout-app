package com.livado.workout.utils;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewItemSpacing extends RecyclerView.ItemDecoration {
    private final int spaceHeight;

    public RecyclerViewItemSpacing(int spaceHeight) {
        this.spaceHeight = spaceHeight;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.bottom = spaceHeight; // Add spacing below each item
    }
}
