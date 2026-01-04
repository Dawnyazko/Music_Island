package com.suchi.musicisland;

import android.app.Application;

import com.suchi.musicisland.Executor.AppExecutors;
import com.suchi.musicisland.Store.SongStateStore;
import com.suchi.musicisland.Utils.ExoPlayerManager;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

import java.util.List;


public class Init extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        //Initialize DBFlow
        FlowManager.init(new FlowConfig.Builder(this).build());

        ExoPlayerManager.getInstance().initContext(this);

        AppExecutors.DB.execute(() -> {
            List<Long> likedSongIds = MusicDBHelper.getAllLikedSongIds();
            SongStateStore.init(likedSongIds);
        });
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        //clear DBFlow
        FlowManager.destroy();
    }
}
