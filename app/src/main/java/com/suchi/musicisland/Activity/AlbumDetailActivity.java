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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;


import com.suchi.musicisland.Activity.popup.AlbumDetailSwipeCallback;
import com.suchi.musicisland.Adapter.AlbumDetailAdapter;
import com.suchi.musicisland.Adapter.FooterAdapter;

import com.suchi.musicisland.Adapter.HeaderBlankAdapter;
import com.suchi.musicisland.Adapter.ItemAdapter;
import com.suchi.musicisland.Adapter.SongAdapterAlbum;
import com.suchi.musicisland.Album;
import com.suchi.musicisland.Album_Table;
import com.suchi.musicisland.Executor.AppExecutors;
import com.suchi.musicisland.Listener.SongLikeChangeNotifier;
import com.suchi.musicisland.Listener.SongPlayChangeNotifier;
import com.suchi.musicisland.Store.SongStateStore;
import com.suchi.musicisland.Utils.ExoPlayerManager;
import com.suchi.musicisland.MusicDBHelper;
import com.suchi.musicisland.R;
import com.suchi.musicisland.Song;
import com.suchi.musicisland.Song_Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.Iterator;
import java.util.List;

public class AlbumDetailActivity extends BaseActivity {
    private ImageButton returnButton;
    private ImageButton playButton;
    private ImageView likeIconMenu;
    private TextView likeTextMenu;
    private RecyclerView recyclerSongs;
    private GridLayoutManager gridLayoutManager;
    private ExoPlayerManager exoPlayManager;
    private long albumId;
    private List<Song> songList;
    private List<Album> albumList;
    private SongAdapterAlbum songAdapterAlbum;
    private AlbumDetailAdapter albumDetailAdapter;
    private ConcatAdapter concatAdapter;
    private HeaderBlankAdapter headerBlankAdapter;
    private FooterAdapter footerAdapter;
    private ItemAdapter itemAdapter;

    private AlbumDetailSwipeCallback swipeCallback;
    private ItemTouchHelper touchHelper;

    private final SongLikeChangeNotifier.OnSongLikeChangedListener likeListener =
            (songId, isLike) -> refreshLikeStatus(songId, isLike);


    //是否滚动
    private boolean scrollAttached = false;

    private void initializeViews() {
        returnButton = findViewById(R.id.icon_return_album);
        recyclerSongs = findViewById(R.id.recycler_songs);
        playButton = findViewById(R.id.pause_bar);
        exoPlayManager = ExoPlayerManager.getInstance();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.album_list);

        //保证状态栏可读性
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initializeViews();
        setOnClickListener();
        loadSongsToUI();
        attachScrollListenerWhenUserScrolls(recyclerSongs);

        SongLikeChangeNotifier.getInstance().addListener(likeListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SongLikeChangeNotifier.getInstance().removeListener(likeListener);
        SongPlayChangeNotifier.getInstance().removeListener(itemAdapter);
    }

    private void setOnClickListener() {
        returnButton.setOnClickListener(v -> {
            Intent intent = new Intent(AlbumDetailActivity.this, MainActivity.class);
            startActivity(intent);
        });

        setScrollToTopProvider(() -> {
            recyclerSongs.stopScroll();
            recyclerSongs.smoothScrollToPosition(0);
        });
    }

    private void loadSongsToUI() {
        albumId = getIntent().getLongExtra("albumId", -1);

        songList = SQLite.select()
                .from(Song.class)
                .where(Song_Table.albumId.eq(albumId))
                .orderBy(Song_Table.trackNum.asc()) //按照歌曲track来排序
                .queryList();

        albumList = SQLite.select()
                .from(Album.class)
                .where(Album_Table.id.eq(albumId))
                .queryList();

        Album album = SQLite.select()
                .from(Album.class)
                .where(Album_Table.id.eq(albumId))
                .querySingle();

        gridLayoutManager = new GridLayoutManager(this, 1);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return 1;
            }
        });
        recyclerSongs.setLayoutManager(gridLayoutManager);

        headerBlankAdapter = new HeaderBlankAdapter();

        itemAdapter = new ItemAdapter(new ItemAdapter.OnButtonClickListener() {
            @Override
            public void onPlayBtnClick(View v) {
                exoPlayManager.setPlayList(songList,0, false);
            }

            @Override
            public void onMenuBtnClick(View v) {
            }

            @Override
            public void onPinBtnClick(View v) {

            }
        });
        //注册listener
        SongPlayChangeNotifier.getInstance().addListener(itemAdapter);

        albumDetailAdapter = new AlbumDetailAdapter(this, albumList);

        footerAdapter = new FooterAdapter(MusicDBHelper.getAlbumSongCount(albumId), MusicDBHelper.convertMsToMin(), album.getCopyRight());

        songAdapterAlbum = new SongAdapterAlbum(this, songList, new SongAdapterAlbum.OnSongClickListener() {
            @Override
            public void onSongClick(Song song) {
                List<Song> albumSongs = SQLite.select()
                                .from(Song.class)
                                .where(Song_Table.albumId.eq(song.getAlbumId()))
                                .orderBy(Song_Table.trackNum.asc())
                                .queryList();

                exoPlayManager.setAlbumList(albumSongs);

                if (exoPlayManager.getPlayMode() == ExoPlayerManager.PlayMode.SHUFFLE) {
                    //自己shuffle
                    List<Song> shuffleList = exoPlayManager.buildShuffleList(albumSongs, song, false);
                    exoPlayManager.setPlayList(shuffleList, 0, false);
                } else {
                    exoPlayManager.setPlayList(albumSongs, exoPlayManager.findTheCurrentSongPositionInAlbum(albumSongs, song), false);
                }

                SongPlayChangeNotifier.getInstance().notifyUIChanged();
            }

            @Override
            public void onSongLongPressed(Song song, View view) {
                showSongPopup(song, view);
            }
        });

        //滑动歌曲
        swipeCallback = new AlbumDetailSwipeCallback(this, songList, exoPlayManager, songAdapterAlbum);
        touchHelper = new ItemTouchHelper(swipeCallback);
        touchHelper.attachToRecyclerView(recyclerSongs);
        concatAdapter = new ConcatAdapter(headerBlankAdapter, albumDetailAdapter, itemAdapter, songAdapterAlbum, footerAdapter);
        recyclerSongs.setAdapter(concatAdapter);
    }

    /// /////歌曲菜单//////////
    private void showSongPopup(Song song, View anchorView) {

        View popupView = LayoutInflater.from(this)
                .inflate(R.layout.popup_menu_song_longpress, null, false);

        likeIconMenu = popupView.findViewById(R.id.like_icon_longPressed);
        likeTextMenu = popupView.findViewById(R.id.like_text_longPressed);

        if (song.getIsLiked() == true) {
            likeIconMenu.setImageResource(R.drawable.hollow_favorite_icon);
            likeTextMenu.setText("撤销");
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
            new Handler().postDelayed(() -> {
                boolean isNowLiked;

                if (!SongStateStore.isLiked(song.getId())) {
                    SongStateStore.setLiked(song.getId(), true);
                    song.markAsLiked(true);

                    AppExecutors.DB.execute(song::update);

                    isNowLiked = true;
                } else {
                    SongStateStore.removeLiked(song.getId());
                    song.markAsDisliked(false);

                    AppExecutors.DB.execute(song::update);

                    isNowLiked = false;
                }

                SongLikeChangeNotifier.getInstance().notifySongLikeChanged(song.getId(), isNowLiked);

                popupWindow.dismiss();
            }, 200);

        });

        popupView.findViewById(R.id.action_delete_song).setOnClickListener(v -> {
            new Handler().postDelayed(() -> {
                popupWindow.dismiss();
                removeSongFromUI(song);
                MusicDBHelper.deleteSingleSong(song);
            }, 200);

        });

        //决定弹出位置
        int x = SongAdapterAlbum.lastTouchX;
        int y = SongAdapterAlbum.lastTouchY;

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
        songAdapterAlbum.notifyDataSetChanged();
    }

    private void refreshLikeStatus(long songId, boolean isLike) {
        for (int i = 0; i < songList.size(); i++) {
            if (songList.get(i).getId() == songId) {
                songList.get(i).markAsLiked(isLike);
                songAdapterAlbum.notifyItemChanged(i);
                break;
            }
        }
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