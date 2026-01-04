package com.suchi.musicisland.Listener;

import java.util.HashSet;
import java.util.Set;

public class SongLikeChangeNotifier {
    private final Set<OnSongLikeChangedListener> listeners = new HashSet<>();
    private static SongLikeChangeNotifier instance;


    public interface OnSongLikeChangedListener {
        void onSongLikeChanged(long songId, boolean isLike);
    }

    public static SongLikeChangeNotifier getInstance() {
        if (instance == null) instance = new SongLikeChangeNotifier();
        return instance;
    }

    public void addListener(OnSongLikeChangedListener listener) {
        listeners.add(listener);
    }

    public void removeListener(OnSongLikeChangedListener listener) {
        listeners.remove(listener);
    }

    public void notifySongLikeChanged(long songId, boolean isLike) {
        for (OnSongLikeChangedListener listener : listeners) {
            listener.onSongLikeChanged(songId, isLike);
        }
    }

}
