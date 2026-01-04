package com.suchi.musicisland.UI;

import androidx.recyclerview.widget.DiffUtil;

import com.suchi.musicisland.Album;
import com.suchi.musicisland.MusicDBHelper;

import java.util.List;

public class AlbumDiff extends DiffUtil.Callback {
    private final List<Album> oldList;
    private final List<Album> newList;

    public AlbumDiff(List<Album> oldList, List<Album> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList == null ? 0 : oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList == null ? 0 : newList.size();
    }

    //是否是同一个 Album
    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getId()
                == newList.get(newItemPosition).getId();
    }

    //内容是否相同
    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Album oldAlbum = oldList.get(oldItemPosition);
        Album newAlbum = newList.get(newItemPosition);

        return oldAlbum.getAlbumName().equals(newAlbum.getAlbumName())
                && oldAlbum.getCoverUri().equals(newAlbum.getCoverUri())
                && MusicDBHelper.getAlbumSongCount(oldAlbum.getId()) == MusicDBHelper.getAlbumSongCount(newAlbum.getId());
    }
}
