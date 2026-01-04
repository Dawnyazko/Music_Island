package com.suchi.musicisland.Activity;

import static com.suchi.musicisland.Utils.Util.dpToPx;
import static com.suchi.musicisland.Utils.Util.formatTime;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.Activity.popup.PlayQueueSwipeCallback;
import com.suchi.musicisland.Adapter.HeaderPlayListAdapter;
import com.suchi.musicisland.Adapter.PlayQueueAdapter;
import com.suchi.musicisland.Executor.AppExecutors;
import com.suchi.musicisland.Listener.SongLikeChangeNotifier;
import com.suchi.musicisland.Listener.SongPlayChangeNotifier;
import com.suchi.musicisland.Store.SongStateStore;
import com.suchi.musicisland.Utils.ExoPlayerManager;
import com.suchi.musicisland.Utils.ExoPlayerManager.QueueItem;
import com.suchi.musicisland.R;
import com.suchi.musicisland.Song;
import com.suchi.musicisland.Song_Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.List;

public class PlayerActivity extends AppCompatActivity {
    private ImageButton likeButton;
    private ImageButton skipPreviousBtn;
    private ImageButton playOrderBtn;
    private ImageButton skipNextBtn;
    private ConstraintLayout playerPanel;
    private float startY;
    private float currentY;
    private float screenHeight;
    private boolean dragging;
    private View dimBackground;
    private ImageButton playButton;
    private ImageView currentSongCover;
    private TextView currentSongName;
    private TextView getCurrentSongArtist;
    private TextView timeTextView;
    private PopupWindow playQueuePopup;
    private RecyclerView recyclerPlayList;
    private PlayQueueAdapter playQueueAdapter;
    private HeaderPlayListAdapter headerPlayListAdapter;
    private ConcatAdapter concatAdapter;
    private PlayQueueSwipeCallback playQueueSwipeCallback;
    private ItemTouchHelper swipeHelper;

    private SeekBar seekBar;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Handler skipHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private ExoPlayerManager exoPlayManager;
    private boolean userSeeking = false;
    private boolean isDragging = false;
    private float dragProgressOffset = 0f;
    private float lastX = 0f;
    private float draggingProgress = 0f;
    private int lastKnownDuration = 0; // 保存最后已知的时长，避免闪烁
    private boolean isSongCompleted = false; // 跟踪歌曲是否完成

    private final SongPlayChangeNotifier.OnSongPlayerChangedListener playerStateListener =
            () -> loadCurrentSongsToUI();


    private void initializeView() {
        playerPanel = findViewById(R.id.playerPanel);
        dimBackground = findViewById(R.id.dimBackground);
        playButton = findViewById(R.id.icon_play_player);
        currentSongCover = findViewById(R.id.imgCover_detail_player);
        currentSongName = findViewById(R.id.song_name_player);
        getCurrentSongArtist = findViewById(R.id.song_artist_player);
        likeButton = findViewById(R.id.icon_like_player);
        skipPreviousBtn = findViewById(R.id.icon_skip_previous_player);
        seekBar = findViewById(R.id.seekBar);
        timeTextView = findViewById(R.id.time_player);
        skipNextBtn = findViewById(R.id.icon_skip_next_player);
        playOrderBtn = findViewById(R.id.icon_playOrder_player);
        skipNextBtn = findViewById(R.id.icon_skip_next_player);

        exoPlayManager = ExoPlayerManager.getInstance();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);

        //保证状态栏可读性
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initializeView();

        SongPlayChangeNotifier.getInstance().addListener(playerStateListener);

        startProgressUpdater();

        //禁用系统动画
        overridePendingTransition(0,0);

        setupOnClickListener();

        touchToOpenAnimation();
        slideToClose();
        setupBackGesture();
        loadCurrentSongsToUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncSeekBarMax();
        updateProgressUI(exoPlayManager.getCurrentPosition());
        updatePlayModeIcon(exoPlayManager.getPlayMode());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacksAndMessages(null);
        SongPlayChangeNotifier.getInstance().removeListener(playerStateListener);
    }

    private void setupOnClickListener() {
        if (!exoPlayManager.isPlaying()) {
            playButton.setImageResource(R.drawable.play_arrow_icon);
        }
        playButton.setOnClickListener(v -> {
            if (!exoPlayManager.isPlaying()) {
                exoPlayManager.resume();
                //播放动画
                playButton.animate().scaleX(0.75f).scaleY(0.75f).setDuration(80)
                        .withEndAction(() ->
                                playButton.animate().scaleX(1f).scaleY(1f).setDuration(80));

                if (!isSongCompleted) playButton.setImageResource(R.drawable.pause_icon);
            } else {
                exoPlayManager.pause();
                //暂停动画
                playButton.animate().scaleX(0.75f).scaleY(0.75f).setDuration(80)
                        .withEndAction(() -> playButton.animate().scaleX(1f).scaleY(1f).setDuration(80));

                playButton.setImageResource(R.drawable.play_arrow_icon);
            }
        });
        likeButton.setOnClickListener(v -> {
            if (exoPlayManager.isPlaying()){
                Song currentSong = exoPlayManager.getCurrentSong();
                boolean isNowLiked = false;

                if (SongStateStore.isLiked(currentSong.getId()) != true) {
                    SongStateStore.setLiked(currentSong.getId(), true);
                    currentSong.markAsLiked(true);
                    isNowLiked = true;

                    AppExecutors.DB.execute(() -> {
                        currentSong.update();
                    });

                    likeButton.animate().scaleX(0.75f).scaleY(0.75f).setDuration(80)
                            .withEndAction(() -> likeButton.animate().scaleX(1f).scaleY(1f).setDuration(80));

                    likeButton.setImageResource(R.drawable.solid_favorite_icon);
                } else {
                    SongStateStore.removeLiked(currentSong.getId());
                    currentSong.markAsDisliked(false);
                    isNowLiked = false;

                    AppExecutors.DB.execute(() -> {
                        currentSong.update();
                    });

                    likeButton.animate().scaleX(0.75f).scaleY(0.75f).setDuration(80)
                            .withEndAction(() -> likeButton.animate().scaleX(1f).scaleY(1f).setDuration(80));

                    likeButton.setImageResource(R.drawable.hollow_favorite_icon);
                }

                //通知activityUI状态改变
                SongLikeChangeNotifier.getInstance().notifySongLikeChanged(currentSong.getId(), isNowLiked);
            }
        });

        playOrderBtn.setOnClickListener(v -> {
            //动画
            playOrderBtn.animate().scaleX(0.75f).scaleY(0.75f).setDuration(80)
                    .withEndAction(() -> playOrderBtn.animate().scaleX(1f).scaleY(1f).setDuration(80)
                            .withEndAction(() -> {
                                ExoPlayerManager.PlayMode mode = exoPlayManager.switchPlayMode();
                                updatePlayModeIcon(mode);
                            })
                    );
        });

        playOrderBtn.setOnLongClickListener(v -> {
            showPlayQueuePopup(v);
            return true;
        });

        skipNextBtn.setOnClickListener(v -> {
            //动画
            skipNextBtn.animate().scaleX(0.75f).scaleY(0.75f).setDuration(80)
                    .withEndAction(() -> skipNextBtn.animate().scaleX(1f).scaleY(1f).setDuration(80)
                            .withEndAction(() -> {
                                    // 先暂停进度更新
                                    userSeeking = true;
                                    // 重置进度到0
                                    updateProgressUI(0);

                                    //下一首
                                    exoPlayManager.skipNext();

                                    // 延迟一小段时间后恢复进度更新
                                    skipHandler.postDelayed(() -> {
                                        userSeeking = false;

                                        // 确保进度条正确显示
                                        int duration = exoPlayManager.getDuration();
                                        if (duration > 0) {
                                            seekBar.setMax(duration);
                                        }
                                        updateProgressUI(0);

                                        // 重新启动进度更新器
                                        stopProgressUpdater();
                                        startProgressUpdater();
                                    }, 100);
                            })
                    );
        });

        skipPreviousBtn.setOnClickListener(v -> {
            //动画
            skipPreviousBtn.animate().scaleX(0.75f).scaleY(0.75f).setDuration(80)
                    .withEndAction(() -> skipPreviousBtn.animate().scaleX(1f).scaleY(1f).setDuration(80)
                            .withEndAction(() -> {
                                // 先暂停进度更新
                                userSeeking = true;

                                // 重置进度到0
                                updateProgressUI(0);

                                exoPlayManager.skipPrevious();

                                // 延迟一小段时间后恢复进度更新
                                skipHandler.postDelayed(() -> {
                                    userSeeking = false;

                                    // 确保进度条正确显示
                                    int duration = exoPlayManager.getDuration();
                                    if (duration > 0) {
                                        seekBar.setMax(duration);
                                    }
                                    updateProgressUI(0);

                                    // 重新启动进度更新器
                                    stopProgressUpdater();
                                    startProgressUpdater();
                                }, 100);
                            })
                    );
        });

        seekBar.setOnTouchListener((v, event) -> {
            float x = event.getX();
            float width = seekBar.getWidth();
            x = Math.max(0, Math.min(x, width));

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = x;
                    draggingProgress = seekBar.getProgress();
                    dragProgressOffset = 0f;
                    isDragging = false;
                    userSeeking = true;

                    // 保存当前状态
                    isSongCompleted = exoPlayManager.isCompleted();
                    lastKnownDuration = seekBar.getMax(); // 保存当前最大值
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = x - lastX;

                    if (!isDragging) {
                        if (Math.abs(dx) < ViewConfiguration.get(v.getContext())
                                .getScaledTouchSlop()) {
                            return true;
                        }
                        isDragging = true;
                    }

                    // 使用最后已知的时长计算增量
                    float delta = dx / width * lastKnownDuration;
                    draggingProgress += delta;

                    // 确保进度在有效范围内
                    draggingProgress = Math.max(0f,
                            Math.min(draggingProgress, lastKnownDuration));

                    // 更新UI，但不改变进度条最大值
                    updateProgressUIWithoutChangingMax(Math.round(draggingProgress));

                    lastX = x;
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isDragging) {
                        int finalProgress = Math.round(draggingProgress);

                        // 如果歌曲已经播放完成
                        if (isSongCompleted) {
                            // 检查是否是拖动到末尾
                            if (finalProgress >= lastKnownDuration) {

                                //拖动到完成位置，继续下一首
                                exoPlayManager.skipNext();

                            } else {
                                // 拖动到中间位置，恢复播放
                                exoPlayManager.seekTo(finalProgress);

                                // 更新UI
                                updateProgressUI(finalProgress);

                                SongPlayChangeNotifier.getInstance().notifyUIChanged();

                                // 重新启动进度更新
                                isSongCompleted = false;
                            }
                        } else {
                            exoPlayManager.seekTo(finalProgress);
                            updateProgressUI(finalProgress);
                        }

                        // 延迟一小段时间后恢复进度更新
                        skipHandler.postDelayed(() -> {
                            userSeeking = false;
                            isDragging = false;
                            // 重新同步进度条最大值
                            syncSeekBarMax();
                        }, 50);
                    } else {
                        userSeeking = false;
                        isDragging = false;
                    }
                    return true;
            }

            return true;
        });
    }


    //////////进度条操作类///////////
    private void syncSeekBarMax() {
        int duration = exoPlayManager.getDuration();
        if (duration > 0 && duration != seekBar.getMax()) {
            seekBar.setMax(duration);
            lastKnownDuration = duration;
        }
    }


    //让进度条走起来
    private void startProgressUpdater() {
        stopProgressUpdater();

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                // 如果用户正在拖动，跳过更新
                if (userSeeking) {
                    progressHandler.postDelayed(this, 33);
                    return;
                }

                // 检查播放状态
                if (exoPlayManager.isCompleted()) {
                    // 播放完成时，显示总时长
                    int duration = exoPlayManager.getDuration();
                    if (duration > 0) {
                        // 如果最大值不同，先更新最大值
                        if (seekBar.getMax() != duration) {
                            seekBar.setMax(duration);
                            lastKnownDuration = duration;
                        }
                        updateProgressUI(duration);
                    }

                    if (exoPlayManager.getPlayMode() != ExoPlayerManager.PlayMode.REPEAT_ONE){
                        isSongCompleted = true;
                    }

                    // 继续监听，状态可能改变
                    progressHandler.postDelayed(this, 1000);
                    return;
                }

                // 获取当前进度
                int position = exoPlayManager.getCurrentPosition();
                int duration = exoPlayManager.getDuration();

                // 更新进度条最大值
                if (duration > 0 && duration != seekBar.getMax()) {
                    seekBar.setMax(duration);
                    lastKnownDuration = duration;
                }

                // 安全地更新UI
                if (duration > 0) {
                    // 确保进度不超过总时长
                    position = Math.min(position, duration);
                    updateProgressUI(position);
                } else {
                    // 如果无法获取时长，至少更新当前位置
                    updateProgressUI(position);
                }

                // 继续更新
                progressHandler.postDelayed(this, 33);
            }
        };

        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdater() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }



    /////////////UI操作类///////////////

    private void updatePlayModeIcon(ExoPlayerManager.PlayMode mode) {
        int resId;

        switch (mode) {
            case SEQUENCE:
                resId = R.drawable.playlist_play_icon;
                break;
            case REPEAT_ALL:
                resId = R.drawable.repeat_icon;
                break;
            case REPEAT_ONE:
                resId = R.drawable.repeat_one_icon;
                break;
            case SHUFFLE:
                resId = R.drawable.shuffle_icon;
                break;
            default:
                return;
        }
        playOrderBtn.setImageResource(resId);
    }

    private void updateProgressUI(int progressMs) {
        // 确保进度不超过最大值
        if (progressMs > seekBar.getMax()) {
            progressMs = seekBar.getMax();
        }
        seekBar.setProgress(progressMs);
        timeTextView.setText(formatTime(progressMs));
    }

    private void updateProgressUIWithoutChangingMax(int progressMs) {
        // 只设置进度，不改变最大值
        if (progressMs <= seekBar.getMax()) {
            seekBar.setProgress(progressMs);
        }
        timeTextView.setText(formatTime(progressMs));
    }

    private void setupBackGesture() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closePlayerWithAnimation();
            }
        });
    }

    private void setDimBackgroundWhenOpen() {
        dimBackground.setVisibility(View.VISIBLE);
        dimBackground.animate()
                .alpha(1f)
                .setDuration(200)
                .start();
    }

    private void loadCurrentSongsToUI() {
        Song currentSong = exoPlayManager.getCurrentSong();

        if (currentSong == null) {
            return;
        }

        //播放状态改变时改变按钮状态
        if (exoPlayManager.isCompleted() || !exoPlayManager.isPlaying()) {
            playButton.setImageResource(R.drawable.play_arrow_icon);
        } else {
            playButton.setImageResource(R.drawable.pause_icon);
        }

        //判断当前歌曲是否为喜欢
        if (SongStateStore.isLiked(currentSong.getId()) == true) {
            likeButton.setImageResource(R.drawable.solid_favorite_icon);
        } else {
            likeButton.setImageResource(R.drawable.hollow_favorite_icon);
        }

        String songArtist = exoPlayManager.getCurrentSongArtist();
        Uri songCover = Uri.parse(exoPlayManager.getCurrentSongCoverUri());

        currentSongName.setText(exoPlayManager.getCurrentSongName());
        getCurrentSongArtist.setText(songArtist);

        if (songCover != null) {
            currentSongCover.setImageURI(songCover);
        }

        // 初始化进度条
        int duration = exoPlayManager.getDuration();
        if (duration > 0) {
            seekBar.setMax(duration);
            lastKnownDuration = duration;
        }

        // 初始化当前进度
        int position = exoPlayManager.getCurrentPosition();
        updateProgressUI(position);
    }

     private void touchToOpenAnimation() {
        playerPanel.setTranslationY(getResources().getDisplayMetrics().heightPixels);

        playerPanel.post(() -> {
           playerPanel.animate()
                   .translationY(0)
                   .setDuration(550)
                   .setInterpolator(new DecelerateInterpolator(2.0f))
                   .start();
        });
     }

    private void slideToClose() {
        startY = 0;
        currentY = 0;
        dragging = false;

        screenHeight = getResources().getDisplayMetrics().heightPixels;

        playerPanel.requestDisallowInterceptTouchEvent(true);

        playerPanel.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startY = event.getRawY();
                    dragging = true;
                    setDimBackgroundWhenOpen();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (!dragging) return false;

                    currentY = event.getRawY();
                    float deltaY = currentY - startY;

                    //只允许向下滑
                    if (deltaY > 0) {
                        playerPanel.setTranslationY(deltaY);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:

                    dragging = false;

                    float totalDrag = currentY - startY;

                    //滑动超过1/5屏幕后关闭
                    if ((totalDrag > screenHeight / 5)) {
                        closePlayerWithAnimation();
                    } else {
                        resetPlayerPosition();
                    }
                    return true;
            }
            return false;
        });
    }

    private void resetPlayerPosition() {

        playerPanel.animate()
                .translationY(0)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void closePlayerWithAnimation() {

        playerPanel.animate()
                .translationY(screenHeight)
                .setDuration(200)
                .withEndAction(this::finish)
                .start();

        dimBackground.animate()
                .alpha(0f)
                .withEndAction(() -> dimBackground.setVisibility(View.GONE))
                .start();
    }


    ///////////弹出播放列表////////////
    private void showPlayQueuePopup(View anchor) {
        List<QueueItem> queueItems;

        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_play_queue, null);

        recyclerPlayList = popupView.findViewById(R.id.playQueueRecycler);

        recyclerPlayList.setLayoutManager(new LinearLayoutManager(this));

        queueItems = exoPlayManager.getNextlayList();

        //滑动手势
        playQueueSwipeCallback = new PlayQueueSwipeCallback(
                playQueueAdapter,
                exoPlayManager,
                recyclerPlayList.getContext()
        );
        swipeHelper = new ItemTouchHelper(playQueueSwipeCallback);
        swipeHelper.attachToRecyclerView(recyclerPlayList);

        playQueuePopup = new PopupWindow(
                popupView,
                dpToPx(this,365), //X
                dpToPx(this,500), //Y
                true
        );

        playQueuePopup.setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );
        playQueuePopup.setAnimationStyle(android.R.style.Animation_Dialog);
        //设置样式
        playQueuePopup.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_play_queue));
        playQueuePopup.setElevation(16f);
        //允许点击外部关闭
        playQueuePopup.setOutsideTouchable(true);
        //防止阴影被切掉
        playQueuePopup.setClippingEnabled(false);

        headerPlayListAdapter = new HeaderPlayListAdapter("接下来播放");
        playQueueAdapter = new PlayQueueAdapter(queueItems, new PlayQueueAdapter.OnSongQueueClickedListener() {
            @Override
            public void onSongClick(QueueItem item) {
                exoPlayManager.seekToQueuePosition(item.mediaIndex);

                new Handler().postDelayed(() -> {
                    playQueuePopup.dismiss();
                }, 300);
            }
        });

        concatAdapter = new ConcatAdapter(headerPlayListAdapter, playQueueAdapter);
        recyclerPlayList.setAdapter(concatAdapter);

        //在按钮左下角显示
        playQueuePopup.showAsDropDown(
                anchor,
                -dpToPx(this,40),
                -dpToPx(this,40)
        );
    }
}
