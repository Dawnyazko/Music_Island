package com.suchi.musicisland;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.suchi.musicisland.Utils.AlbumImportUtil;
import com.suchi.musicisland.Utils.Util;

import java.io.FileOutputStream;



public class MusicScanner {
    private static final String[] MUSIC_EXT = {
            ".mp3",".flac",".m4a",".aac",".ogg",".wav"
    };

    private Context context;

    public MusicScanner(Context context) {
        this.context = context;
    }

    // 扫描整个目录并导入音乐
    public void scanFolder(Uri treeUri) {
        DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
        if (root == null || !root.isDirectory()) {
            Log.e("MusicScanner", "Invalid Directory");
            return;
        }

        scanDocumentTree(root);
    }

    private void scanDocumentTree(DocumentFile dir) {
        for (DocumentFile file : dir.listFiles()) {
            if (file.isDirectory()) {
                scanDocumentTree(file);
            } else if (isMusicFile(file.getName())) {
                importMusicFile(file.getUri());
            }
        }
    }


    private boolean isMusicFile(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".flac") ||
                lower.endsWith(".m4a") || lower.endsWith(".aac") ||
                lower.endsWith(".ogg") || lower.endsWith(".wav");
    }

    // 导入单个音乐文件
    private void importMusicFile(Uri fileUri) {
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(context, fileUri);

            String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String albumArtists = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST);
            String albumName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

            String songArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String songFileUri = fileUri.toString();
            String trackStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            boolean isLike = false;

            long durationMs = Long.parseLong(duration);

            // 封面
            byte[] cover = mmr.getEmbeddedPicture();
            String coverPath = null;

            if (cover != null) {
                coverPath = saveAlbumArt(cover, albumName);
            }

            //导入
            AlbumImportUtil.importAlbum(
                    context,
                    fileUri,
                    albumName,
                    albumArtists,
                    coverPath,
                    new AlbumImportUtil.ImportCallback() {
                        @Override
                        public void onAlbumImported(long albumId) {
                            int trackNum = Util.parseTrackNumber(trackStr);

                            // 保存 Song
                            MusicDBHelper.saveSong(new Song(title, albumId, songArtist, durationMs, songFileUri, isLike, trackNum));
                        }

                        @Override
                        public void onError(Exception e) {
                            e.printStackTrace();
                        }
                    }
            );

            mmr.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // 保存封面图片到本地
    private String saveAlbumArt(byte[] data, String albumName) {
        try {
             FileOutputStream fos = context.openFileOutput(
                     albumName.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg",
                     Context.MODE_PRIVATE
             );
             fos.write(data);
             fos.close();

             return context.getFileStreamPath(
                     albumName.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg"
             ).getAbsolutePath();

        } catch (Exception e) {
            return null;
        }
    }
}
