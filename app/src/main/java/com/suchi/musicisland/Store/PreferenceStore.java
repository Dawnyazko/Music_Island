package com.suchi.musicisland.Store;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.media3.common.Player;

public class PreferenceStore {
    private static final String PREF_NAME = "MusicIslandPreferences";
    private static final String KEY_SORT_METHOD = "sort_method";

    public static final String SORT_RECENT = "添加日期";
    public static final String SORT_ALPHABETICAL = "字母顺序";
    public static final String DEFAULT_SORT = SORT_RECENT;

    private static final String PREF = "player_pref";
    private static final String KEY_REPEAT = "repeat_mode";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public PreferenceStore(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveSortMethod(String sortMethod) {
        editor.putString(KEY_SORT_METHOD, sortMethod);
        editor.apply();
    }

    public String getSortMethod() {
        return sharedPreferences.getString(KEY_SORT_METHOD, DEFAULT_SORT);
    }

    public static void savePlayMode(Context context, int mode) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_REPEAT, mode)
                .apply();
    }

    public static int loadPlayMode(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getInt(KEY_REPEAT, Player.REPEAT_MODE_OFF);
    }

}
