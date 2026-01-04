package com.suchi.musicisland.Utils;

import android.content.Context;
import android.net.Uri;

import com.suchi.musicisland.Album;
import com.suchi.musicisland.Executor.AppExecutors;
import com.suchi.musicisland.MusicDBHelper;

public class AlbumImportUtil {

    public static void importAlbum(
            Context context,
            Uri fileUri,
            String albumName,
            String albumArtists,
            String coverPath,
            ImportCallback callback
    ) {
        AppExecutors.DB.execute(() -> {
            try {
                //解析 publisher（jaudiotagger）
                String publisher =
                        AudioMetaUtil.getPublisherFromUri(context, fileUri);

                // 保存 Album
                Album album = new Album(
                        albumName,
                        albumArtists,
                        coverPath,
                        publisher
                );
                long albumId = MusicDBHelper.saveAlbum(album);

                //回调（回到调用者）
                    callback.onAlbumImported(albumId);

            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public interface ImportCallback {
        void onAlbumImported(long albumId);
        void onError(Exception e);
    }
}
