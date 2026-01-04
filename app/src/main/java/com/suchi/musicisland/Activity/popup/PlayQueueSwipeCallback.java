package com.suchi.musicisland.Activity.popup;

import static android.view.HapticFeedbackConstants.LONG_PRESS;
import static com.suchi.musicisland.Utils.Util.dpToPx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.Adapter.PlayQueueAdapter;
import com.suchi.musicisland.R;
import com.suchi.musicisland.Utils.ExoPlayerManager.QueueItem;
import com.suchi.musicisland.Utils.ExoPlayerManager;

import java.util.List;

public class PlayQueueSwipeCallback extends ItemTouchHelper.SimpleCallback {
    private final Context context;
    private final PlayQueueAdapter playQueueAdapter;
    private final ExoPlayerManager exoPlayManager;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bgRect = new RectF();

    //震动相关
    private boolean hasTriggeredVibration = false;

    public PlayQueueSwipeCallback(
            PlayQueueAdapter playQueueAdapter,
            ExoPlayerManager exoPlayManager,
            Context context
    ) {
        super(0, ItemTouchHelper.LEFT); // | ItemTouchHelper.RIGHT
        this.playQueueAdapter = playQueueAdapter;
        this.exoPlayManager = exoPlayManager;
        this.context = context.getApplicationContext();
    }

    @Override
    public boolean onMove(
            RecyclerView recyclerView,
            RecyclerView.ViewHolder viewHolder,
            RecyclerView.ViewHolder target
    ) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) return;
        List<QueueItem> queueItems = exoPlayManager.getNextlayList();
        QueueItem item = queueItems.get(position);

        //真正从列表中删除
        exoPlayManager.removeFromList(item.mediaIndex);

        //从UI处删除
        playQueueAdapter.removeAt(position);
    }

    @Override
    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
        return 0.5f; // 滑到一半才删除
    }

    @Override
    public void onChildDraw(
            @NonNull Canvas c,
            @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            float dX,
            float dY,
            int actionState,
            boolean isCurrentlyActive
    ) {

        View itemView = viewHolder.itemView;

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

            float progress = Math.min(1f, Math.abs(dX) / itemView.getWidth());

            drawDeleteBackground(c, itemView, dX, progress);

            //震动判断
            if (progress >= 0.5f && !hasTriggeredVibration) {
                itemView.performHapticFeedback(LONG_PRESS);
                hasTriggeredVibration = true;
            } else if (progress < 0.5f) {
                hasTriggeredVibration = false;
            }
        }

        super.onChildDraw(
                c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
        );
    }

    //画红色背景+图标
    private void drawDeleteBackground(
            Canvas c,
            View itemView,
            float dX,
            float progress
    ) {
        bgPaint.setColor(Color.parseColor("#FF3B30")); // Apple 红

        float radius = dpToPx(context,40);

        if (dX < 0) { // 左滑
            bgRect.set(
                    itemView.getRight() + dX,
                    itemView.getTop(),
                    itemView.getRight(),
                    itemView.getBottom()
            );
        } else { // 右滑（如果你允许）
            bgRect.set(
                    itemView.getLeft(),
                    itemView.getTop(),
                    itemView.getLeft() + dX,
                    itemView.getBottom()
            );
        }

        c.drawRoundRect(bgRect, radius, radius, bgPaint);

        drawDeleteIcon(c, itemView, dX, progress);
    }

    //画删除图标
    private void drawDeleteIcon(
            Canvas c,
            View itemView,
            float dX,
            float progress
    ) {
        if (dX >= 0) return; // 只处理左滑

        Context context = itemView.getContext();
        Drawable icon = ContextCompat.getDrawable(context, R.drawable.ic_playlist_remove);
        if (icon == null) return;

        int iconSize = dpToPx(context, 35);

        // 红色背景宽度
        float bgWidth = Math.abs(dX);

        // 背景区域
        float bgLeft = itemView.getRight() - bgWidth;
        float bgRight = itemView.getRight();

        // 图标居中
        int centerX = (int) ((bgLeft + bgRight) / 2);
        int centerY = itemView.getTop() + itemView.getHeight() / 2;

        int left = centerX - iconSize / 2;
        int top = centerY - iconSize / 2;
        int right = centerX + iconSize / 2;
        int bottom = centerY + iconSize / 2;

        icon.setBounds(left, top, right, bottom);

        //更早显现+非线性显现（加速出现）
        float alphaProgress = Math.min(1f, progress / 0.7f) * Math.min(1f, progress / 0.7f);

        icon.setAlpha((int) (255 * alphaProgress));

        icon.draw(c);
    }

}
