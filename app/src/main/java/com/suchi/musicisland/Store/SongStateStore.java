package com.suchi.musicisland.Store;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SongStateStore {

    private static final Set<Long> likedIds =
            Collections.synchronizedSet(new HashSet<>());

    private SongStateStore() {}

    public static void init(Collection<Long> ids) {
        likedIds.clear();
        likedIds.addAll(ids);
    }

    public static boolean isLiked(long songId) {
        return likedIds.contains(songId);
    }

    public static void setLiked(long songId, boolean liked) {
        if (liked) {
            likedIds.add(songId);
        } else {
            likedIds.remove(songId);
        }
    }

    public static void removeLiked(long songId) {
        likedIds.remove(songId);
    }
}
