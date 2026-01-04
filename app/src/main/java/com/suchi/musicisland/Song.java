package com.suchi.musicisland;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = MusicDB.class)
public class Song extends BaseModel {

    @PrimaryKey(autoincrement = true)
    long id;

    @Column
    String title;

    @Column
    long albumId;   // 对应 Album.id

    @Column
    String songArtist;

    @Column
    long durationMs;
    @Column
    String songFileUri;
    @Column
    Boolean isLike = false;
    @Column
    int trackNum;

    public int getTrackNum() {
        return trackNum;
    }

    public void setTrackNum(int trackNum) {
        this.trackNum = trackNum;
    }

    public Boolean getIsLiked() {
        return isLike;
    }

    public Song() { }

    public void markAsLiked(boolean isLike) {
        this.isLike = isLike;
    }

    public void markAsDisliked(boolean isLike) {
        this.isLike = isLike;
    }

    public String getSongFileUri() {
        return songFileUri;
    }

    public void setSongFileUri(String songFileUri) {
        this.songFileUri = songFileUri;
    }

    public Song(String title, long albumId, String songArtist, long durationMs, String songFileUri, boolean isLike, int trackNum) {
        this.title = title;
        this.albumId = albumId;
        this.songArtist = songArtist;
        this.durationMs = durationMs;
        this.songFileUri = songFileUri;
        this.isLike = isLike;
        this.trackNum = trackNum;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSongArtist() {
        return songArtist;
    }

    public void setSongArtist(String songArtist) {
        this.songArtist = songArtist;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }
}
