package com.suchi.musicisland.Activity;

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.Activity.popup.MixedListSwipeCallBack;
import com.suchi.musicisland.Adapter.FooterBlankAdapter;
import com.suchi.musicisland.Adapter.HeaderBlankAdapter;
import com.suchi.musicisland.Adapter.SearchAdapter;
import com.suchi.musicisland.Album;
import com.suchi.musicisland.Album_Table;
import com.suchi.musicisland.Listener.SongLikeChangeNotifier;
import com.suchi.musicisland.Listener.SongPlayChangeNotifier;
import com.suchi.musicisland.Utils.ExoPlayerManager;
import com.suchi.musicisland.MusicDBHelper;
import com.suchi.musicisland.R;
import com.suchi.musicisland.Song;
import com.suchi.musicisland.Song_Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private EditText editSearch;
    private View root;
    private View searchBar;
    private ImageView likeIconMenu;
    private TextView likeTextMenu;
    private SearchAdapter searchAdapter;
    private EditText editText;
    private RecyclerView recycler;
    private Runnable searchRunnable;
    private HeaderBlankAdapter headerBlankAdapter;
    private ConcatAdapter concatAdapter;
    private FooterBlankAdapter footerBlankAdapter;
    private Drawable dividerDrawable;
    private ExoPlayerManager exoPlayManager;

    private ItemTouchHelper touchHelper;
    private MixedListSwipeCallBack swipeCallback;

    private List<Object> searchResults = new ArrayList<>();
    private Handler handler = new Handler();

    private final SongLikeChangeNotifier.OnSongLikeChangedListener likeListener =
            (songId, isLike) -> refreshLikeStatus(songId, isLike);

    private void initializeView() {
        editSearch = findViewById(R.id.edit_search);
        root = findViewById(R.id.search_layout);
        searchBar = findViewById(R.id.searchBar);
        editText = findViewById(R.id.edit_search);
        recycler = findViewById(R.id.recycler_search);
        exoPlayManager = ExoPlayerManager.getInstance();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        //保证状态栏可读性
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initializeView();
        setupOnClickListener();

        autoOpenKeyboard();
        setupKeyboardAwareSearchBar(root, searchBar);

        loadResultsToUI();

        //打开本activity后开始搜索
        String keyword = editText.getText().toString().trim();
        if (!keyword.isEmpty()) {
            searchDatabase(keyword);
        }

        SongLikeChangeNotifier.getInstance().addListener(likeListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SongLikeChangeNotifier.getInstance().removeListener(likeListener);
    }

    private void setupOnClickListener() {
    }

    private void autoOpenKeyboard() {
        editSearch.requestFocus();

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        );
        //压缩根布局
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );
    }

    //SearchBar适应键盘弹出
    private void setupKeyboardAwareSearchBar(View root, View searchBar) {

        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {

            Rect r = new Rect();
            root.getWindowVisibleDisplayFrame(r);

            int screenHeight = root.getRootView().getHeight();
            int visibleHeight = r.bottom - r.top;
            int heightDiff = screenHeight - visibleHeight;
            int dp150 = (int)(150 * getResources().getDisplayMetrics().density);

            float targetY;
            long duration;

            if (heightDiff > dp150) {
                // 键盘弹出来 → 动起来
                targetY = -(heightDiff - 250);
                duration = 140;
            } else {
                // 键盘收回 → 回到底部
                targetY = 0;
                duration = 220;
            }

            // 平移动画
            searchBar.animate()
                    .translationY(targetY)
                    .setDuration(duration)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .start();
        });
    }

    //执行搜索
    private void searchDatabase(String keyword) {

        searchResults.clear();

        //搜索专辑
        List<Album> albums = SQLite.select()
                .from(Album.class)
                .where(Album_Table.albumName.like("%" + keyword + "%"))
                .limit(20)
                .queryList();

        //搜索歌曲
        List<Song> songs = SQLite.select()
                .from(Song.class)
                .where(Song_Table.title.like("%" + keyword + "%"))
                .limit(20)
                .queryList();

        //合并存储
        searchResults.addAll(albums);
        searchResults.addAll(songs);

        //最多展示20条
        if (searchResults.size() > 20) {
            searchResults = searchResults.subList(0, 20);
        }

        searchAdapter.updateList(searchResults);
    }

    private void loadResultsToUI() {
        recycler.setLayoutManager(new LinearLayoutManager(this));

        searchAdapter = new SearchAdapter(this, new ArrayList<>(), new SearchAdapter.OnSearchClickListener() {
            @Override
            public void onAlbumClicked(Album album) {
                Intent intent = new Intent(SearchActivity.this, AlbumDetailActivity.class);
                intent.putExtra("albumId", album.getId());
                MusicDBHelper.getInstance().setCurrentAlbumId(album.getId());
                startActivity(intent);
            }

            @Override
            public void onSongClicked(Song song) {
                List<Song> albumSongs = SQLite.select()
                        .from(Song.class)
                        .where(Song_Table.albumId.eq(song.getAlbumId()))
                        .orderBy(Song_Table.trackNum.asc())
                        .queryList();

                exoPlayManager.setPlayList(albumSongs, exoPlayManager.findTheCurrentSongPositionInAlbum(albumSongs, song), false);
                SongPlayChangeNotifier.getInstance().notifyUIChanged();
            }

            @Override
            public void onSongLongPressed(Song song, View view) {
                showSongPopup(song, view);
            }
        });

        //歌曲滑动
        swipeCallback = new MixedListSwipeCallBack(this, exoPlayManager, searchAdapter);
        touchHelper = new ItemTouchHelper(swipeCallback);
        touchHelper.attachToRecyclerView(recycler);

        headerBlankAdapter = new HeaderBlankAdapter();
        footerBlankAdapter = new FooterBlankAdapter();

        concatAdapter = new ConcatAdapter(headerBlankAdapter, searchAdapter, footerBlankAdapter);
        recycler.setAdapter(concatAdapter);

        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);

                searchRunnable = () -> {
                    String text = s.toString().trim();
                    if (text.isEmpty()) {
                        searchAdapter.updateList(new ArrayList<>());
                    } else {
                        searchDatabase(text);
                    }
                };
                handler.postDelayed(searchRunnable, 250);
            }
        });
    }


    /////////搜索内歌曲弹出菜单///////////
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
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.pill_bg));
        popupWindow.setElevation(12f);


        popupView.findViewById(R.id.action_like_song).setOnClickListener(v -> {
            new Handler().postDelayed(()->{
                boolean isNowLiked;

                if (song.getIsLiked() != true) {
                    song.markAsLiked(true);
                    song.update();
                    isNowLiked = true;

                    Toast.makeText(this, "已喜爱", Toast.LENGTH_SHORT).show();
                } else {
                    song.markAsDisliked(false);
                    song.update();
                    isNowLiked = false;

                    Toast.makeText(this, "已取消喜爱", Toast.LENGTH_SHORT).show();
                }

                SongLikeChangeNotifier.getInstance().notifySongLikeChanged(song.getId(), isNowLiked);
                popupWindow.dismiss();
            }, 200);

        });

        popupView.findViewById(R.id.action_delete_song).setOnClickListener(v -> {
            new Handler().postDelayed(()->{
                popupWindow.dismiss();
                MusicDBHelper.deleteSingleSong(song);

                removeSongFromUI(song.getId());

            }, 200);

        });

        //决定弹出位置
        int x = SearchAdapter.lastTouchX;
        int y = SearchAdapter.lastTouchY;

        // 计算 popup 显示方向
        boolean isRight = x < getResources().getDisplayMetrics().widthPixels / 2;

        // 先测量 popup 大小
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = popupView.getMeasuredWidth();

        // X轴偏移量（避免挤出屏幕）
        int offsetX = isRight ? 40 : -(popupWidth + 40);
        // Y轴偏移量（让它稍微靠下）
        int offsetY = 40;

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x + offsetX, y + offsetY);
    }

    private void removeSongFromUI(long songId) {
        Iterator<Object> iterator =  searchResults.iterator();

        while (iterator.hasNext()) {
            Object item = iterator.next();

            //只处理Song项目
            if (item instanceof Song) {
                Song s = (Song) item;
                if (s.getId() == songId) {
                    iterator.remove();
                    break;
                }
            }
        }
        searchAdapter.notifyDataSetChanged();
    }

    private void refreshLikeStatus(long songId, boolean isLike) {
        if (searchResults == null || searchResults.isEmpty()) return;

        for (int i = 0; i < searchResults.size(); i++) {
            Object item = searchResults.get(i);

            // 只处理 Song
            if (item instanceof Song) {
                Song song = (Song) item;

                if (song.getId() == songId) {
                    // 更新对象状态
                    song.markAsLiked(isLike);

                    // 刷新该 item，而不是全局刷新
                    searchAdapter.notifyItemChanged(i);

                    break; // 找到一个即可，退出循环
                }
            }
        }
    }

}
