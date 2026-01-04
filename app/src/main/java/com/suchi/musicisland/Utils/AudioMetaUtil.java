package com.suchi.musicisland.Utils;


import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AudioMetaUtil {
    private static final String TAG = "AudioMetadataUtil";

     //从 Uri 读取专辑发行公司（Record Label / Publisher）

    @Nullable
    public static String getPublisherFromUri(Context context, Uri uri) {
        File tempFile = null;
        try {
            tempFile = copyUriToTempFile(context, uri);

            AudioFile audioFile = AudioFileIO.read(tempFile);
            Tag tag = audioFile.getTag();
            if (tag == null) return null;

            // 通用字段（MP3 / MP4 等）
            String label = tag.getFirst(FieldKey.RECORD_LABEL);
            if (!isEmpty(label)) return label;

            // MP3: TCOP
            String copyright = tag.getFirst(FieldKey.COPYRIGHT);
            if (!isEmpty(copyright)) return copyright;

            // FLAC / OGG: Vorbis Comment
            if (tag instanceof org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag) {
                org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag vcTag =
                        (org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag) tag;

                String vcCopyright = vcTag.getFirst("COPYRIGHT");
                if (!isEmpty(vcCopyright)) return vcCopyright;

                // 有些人会写在 LABEL / PUBLISHER
                String vcLabel = vcTag.getFirst("LABEL");
                if (!isEmpty(vcLabel)) return vcLabel;

                String vcPublisher = vcTag.getFirst("PUBLISHER");
                if (!isEmpty(vcPublisher)) return vcPublisher;
            }

        } catch (Exception e) {
            Log.e(TAG, "read metadata failed", e);
        } finally {
            if (tempFile != null) tempFile.delete();
        }
        return null;
    }


     //把 content:// Uri 复制成临时 File

    private static File copyUriToTempFile(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new FileNotFoundException("Cannot open InputStream from Uri");
        }

        String ext = getExtensionFromUri(context, uri); // flac / mp3 / m4a
        File tempFile = File.createTempFile("audio_", "." + ext, context.getCacheDir());

        FileOutputStream outputStream = new FileOutputStream(tempFile);

        byte[] buffer = new byte[8192];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }

        inputStream.close();
        outputStream.close();

        return tempFile;
    }

    private static String getExtensionFromUri(Context context, Uri uri) {
        String extension = null;

        // content://
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType != null) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            }
        }

        // file:// 或兜底
        if (extension == null) {
            String path = uri.getPath();
            if (path != null) {
                int dot = path.lastIndexOf('.');
                if (dot >= 0) {
                    extension = path.substring(dot + 1);
                }
            }
        }

        if (extension == null) {
            extension = "tmp";
        }

        return extension.toLowerCase();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
