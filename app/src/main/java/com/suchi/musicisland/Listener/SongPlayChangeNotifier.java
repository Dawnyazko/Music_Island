package com.suchi.musicisland.Listener;

import java.util.ArrayList;
import java.util.List;

public class SongPlayChangeNotifier {
    private final List<OnSongPlayerChangedListener> listeners = new ArrayList<>();
    private static SongPlayChangeNotifier instance;

    public interface OnSongPlayerChangedListener {
        void onPlayerStateChanged();
    }

    public static SongPlayChangeNotifier getInstance() {
        if (instance == null) instance = new SongPlayChangeNotifier();
        return instance;
    }

    public void addListener(OnSongPlayerChangedListener listener) {
        listeners.add(listener);
    }

    public void removeListener(OnSongPlayerChangedListener listener) {
        listeners.remove(listener);
    }

    public void notifyUIChanged() {
        for (OnSongPlayerChangedListener listener : listeners) {
            listener.onPlayerStateChanged();
        }
    }
}
