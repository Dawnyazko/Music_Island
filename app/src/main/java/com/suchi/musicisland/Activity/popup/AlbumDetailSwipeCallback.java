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
import android.view.animation.DecelerateInterpolator;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.Adapter.SongAdapter;
import com.suchi.musicisland.MusicDBHelper;
import com.suchi.musicisland.R;
import com.suchi.musicisland.Song;
import com.suchi.musicisland.Song_Table;
import com.suchi.musicisland.Utils.ExoPlayerManager;
import com.suchi.musicisland.Utils.Util;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.List;

public class AlbumDetailSwipeCallback extends ItemTouchHelper.SimpleCallback {

    private final Context context;
    private final ExoPlayerManager exoPlayManger;
    private SongAdapter songAdapter;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bgRect = new RectF();
    private final List<Song> songList;
    private boolean hasTriggered = false;

    // 阻尼相关参数
    private static final float DAMP_THRESHOLD = 0.4f; // 开始阻尼
    private static final float MAX_SWIPE_RATIO = 1.0f; // 最大滑动距离比例

    // 回弹动画相关
    private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator(2f);
    private static final int ANIMATION_DURATION = 300; // 动画时长
    private long animationStartTime = 0;
    private float animationStartDx = 0;
    private boolean isAnimatingBack = false;
    private boolean isDeleteSwipe = false;

    //震动相关
    private boolean hasTriggeredVibration = false;

    public AlbumDetailSwipeCallback(
            Context context,
            List<Song> songList,
            ExoPlayerManager exoPlayManger,
            SongAdapter songAdapter
    ) {
        super(0, ItemTouchHelper.RIGHT); // | ItemTouchHelper.LEFT
        this.context = context.getApplicationContext();
        this.songList = songList;
        this.exoPlayManger = exoPlayManger;
        this.songAdapter = songAdapter;
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
        Song song = songList.get(position);

        MusicDBHelper.deleteSingleSong(song);

        //从UI删除
        songAdapter.removeAt(position);
    }

    @Override
    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
        if (isDeleteSwipe) {
            return 0.5f; //触发删除动画
        } else {
            return 2f; // 永远达不到
        }
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return Float.MAX_VALUE;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        // 降低速度阈值
        return defaultValue * 0.5f;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        // 重写此方法以控制滑动行为
        if (viewHolder instanceof SongAdapter.SongViewHolder) {
            return makeMovementFlags(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT);
        } else {
            return makeMovementFlags(0, 0); // 禁止滑动
        }
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
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX > 0) {

            handleRightSwipe(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    isCurrentlyActive
            );

            return; //阻断 ItemTouchHelper
        }
        //删除滑动时的阴影
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            viewHolder.itemView.setElevation(0);
        }

        View itemView = viewHolder.itemView;

        if (isAnimatingBack && isCurrentlyActive) {
            // 用户重新触摸，取消回弹
            isAnimatingBack = false;
        }

        if (isCurrentlyActive) {
            isDeleteSwipe = dX < 0; //左滑 = 删除意图
        }

        if (isAnimatingBack && !isCurrentlyActive) {
            // 执行回弹动画
            long elapsed = System.currentTimeMillis() - animationStartTime;
            float progress = Math.min(1f, elapsed / (float) ANIMATION_DURATION);
            float interp = decelerateInterpolator.getInterpolation(progress);

            dX = animationStartDx * (1 - interp);

            if (progress >= 1f) {
                isAnimatingBack = false;
            }

            recyclerView.invalidate();

        } else if (!isCurrentlyActive && dX > 0 && !isAnimatingBack) {
            // 只启动一次回弹
            startBackAnimation(dX);
        }

        //滑动删除判断
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

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void handleRightSwipe(
            Canvas c,
            RecyclerView recyclerView,
            RecyclerView.ViewHolder viewHolder,
            float dX,
            float dY,
            boolean isCurrentlyActive
    ) {
        //删除滑动时的阴影
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            viewHolder.itemView.setElevation(0);
        }
        
        View itemView = viewHolder.itemView;
        float maxSwipe = itemView.getWidth() * MAX_SWIPE_RATIO;
        float threshold = itemView.getWidth() * DAMP_THRESHOLD;

        float realDx = dX;
        if (realDx > threshold) {
            float over = realDx - threshold;
            float damp = 1f - (over / (maxSwipe - threshold)) * 0.7f;
            realDx = threshold + over * damp;
        }
        realDx = Math.min(realDx, maxSwipe);

        //关键：只移动 View
        itemView.setTranslationX(realDx);

        float progress = Math.min(1f, realDx / itemView.getWidth());
        drawAddBackground(c, itemView, realDx, progress);

        //震动判断
        if (progress >= 0.5f && !hasTriggeredVibration) {
            itemView.performHapticFeedback(LONG_PRESS);
            hasTriggeredVibration = true;
        } else if (progress < 0.5f) {
            hasTriggeredVibration = false;
        }

        if (isCurrentlyActive) {
            hasTriggered = false;
        }

        if (!isCurrentlyActive && dX > 0 && !hasTriggered) {
            if (progress >= 0.5f) {
                int pos = viewHolder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    exoPlayManger.insertNext(songList.get(pos));
                    Util.ToastUtil.showShortToast(context, "下个播放", 600);
                    hasTriggered = true;
                }
            }
        }
    }


    private void startBackAnimation(float startDx) {
        if (startDx > 0) {
            isAnimatingBack = true;
            animationStartTime = System.currentTimeMillis();
            animationStartDx = startDx;
        }
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        // 重置状态
        isAnimatingBack = false;
    }

    private void drawAddBackground(
            Canvas c,
            View itemView,
            float dX,
            float progress
    ) {
        if (dX <= 0) return; // 只处理左 → 右滑

        bgPaint.setColor(Color.parseColor("#857dff"));
        float radius = dpToPx(context, 40);

        // 背景：从 item 左边开始，宽度 = dX
        bgRect.set(
                itemView.getLeft(),
                itemView.getTop(),
                itemView.getLeft() + dX,
                itemView.getBottom()
        );

        c.drawRoundRect(bgRect, radius, radius, bgPaint);

        drawAddIcon(c, itemView, dX, progress);
    }


    private void drawAddIcon(
            Canvas c,
            View itemView,
            float dX,
            float progress
    ) {
        if (dX <= 0) return;

        Drawable icon = ContextCompat.getDrawable(
                itemView.getContext(),
                R.drawable.ic_playlist_add
        );
        if (icon == null) return;

        int iconSize = dpToPx(context, 35);

        // 背景中心点
        int centerX = (int) (itemView.getLeft() + dX / 2);
        int centerY = itemView.getTop() + itemView.getHeight() / 2;

        icon.setBounds(
                centerX - iconSize / 2,
                centerY - iconSize / 2,
                centerX + iconSize / 2,
                centerY + iconSize / 2
        );

        //更早显现+非线性显现（加速出现）
        float alphaProgress = Math.min(1f, progress / 0.7f) * Math.min(1f, progress / 0.7f);

        icon.setAlpha((int) (255 * alphaProgress));
        icon.draw(c);
    }

    //画红色背景+图标
    private void drawDeleteBackground(
            Canvas c,
            View itemView,
            float dX,
            float progress
    ) {
        bgPaint.setColor(Color.parseColor("#FF3B30")); // Apple 红

        if (dX >= 0) return;

        float radius = dpToPx(context,40);

        if (dX < 0) { // 左滑
            bgRect.set(
                    itemView.getRight() + dX,
                    itemView.getTop(),
                    itemView.getRight(),
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
        Drawable icon = ContextCompat.getDrawable(context, R.drawable.ic_delete_forever);
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

        //更早显现 + 非线性显现（加速出现）
        float alphaProgress = Math.min(1f, progress / 0.7f) * Math.min(1f, progress / 0.7f);

        icon.setAlpha((int) (255 * alphaProgress));

        icon.draw(c);
    }

}