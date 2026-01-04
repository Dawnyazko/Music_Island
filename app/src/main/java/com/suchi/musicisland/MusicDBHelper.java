package com.suchi.musicisland;

import android.database.Cursor;

import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.suchi.musicisland.Executor.AppExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MusicDBHelper {
    private static MusicDBHelper instance;
    private long currentAlbumID;

    // -----------------------------
    // Album 操作
    // -----------------------------

    /** 保存或更新 Album（以 albumName + artist 去重） */
    public static long saveAlbum(Album album) {

        Album existing = SQLite.select()
                .from(Album.class)
                .where(Album_Table.albumName.eq(album.getAlbumName()))
                .and(Album_Table.albumArtists.eq(album.getArtist()))
                .querySingle();

        if (existing != null) {
            existing.setAlbumName(album.getAlbumName());
            existing.setArtist(album.getArtist());
            existing.update();
            return existing.getId();
        }

        album.insert();
        return album.getId();
    }

    /** 获取所有专辑 */
    public static List<Album> getAllAlbums() {
        return SQLite.select()
                .from(Album.class)
                .queryList();
    }

    /** 根据 ID 获取专辑 */
    public static Album getAlbumById(long id) {
        return SQLite.select()
                .from(Album.class)
                .where(Album_Table.id.eq(id))
                .querySingle();
    }

    /** 根据 名称 + 歌手 获取专辑 */
    public static Album getAlbumByNameAndArtist(String name, String artist) {
        return SQLite.select()
                .from(Album.class)
                .where(Album_Table.albumName.eq(name))
                .and(Album_Table.albumArtists.eq(artist))
                .querySingle();
    }

    /** 删除专辑（同时删除所属歌曲） */
    public static void deleteAlbum(long id) {
            // 先删除该专辑下的歌曲
            SQLite.delete(Song.class)
                    .where(Song_Table.albumId.eq(id))
                    .execute();

            // 再删除专辑
            SQLite.delete(Album.class)
                    .where(Album_Table.id.eq(id))
                    .execute();
    }

    /** 获取专辑数量 */
    public static long getAlbumCount() {
        return SQLite.selectCountOf()
                .from(Album.class)
                .count();
    }


    // -----------------------------
    // Song 操作
    // -----------------------------

    /** 保存或更新 Song（以 title + albumId 去重） */
    public static long saveSong(Song song) {

        Song existing = SQLite.select()
                .from(Song.class)
                .where(Song_Table.title.eq(song.getTitle()))
                .and(Song_Table.albumId.eq(song.getAlbumId()))
                .querySingle();

        if (existing != null) {
            existing.setTitle(song.getTitle());
            existing.setAlbumId(song.getAlbumId());
            existing.setSongArtist(song.getSongArtist());
            existing.setDurationMs(song.getDurationMs());
            existing.setSongFileUri(song.getSongFileUri().toString());
            existing.update();
            return existing.getId();
        }

        song.insert();
        return song.getId();
    }

    /** 删除某个歌曲 */
    public static void deleteSingleSong(Song song) {
        AppExecutors.DB.execute(() -> {
            SQLite.delete(Song.class)
                    .where(Song_Table.id.eq(song.getId()))
                    .execute();
        });
    }

    /** 获取所有已喜欢的歌曲 */
    public static List<Long> getAllLikedSongIds() {
        List<Long> result = new ArrayList<>();

        Cursor cursor = SQLite
                .select(Song_Table.id)
                .from(Song.class)
                .where(Song_Table.isLike.eq(true))
                .query();

        try {
            int idIndex = cursor.getColumnIndexOrThrow("id");
            while (cursor.moveToNext()) {
                result.add(cursor.getLong(idIndex));
            }
        } finally {
            cursor.close();
        }

        return result;
    }


    /** 获取某个专辑的所有歌曲 */
    public static List<Song> getSongsByAlbum(long albumId) {
        return SQLite.select()
                .from(Song.class)
                .where(Song_Table.albumId.eq(albumId))
                .queryList();
    }

    public static long getTotalTimeFromAlbum() {
        long albumId = MusicDBHelper.getInstance().getCurrentAlbumID();
        List<Song> songList = getSongsByAlbum(albumId);
        long totalTime = 0;

        for (int i = 0; i < songList.size(); i++) {
            long songTime = songList.get(i).getDurationMs();
            totalTime += songTime;
        }
        return totalTime;
    }

    public static long convertMsToMin() {
        long milliseconds = getTotalTimeFromAlbum();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        return minutes;
    }

    /** 统计歌曲数量 */
    private MusicDBHelper() {
        // private 防止外部 new
    }

    public void setCurrentAlbumId(long currentAlbumID) {
        this.currentAlbumID = currentAlbumID;
    }

    public static synchronized MusicDBHelper getInstance() {
        if (instance == null) {
            instance = new MusicDBHelper();
        }
        return instance;
    }

    public long getCurrentAlbumID() {
        return currentAlbumID;
    }

    public static long getAlbumSongCount(Long currentAlbumId) {
        return SQLite.selectCountOf()
                .from(Song.class)
                .where(Song_Table.albumId.eq(currentAlbumId))
                .count();
    }


    // -----------------------------
    // 清空数据库
    // -----------------------------

    public static void clearAllData() {

        SQLite.delete(Song.class).execute();
        SQLite.delete(Album.class).execute();
    }
}
