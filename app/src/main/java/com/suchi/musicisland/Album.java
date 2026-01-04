package com.suchi.musicisland;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = MusicDB.class)
public class Album extends BaseModel {

    @PrimaryKey(autoincrement = true)
    long id;

    @Column
    String albumName;

    @Column
    String albumArtists;

    @Column
    String coverUri;
    @Column
    String copyRight;

    public Album() { }

    public String getCopyRight() {
        return copyRight;
    }

    public String getCoverUri() {
        return coverUri;
    }

    public Album(String albumName, String artist, String coverUri, String copyRight) {
        this.albumName = albumName;
        this.albumArtists = artist;
        this.coverUri = coverUri;
        this.copyRight = copyRight;
    }

    public long getId() {
        return id;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getArtist() {
        return albumArtists;
    }

    public void setArtist(String artist) {
        this.albumArtists = artist;
    }
}
