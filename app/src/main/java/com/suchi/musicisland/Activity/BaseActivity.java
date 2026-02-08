package com.suchi.musicisland.Activity;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.Listener.SongPlayChangeNotifier;
import com.suchi.musicisland.Utils.ExoPlayerManager;
import com.suchi.musicisland.R;

public class BaseActivity extends AppCompatActivity {
    private View musicBar;
    private ConstraintLayout playerBarRoot;
    protected ScrollToTopProvider scrollToTopProvider;
    private ImageButton toTopButton;
    private ImageButton skipNextBtn;
    private ConstraintLayout playerBarButton;
    private ImageButton playButton;
    private ImageButton searchButton;
    private TextView currentSongName;
    private TextView getCurrentSongArtist;
    private ExoPlayerManager exoPlayManager;

    protected int PLAYER_BAR_NORMAL_WIDTH;  // 原始宽度
    protected int PLAYER_BAR_EXPANDED_WIDTH; // 扩张覆盖 toTop
    private boolean isExpanded = false;
    private float toTopOriginalElevation = -1f;


    private final SongPlayChangeNotifier.OnSongPlayerChangedListener playerStateListener =
            () -> loadCurrentSongToBar();

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        super.setContentView(R.layout.base_activity);
        initMusicBar();

        setOnClickListener();

        SongPlayChangeNotifier.getInstance().addListener(playerStateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentSongToBar();

        if (!exoPlayManager.isPlaying()) {
            playButton.setImageResource(R.drawable.play_arrow_icon);
        } else {
            playButton.setImageResource(R.drawable.pause_icon);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SongPlayChangeNotifier.getInstance().removeListener(playerStateListener);
    }

    private void initMusicBar() {
        toTopButton = findViewById(R.id.icon_toTop);
        playerBarButton = findViewById(R.id.playerBar);
        currentSongName = findViewById(R.id.song_name_bar);
        getCurrentSongArtist = findViewById(R.id.song_artist_bar);
        playButton = findViewById(R.id.pause_bar);
        searchButton = findViewById(R.id.icon_search);
        skipNextBtn = findViewById(R.id.skip_next_bar);
        playerBarRoot = findViewById(R.id.musicBar_base);
        exoPlayManager = ExoPlayerManager.getInstance();

        musicBar = findViewById(R.id.musicBar_base);
        if (musicBar != null) {
            musicBar.setVisibility(View.VISIBLE);
        }

        //设置扩张的宽度和正常的宽度
        PLAYER_BAR_NORMAL_WIDTH = (int) (240 * getResources().getDisplayMetrics().density);//将dp转换为px
        PLAYER_BAR_EXPANDED_WIDTH = (int) (310 * getResources().getDisplayMetrics().density);//将dp转换为px
    }

    @Override
    public void setContentView(int layoutResID) {
        FrameLayout container = findViewById(R.id.container_base);
        if (container != null) {
            View view = LayoutInflater.from(this).inflate(layoutResID, container, false);
            container.addView(view);
        }
    }

    public interface ScrollToTopProvider {
        void scrollToTop();
    }

    public void setScrollToTopProvider(ScrollToTopProvider provider) {
        this.scrollToTopProvider = provider;
    }

    private void setOnClickListener() {

        toTopButton.setOnClickListener(v -> {
                if (scrollToTopProvider != null) scrollToTopProvider.scrollToTop();

                //点击后立即执行PlayerBar动画
                animatePlayerBarWidthWithConstraint(PLAYER_BAR_EXPANDED_WIDTH);
                disableToTopShadow();
        });

        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(BaseActivity.this, SearchActivity.class);
            startActivity(intent);
        });

        playerBarButton.setOnClickListener(v -> {
            playButton.postDelayed(() -> {
                Intent intent = new Intent(BaseActivity.this, PlayerActivity.class);
                startActivity(intent);
            }, 50);
        });

        playButton.setOnClickListener(v -> {
            SongPlayChangeNotifier.getInstance().notifyUIChanged();
            if (!exoPlayManager.isPlaying()) {
                exoPlayManager.resume();
                //播放动画
                playButton.animate().scaleX(0.75f).scaleY(0.75f).setDuration(80)
                        .withEndAction(() ->
                                playButton.animate().scaleX(1f).scaleY(1f).setDuration(80));
                if (exoPlayManager.isPlaying()) playButton.setImageResource(R.drawable.pause_icon);
            } else {
                exoPlayManager.pause();
                //暂停动画
                playButton.animate().scaleX(0.75f).scaleY(0.75f).setDuration(80)
                        .withEndAction(() ->
                                playButton.animate().scaleX(1f).scaleY(1f).setDuration(80));

                playButton.setImageResource(R.drawable.play_arrow_icon);
            }
        });

        skipNextBtn.setOnClickListener(v -> {
            //动画
            skipNextBtn.animate().scaleX(0.75f).scaleY(0.75f).setDuration(80)
                    .withEndAction(() -> skipNextBtn.animate().scaleX(1f).scaleY(1f).setDuration(80)
                            .withEndAction(() ->{
                                //切换下一首
                                exoPlayManager.skipNext();
                            })
                    );
        });
    }

    private void loadCurrentSongToBar() {
        if (exoPlayManager.getCurrentSong() == null) return;

        String songName = exoPlayManager.getCurrentSongName();
        String songArtist = exoPlayManager.getCurrentSongArtist();

        //播放状态改变时改变按钮状态
        if (exoPlayManager.isCompleted() || !exoPlayManager.isPlaying()) {
            playButton.setImageResource(R.drawable.play_arrow_icon);
        } else {
            playButton.setImageResource(R.drawable.pause_icon);
        }

        if (songName == null || songName.isEmpty()) {
            return;
        }

        currentSongName.setText(songName);
        getCurrentSongArtist.setText(songArtist);
    }


    /////////////////处理PlayerBar的动画方法//////////////////
    private void animatePlayerBarWidthWithConstraint(int targetWidthPx) {
        ConstraintSet set = new ConstraintSet();
        set.clone(playerBarRoot);

        set.constrainWidth(R.id.playerBar, targetWidthPx);
        set.connect(
                R.id.playerBar,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END
        );

        TransitionManager.beginDelayedTransition(playerBarRoot);
        set.applyTo(playerBarRoot);

        currentSongName.setEllipsize(TextUtils.TruncateAt.END);
    }


    protected void onActivityScrolled(int scrollY) {
        //控制开始滑动多少后执行动画
        int expand = (int) (120 * getResources().getDisplayMetrics().density);//将dp转换为px
        int collapse = (int) (600 * getResources().getDisplayMetrics().density);

        if (scrollY < expand && !isExpanded) {
            // 向左延展
            animatePlayerBarWidthWithConstraint(PLAYER_BAR_EXPANDED_WIDTH);
            disableToTopShadow();
            isExpanded = true;

        } else if (scrollY > collapse && isExpanded) {
            // 恢复原长度
            animatePlayerBarWidthWithConstraint(PLAYER_BAR_NORMAL_WIDTH);

            //渐渐恢复按钮阴影
            animateToTopElevation(0f, 16);

            isExpanded = false;
        }
    }

    private void disableToTopShadow() {
        View toTop = findViewById(R.id.toTop);
        if (toTop == null) return;

        toTopOriginalElevation = toTop.getElevation();

        animateToTopElevation(toTopOriginalElevation, 0f);
    }

    protected void attachScrollListener(View scrollable) {
        if (scrollable instanceof RecyclerView) {
            ((RecyclerView) scrollable).addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                    onActivityScrolled(rv.computeVerticalScrollOffset());
                }
            });
        } else if (scrollable instanceof NestedScrollView) {
            ((NestedScrollView) scrollable).setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                onActivityScrolled(scrollY);
            });
        }
    }

    private void animateToTopElevation(float from, float to) {
        View toTop = findViewById(R.id.toTop);
        if (toTop == null) return;

        ValueAnimator animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(360);
        animator.setInterpolator(new DecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            toTop.setElevation(value);
        });

        animator.start();
    }
}
