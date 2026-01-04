package com.suchi.musicisland.UI;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class SongDividerDecoration extends RecyclerView.ItemDecoration {
    private final Drawable divider;

    public SongDividerDecoration(Drawable divider) {
        this.divider = divider;
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();

        for (int i = 0; i < childCount - 1; i++ ) {
            View child = parent.getChildAt(i);

            int position = parent.getChildAdapterPosition(child);

            //跳过第一第二项
            if (position == 0) continue;
            if (position == 1) continue;

            //跳过最后一项
            if (position == state.getItemCount() - 1) continue;

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + divider.getIntrinsicHeight();

            divider.setBounds(left + 40, top, right - 40, bottom);
            divider.draw(canvas);
        }
    }
}
