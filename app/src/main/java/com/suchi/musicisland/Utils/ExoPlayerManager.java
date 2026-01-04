package com.suchi.musicisland.Utils;

import static androidx.media3.common.Player.REPEAT_MODE_OFF;
import static androidx.media3.common.Player.REPEAT_MODE_ONE;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import com.suchi.musicisland.Album;
import com.suchi.musicisland.Album_Table;
import com.suchi.musicisland.Listener.SongPlayChangeNotifier;
import com.suchi.musicisland.Song;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OptIn(markerClass = UnstableApi.class)
public class ExoPlayerManager {
    private static ExoPlayerManager instance;
    private ExoPlayer exoPlayer;
    private boolean isPlaying = false;
    private boolean isCompleted = false;
    private Song currentSong;
    private int lastValidDuration = 0;
    private Context appContext;
    private PlayMode playMode = PlayMode.SEQUENCE;
    private List<Song> albumList = new ArrayList<>();

    private static final String TAG = "ExoPlayerManager";

    public static class QueueItem {
        public final Song song;
        public final int mediaIndex;

        public QueueItem(Song song, int mediaIndex) {
            this.song = song;
            this.mediaIndex = mediaIndex;
        }
    }

    public enum PlayMode {
        SEQUENCE,
        REPEAT_ALL,
        REPEAT_ONE,
        SHUFFLE
    }

    public static ExoPlayerManager getInstance() {
        if (instance == null) instance = new ExoPlayerManager();
        return instance;
    }

    public void initContext(Context context) {
        this.appContext = context.getApplicationContext();
    }

    private void initializeExoPlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
        }

        exoPlayer = new ExoPlayer.Builder(appContext).build();

        // 设置播放完成监听
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Log.d(TAG, "播放状态变化: " + playbackState);

                switch (playbackState) {
                    case Player.STATE_READY:
                        isPlaying = exoPlayer.isPlaying();
                        isCompleted = false;
                        lastValidDuration = (int) exoPlayer.getDuration();
                        break;
                    case Player.STATE_ENDED:
                        Log.d(TAG, "播放结束");
                        isPlaying = false;
                        isCompleted = true;
                        skipNext();
                        break;
                    case Player.STATE_IDLE:
                    case Player.STATE_BUFFERING:
                        break;
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.d(TAG, "播放状态: " + isPlaying);
                ExoPlayerManager.this.isPlaying = isPlaying;
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                if (mediaItem == null) return;

                Object tag = mediaItem.localConfiguration.tag;
                if (!(tag instanceof Song)) return;

                currentSong = (Song) tag;

                SongPlayChangeNotifier.getInstance().notifyUIChanged();
            }
        });
    }

    public PlayMode switchPlayMode() {
        switch (playMode) {
            case SEQUENCE:
                playMode = PlayMode.REPEAT_ALL;
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
                break;
            case REPEAT_ALL:
                playMode = PlayMode.REPEAT_ONE;
                exoPlayer.setRepeatMode(REPEAT_MODE_ONE);
                break;
            case REPEAT_ONE:
                playMode = PlayMode.SHUFFLE;
                exoPlayer.setRepeatMode(REPEAT_MODE_OFF);
                if (!(getCurrentSong() == null)) {
                    List<Song> shuffleList = buildShuffleList(getAlbumList(), getCurrentSong(), true);
                    updateFutureQueue(shuffleList);
                }
                break;
            case SHUFFLE:
                playMode = PlayMode.SEQUENCE;
                exoPlayer.setRepeatMode(REPEAT_MODE_OFF);
                if (!(getCurrentSong() == null)) {
                    List<Song> sequenceList = buildSequenceList(getAlbumList(), getCurrentSong());
                    updateFutureQueue(sequenceList);
                }
                break;
        }
        return playMode;
    }

    public ExoPlayer getPlayer() {
        if (exoPlayer == null) {
            initializeExoPlayer();
        }
        return exoPlayer;
    }

    public int getDuration() {
        if (exoPlayer != null) {
            int duration = (int) exoPlayer.getDuration();
            if (duration > 0) {
                lastValidDuration = duration;
                return duration;
            }
        }
        return Math.max(lastValidDuration, 0);
    }

    public int getCurrentListIndex() {
        return exoPlayer.getCurrentMediaItemIndex();
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public List<Song> getAlbumList() {
        return albumList;
    }

    public void setAlbumList(List<Song> albumList) {
        this.albumList = albumList;
    }

    public long getCurrentSongId() {
        return currentSong.getId();
    }

    public String getCurrentSongUri() {
        return currentSong.getSongFileUri();
    }

    public String getCurrentSongName() {
        return currentSong.getTitle();
    }

    public String getCurrentSongArtist() {
        return currentSong.getSongArtist();
    }

    public PlayMode getPlayMode() {
        return playMode;
    }

    public List<QueueItem> getNextlayList() {
        List<QueueItem> result = new ArrayList<>();

        int currentIndex = exoPlayer.getCurrentMediaItemIndex();
        if (currentIndex == C.INDEX_UNSET) return result;
        int count = exoPlayer.getMediaItemCount();

        for (int i = currentIndex + 1; i < count; i++) {
            MediaItem item = exoPlayer.getMediaItemAt(i);
            Song song = (Song) item.localConfiguration.tag;
            result.add(new QueueItem(song, i));
        }
        return result;
    }

    public int getCurrentPosition() {
        if (exoPlayer != null) {
            return (int) exoPlayer.getCurrentPosition();
        }
        return 0;
    }

    public String getCurrentSongCoverUri() {
        Album album = SQLite.select()
                .from(Album.class)
                .where(Album_Table.id.eq(currentSong.getAlbumId()))
                .querySingle();

        if (album != null) {
            return album.getCoverUri();
        } else {
            return null;
        }
    }

    public void seekToQueuePosition(int offsetFromCurrent) {
        exoPlayer.seekToDefaultPosition(offsetFromCurrent);
    }

    public void play() {
        if (exoPlayer != null) {
            exoPlayer.play();
        }

        isPlaying = true;
        isCompleted = false;
    }

    public void prepareOnly(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);

            if (exoPlayer != null) {
                exoPlayer.stop();
            }

            MediaItem mediaItem = MediaItem.fromUri(uri);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.pause();

            isPlaying = false;
            isCompleted = false;

        } catch (Exception e) {
            Log.e(TAG, "prepareOnly error: " + e.getMessage(), e);
        }
    }

    public void pause() {
        if (exoPlayer != null) {
            exoPlayer.pause();
            isPlaying = false;

        }
    }

    public void resume() {
        if (exoPlayer == null) {
            initializeExoPlayer();
        }

        Song song = getCurrentSong();
        if (song == null) {
            return;
        }

        if (exoPlayer.getMediaItemCount() == 0) {
            exoPlayer.setMediaItem(
                    MediaItem.fromUri(song.getSongFileUri())
            );
            exoPlayer.prepare();
        }

        if (!exoPlayer.isPlaying()) {
            exoPlayer.play();
        }
        isPlaying = true;

    }

    public void stop() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            isPlaying = false;
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void seekTo(int position) {
        if (exoPlayer != null) {
            exoPlayer.seekTo(position);
        }
    }

    public void removeFromList(int removeIndex) {
        exoPlayer.removeMediaItem(removeIndex);
    }

    public void skipPrevious() {
        if (exoPlayer != null && exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPrevious();
        }
    }

    public void skipNext() {
        if (exoPlayer != null && exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNext();
        }
    }

    public void insertNext(Song song) {
        if (exoPlayer == null) return;

        int insertIndex = getCurrentListIndex() + 1;

        MediaItem item = new MediaItem.Builder()
                .setUri(Uri.parse(song.getSongFileUri()))
                .setTag(song)
                .build();

        exoPlayer.addMediaItem(insertIndex, item);
    }

    /////////////////////播放方法/////////////////////////

    public void setPlayList(List<Song> songList, int startIndex, boolean isModeChange) {
        List<MediaItem> items = new ArrayList<>();

        for (Song song : songList) {
            MediaItem item = new MediaItem.Builder()
                    .setUri(Uri.parse(song.getSongFileUri()))
                    //把 Song ID 放进 tag
                    .setTag(song)
                    .build();
            items.add(item);
        }
        exoPlayer.setShuffleModeEnabled(false);

        exoPlayer.setMediaItems(items,startIndex, 0);
        exoPlayer.prepare();

        if (!isModeChange) {
            exoPlayer.play();
        }
    }

    public void updateFutureQueue(List<Song> futureSongs) {
        if (exoPlayer == null) return;

        int currentIndex = exoPlayer.getCurrentMediaItemIndex() + 1;
        if (currentIndex == C.INDEX_UNSET) return;
        int count = exoPlayer.getMediaItemCount();

        if (currentIndex < count) {
            exoPlayer.removeMediaItems(currentIndex, count);
        }

        List<MediaItem> items = new ArrayList<>();
        for (Song song : futureSongs) {
            MediaItem item = new MediaItem.Builder()
                    .setUri(Uri.parse(song.getSongFileUri()))
                    .setTag(song)
                    .build();
            items.add(item);
        }

        if (!items.isEmpty()) {
            exoPlayer.addMediaItems(currentIndex, items);
        }
    }

    public List<Song> buildSequenceList(List<Song> albumSongs, Song currentSong) {
        if (albumSongs == null) return null;

        List<Song> result = new ArrayList<>();
        boolean found = false;

        for (Song song : albumSongs) {
            if (found) {
                result.add(song);
            }
            if (song.getId() == currentSong.getId()) {
                found = true;
            }
        }

        return result;
    }

    public List<Song> buildShuffleList(List<Song> albumSongs, Song clickedSong, boolean isFromModeChange) {
        if (albumSongs == null) return null;

        List<Song> result = new ArrayList<>();
        List<Song> shuffle = new ArrayList<>();

        for (Song song : albumSongs) {
            if (!(song.getId() == clickedSong.getId())) {
                shuffle.add(song);
            }
        }
        Collections.shuffle(shuffle);

        if (!isFromModeChange) {
            result.add(clickedSong);
        }
        result.addAll(shuffle);

        return result;
    }

    ////////////工具类//////////////
    public int findTheCurrentSongPositionInAlbum(List<Song> albumSongs, Song song) {
        for (int i = 0; i < albumSongs.size(); i++) {
            if (albumSongs.get(i).getTrackNum() == song.getTrackNum()) {
                return i;
            }
        }
        return - 1;
    }

    // 在Activity销毁时释放资源
    public void release() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}
