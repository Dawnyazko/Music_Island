package com.suchi.musicisland.Listener;

import java.util.ArrayList;
import java.util.List;

public class MainUIChangeNotifier {
    private final List<OnMainUIChangedListener> listeners = new ArrayList<>();
    private static MainUIChangeNotifier instance;

    public interface OnMainUIChangedListener {
        void onUIStateChanged();
    }

    public static MainUIChangeNotifier getInstance() {
        if (instance == null) instance = new MainUIChangeNotifier();
        return instance;
    }

    public void addListener(OnMainUIChangedListener listener) {listeners.add(listener);}

    public void removeListener(OnMainUIChangedListener listener) {listeners.remove(listener);}

    public void notifyUIChanged() {
        for (OnMainUIChangedListener listener : listeners) {
            listener.onUIStateChanged();
        }
    }
}
