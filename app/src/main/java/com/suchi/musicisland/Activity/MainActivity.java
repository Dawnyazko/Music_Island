package com.suchi.musicisland.Activity;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.MoreExecutors;
import com.suchi.musicisland.Adapter.AlbumAdapter;
import com.suchi.musicisland.Adapter.FooterBlankAdapter;
import com.suchi.musicisland.Adapter.HeaderAdapter;
import com.suchi.musicisland.Album;
import com.suchi.musicisland.Executor.AppExecutors;
import com.suchi.musicisland.Listener.MainUIChangeNotifier;
import com.suchi.musicisland.Service.MusicPlaybackService;
import com.suchi.musicisland.Utils.ExoPlayerManager;
import com.suchi.musicisland.Store.PreferenceStore;
import com.suchi.musicisland.MusicDBHelper;
import com.suchi.musicisland.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private ImageButton likeBottom;
    private ImageButton optionMenuBottom;
    private PreferenceStore sortManager;
    private String currentSortMethod;
    private RecyclerView recyclerAlbums;
    private AlbumAdapter albumAdapter;
    private List<Album> albumList;
    private GridLayoutManager gridLayoutManager;
    private ExoPlayerManager exoPlayManager;
    private ConcatAdapter concatAdapter;
    private HeaderAdapter headerAdapter;
    private FooterBlankAdapter footerBlankAdapter;
    private Parcelable recyclerViewState;

    //是否滚动
    private boolean scrollAttached = false;

    private ListenableFuture<MediaController> controllerFuture;
    private SessionToken sessionToken;

    private final MainUIChangeNotifier.OnMainUIChangedListener UIStateChangedListener = this::refreshAlbumUI;


    private void initializeViews() {
        likeBottom = findViewById(R.id.icon_like);
        optionMenuBottom = findViewById(R.id.icon_optionMenu);
        recyclerAlbums = findViewById(R.id.recycler_albums);
        exoPlayManager = ExoPlayerManager.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.mainlayout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pill_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();

        sortManager = new PreferenceStore(this);
        currentSortMethod = sortManager.getSortMethod();

        setupClickListeners();
        InitUI();
        refreshAlbumUI();
        attachScrollListenerWhenUserScrolls(recyclerAlbums);
        MainUIChangeNotifier.getInstance().addListener(UIStateChangedListener);

        createMediaControllerFuture();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void createMediaControllerFuture() {
        sessionToken = new SessionToken(this, new ComponentName(this, MusicPlaybackService.class));

        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                MediaController mediaController = controllerFuture.get();
                MediaItem mediaItem = new MediaItem.Builder()
                        .setMediaId("Music Island")
                        .setUri(exoPlayManager.getCurrentSongUri())
                        .setMediaMetadata(
                                new MediaMetadata.Builder()
                                        .setTitle(exoPlayManager.getCurrentSongName())
                                        .setArtist(exoPlayManager.getCurrentSongArtist())
                                        .build()
                        )
                        .build();

                mediaController.setMediaItem(mediaItem);
                mediaController.prepare();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    protected void onPause() {
        super.onPause();

        //保存滚动的位置
        recyclerViewState = recyclerAlbums.getLayoutManager().onSaveInstanceState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //恢复滚动的位置
        if (recyclerViewState != null) {
            recyclerAlbums.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        }
    }

    @Override
    protected void onDestroy() {
        MediaController.releaseFuture(controllerFuture);
        MainUIChangeNotifier.getInstance().removeListener(UIStateChangedListener);
        super.onDestroy();
    }

    private void InitUI() {
        //标题，专辑和底部填充横跨列数决定
        gridLayoutManager = new GridLayoutManager(this,2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {

                //头部跨两列
                if (position == 0) return 2;

                //底部跨两列
                if (position == concatAdapter.getItemCount() - 1) {
                    return 2;
                }

                //中间的专辑跨一列
                return 1;
            }
        });
        recyclerAlbums.setLayoutManager(gridLayoutManager);


        //为添加的顶部和底部填充赋值
        headerAdapter = new HeaderAdapter("资料库",false);
        footerBlankAdapter = new FooterBlankAdapter();

        albumAdapter = new AlbumAdapter(this, new AlbumAdapter.OnAlbumClickListener() {
            @Override
            public void onAlbumClick(Album album) {
                Intent intent = new Intent(MainActivity.this, AlbumDetailActivity.class);
                intent.putExtra("albumId", album.getId());
                MusicDBHelper.getInstance().setCurrentAlbumId(album.getId());
                startActivity(intent);
            }

            @Override
            public void onAlbumLongPressed(Album album, View view) {
                Log.d("DBG", "longPress album=" + album.getAlbumName() + " view=" + (view != null));
                showAlbumPopup(album, view);
            }
        });

        //拼接 Adapter
        concatAdapter = new ConcatAdapter(headerAdapter, albumAdapter, footerBlankAdapter);
        recyclerAlbums.setAdapter(concatAdapter);
    }

    private void setupClickListeners() {
        //like按钮跳转
        likeBottom.setOnClickListener(v -> {
            new Handler().postDelayed(()->{
                Intent intent = new Intent(MainActivity.this, LikeListActivity.class);
                startActivity(intent);
            }, 80);

        });

        //返回顶部(实际接口在BaseActivity）
        setScrollToTopProvider(() -> {
            recyclerAlbums.stopScroll();
            recyclerAlbums.smoothScrollToPosition(0);
        });

        //菜单按钮跳转
        optionMenuBottom.setOnClickListener(v -> {
            new Handler().postDelayed(()->{
                showPopupMenu(v);
            }, 50);

        });

    }

    public void showPopupMenu(View anchor) {
        View popupView = getLayoutInflater().inflate(R.layout.popup_menu_main,null);



        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_pill_blue));
        popupWindow.setElevation(12f);

        LinearLayout layoutImport = popupView.findViewById(R.id.layout_import);
        LinearLayout layoutSort = popupView.findViewById(R.id.layout_sort);
        LinearLayout layoutSettings = popupView.findViewById(R.id.layout_settings);
        LinearLayout layoutRecent = popupView.findViewById(R.id.layout_recent);
        LinearLayout layoutAlphabetical = popupView.findViewById(R.id.layout_alphabetical);

        ImageView iconRecentCheck = popupView.findViewById(R.id.icon_recent_check);
        ImageView iconAlphabeticalCheck = popupView.findViewById(R.id.icon_alphabetical_check);

        updateCheckIcons(iconRecentCheck, iconAlphabeticalCheck);

        layoutImport.setOnClickListener(v -> {
            new Handler().postDelayed(()->{
                Intent intent = new Intent(MainActivity.this, ImportMusicActivity.class);
                startActivity(intent);
                popupWindow.dismiss();
            }, 80);

        });

        layoutSort.setOnClickListener(v -> {
            new Handler().postDelayed(()->{
                boolean isVisible = layoutRecent.getVisibility() == View.VISIBLE;
                layoutRecent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                layoutAlphabetical.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            }, 80);

        });

        layoutRecent.setOnClickListener(v -> {
            new Handler().postDelayed(()->{
                currentSortMethod = PreferenceStore.SORT_RECENT;
                sortManager.saveSortMethod(PreferenceStore.SORT_RECENT);
                updateCheckIcons(iconRecentCheck, iconAlphabeticalCheck);
                Toast.makeText(this, "已选择：添加日期", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
            }, 200);

        });

        layoutAlphabetical.setOnClickListener(v -> {
            new Handler().postDelayed(()->{
                currentSortMethod = PreferenceStore.SORT_ALPHABETICAL;
                sortManager.saveSortMethod(PreferenceStore.SORT_ALPHABETICAL);
                updateCheckIcons(iconRecentCheck, iconAlphabeticalCheck);
                Toast.makeText(this, "已选择：字母顺序", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
            }, 200);

        });

        layoutSettings.setOnClickListener(v -> {
            new Handler().postDelayed(()->{
                popupWindow.dismiss();
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
            }, 80);

        });

        popupWindow.showAsDropDown(anchor, -375, 60);
    }

    private void updateCheckIcons(ImageView recentCheck, ImageView alphabeticalCheck) {
        if (currentSortMethod.equals(PreferenceStore.SORT_RECENT)) {
            recentCheck.setVisibility(View.VISIBLE);
            alphabeticalCheck.setVisibility(View.GONE);
        } else if (currentSortMethod.equals(PreferenceStore.SORT_ALPHABETICAL)) {
            recentCheck.setVisibility(View.GONE);
            alphabeticalCheck.setVisibility(View.VISIBLE);
        }
    }



    /////////////////** 专辑操作 *///////////////

    /** 加载专辑数据 **/

    private void refreshAlbumUI() {
        AppExecutors.DB.execute(() -> {

            albumList = SQLite
                    .select()
                    .from(Album.class)
                    .queryList();

            runOnUiThread(() -> {
                albumAdapter.submitList(new ArrayList<>(albumList));
            });
        });
    }


    /////////////////////长按专辑菜单/////////////////////////
    public void showAlbumPopup(Album album, View anchorView) {

        View popupView = LayoutInflater.from(this)
                .inflate(R.layout.popup_menu_album_longpress, null, false);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_pill_blue));
        popupWindow.setElevation(12f);

        //测量popupWindow 大小
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupW = popupView.getMeasuredWidth();

        //获得专辑卡片的坐标
        int[] loc = new int[2];
        anchorView.getLocationOnScreen(loc);
        int albumX = loc[0];
        int albumY = loc[1];


        // Screen width -> 判断卡片在左半还是右半
        int screenWidth = getResources().getDisplayMetrics().widthPixels;

        boolean isLeftSide = albumX < (screenWidth / 2);

        //最终调整
        int popupX;
        if (isLeftSide) {
            // 卡片在左侧 → 弹窗显示在右下
            popupX = albumX + anchorView.getWidth() - popupW + 10;
        } else {
            // 卡片在右侧 → 弹窗显示在左下
            popupX = albumX - 10;
        }

        int popupY = albumY + anchorView.getHeight() + 2;


        popupWindow.showAtLocation(
                getWindow().getDecorView(),
                Gravity.NO_GRAVITY,
                popupX,
                popupY
        );

        // 点击事件处理
        popupView.findViewById(R.id.action_pin).setOnClickListener(v -> {
            new Handler().postDelayed(()->{
                Toast.makeText(this, "还没建好/(ㄒoㄒ)/~~", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
            }, 200);

        });

        popupView.findViewById(R.id.action_delete).setOnClickListener(v -> {

            new Handler().postDelayed(()->{
                popupWindow.dismiss();
                AppExecutors.DB.execute(() -> {
                    MusicDBHelper.deleteAlbum(album.getId());

                    List<Album> newList = SQLite.select()
                            .from(Album.class)
                            .queryList();

                    runOnUiThread(() -> {
                        albumAdapter.submitList(newList);
                    });
                });

            }, 200);
        });
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