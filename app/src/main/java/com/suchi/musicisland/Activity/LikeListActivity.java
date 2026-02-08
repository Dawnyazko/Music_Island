package com.suchi.musicisland.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.Adapter.FooterBlankAdapter;
import com.suchi.musicisland.Adapter.HeaderAdapter;
import com.suchi.musicisland.Adapter.SongAdapterAlbum;
import com.suchi.musicisland.Adapter.SongAdapterList;
import com.suchi.musicisland.Executor.AppExecutors;
import com.suchi.musicisland.Listener.SongLikeChangeNotifier;
import com.suchi.musicisland.Listener.SongPlayChangeNotifier;
import com.suchi.musicisland.MusicDBHelper;
import com.suchi.musicisland.Store.SongStateStore;
import com.suchi.musicisland.Utils.ExoPlayerManager;
import com.suchi.musicisland.R;
import com.suchi.musicisland.Song;
import com.suchi.musicisland.Song_Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.Iterator;
import java.util.List;

public class LikeListActivity extends BaseActivity {
    private ImageButton returnButton;
    private ImageButton playButton;
    private ImageView likeIcon;
    private TextView likeText;
    private List<Song> songList;
    private GridLayoutManager gridLayoutManager;
    private ExoPlayerManager exoPlayManager;
    private RecyclerView recyclerSongs;
    private HeaderAdapter headerAdapter;
    private FooterBlankAdapter footerBlankAdapter;
    private SongAdapterList songAdapterList;
    private ConcatAdapter concatAdapter;

    private boolean scrollAttached = false;

    private final SongLikeChangeNotifier.OnSongLikeChangedListener likeListener =
            (songId, isLike) -> loadSongsToUI();

    private void initializeView() {
        returnButton = findViewById(R.id.returnIcon);
        recyclerSongs = findViewById(R.id.recycler_songs_likeList);
        playButton = findViewById(R.id.pause_bar);
        exoPlayManager = ExoPlayerManager.getInstance();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.like_list);

        //保证状态栏可读性
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initializeView();
        setupOnClickListener();
        loadSongsToUI();
        attachScrollListenerWhenUserScrolls(recyclerSongs);

        SongLikeChangeNotifier.getInstance().addListener(likeListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SongLikeChangeNotifier.getInstance().removeListener(likeListener);
    }

    private void setupOnClickListener() {
        returnButton.setOnClickListener(v -> {
            Intent intent = new Intent(LikeListActivity.this, MainActivity.class);
            startActivity(intent);
        });

        setScrollToTopProvider(() -> {

        });
    }

    private void loadSongsToUI() {
        songList = SQLite.select()
                .from(Song.class)
                .where(Song_Table.isLike.eq(true))
                .queryList();

        gridLayoutManager = new GridLayoutManager(this,1);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return 1;
            }
        });
        recyclerSongs.setLayoutManager(gridLayoutManager);

        headerAdapter = new HeaderAdapter("已喜爱的歌曲",true);
        footerBlankAdapter = new FooterBlankAdapter();

        songAdapterList = new SongAdapterList(this, songList, new SongAdapterList.OnSongClickListener() {
            @Override
            public void onSongClick(Song song) {
                List<Song> albumSongs = SQLite.select()
                        .from(Song.class)
                        .where(Song_Table.albumId.eq(song.getAlbumId()))
                        .orderBy(Song_Table.trackNum.asc())
                        .queryList();

                exoPlayManager.setPlayList(albumSongs, exoPlayManager.findTheCurrentSongPositionInAlbum(albumSongs, song), false);
                SongPlayChangeNotifier.getInstance().notifyUIChanged();
                playButton.setImageResource(R.drawable.pause_icon);
            }

            @Override
            public void onSongLongPressed(Song song, View view) {
                showSongPopup(song, view);
            }
        });

        concatAdapter = new ConcatAdapter(headerAdapter, songAdapterList,footerBlankAdapter);
        recyclerSongs.setAdapter(concatAdapter);
    }

    ////////歌曲菜单//////////
    private void showSongPopup(Song song, View anchorView) {

        View popupView = LayoutInflater.from(this)
                .inflate(R.layout.popup_menu_song_longpress, null, false);

        likeIcon = popupView.findViewById(R.id.like_icon_longPressed);
        likeText = popupView.findViewById(R.id.like_text_longPressed);

        if (song.getIsLiked() == true) {
            likeIcon.setImageResource(R.drawable.hollow_favorite_icon);
            likeText.setText("撤销");
        }

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_pill_blue));
        popupWindow.setElevation(12f);


        popupView.findViewById(R.id.action_like_song).setOnClickListener(v -> {
            new Handler().postDelayed(()->{
                    SongStateStore.removeLiked(song.getId());
                    song.markAsDisliked(false);
                    AppExecutors.DB.execute(song::update);
                    removeSongFromUI(song);
                popupWindow.dismiss();
            }, 200);
        });

        popupView.findViewById(R.id.action_delete_song).setOnClickListener(v -> {
            new Handler().postDelayed(()->{
                popupWindow.dismiss();
                removeSongFromUI(song);
                MusicDBHelper.deleteSingleSong(song);
            }, 200);
        });

        //决定弹出位置
        int x = SongAdapterList.lastTouchX;
        int y = SongAdapterList.lastTouchY;

        // 计算 popup 显示方向
        boolean isRight = x < getResources().getDisplayMetrics().widthPixels / 2;

        // 先测量 popup 大小
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = popupView.getMeasuredWidth();

        // X轴偏移量（避免挤出屏幕）
        int offsetX = isRight ? 40 : -(popupWidth + 40);
        // Y轴偏移量（让它稍微靠下）
        int offsetY = 20;

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x + offsetX, y + offsetY);
    }

    private void removeSongFromUI(Song song) {
        Iterator<Song> iterator = songList.iterator();

        while (iterator.hasNext()) {
            Song item = iterator.next();

            //只处理song项目
            if (item instanceof Song) {
                Song s = (Song) item;
                if (s.getId() == song.getId()) {
                    iterator.remove();
                    break;
                }
            }
        }
        songAdapterList.notifyDataSetChanged();
    }

    //检测是否滑动
    private void attachScrollListenerWhenUserScrolls(RecyclerView rv) {

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {

                if (!scrollAttached && newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    //用户第一次真实滑动
                    attachScrollListener(recyclerView);
                    scrollAttached = true;
                }
            }
        });

    }

}
