package com.suchi.musicisland.Service;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.CommandButton;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.suchi.musicisland.Utils.ExoPlayerManager;

@UnstableApi
public class MusicPlaybackService extends MediaSessionService {
    private MediaSession mediaSession = null;
    private ExoPlayerManager exoPlayerManager;

    CommandButton skipNextButton;

    ForwardingPlayer forwardingPlayer;
    ExoPlayer player;


    @Override
    public void onCreate() {
        super.onCreate();
        exoPlayerManager = ExoPlayerManager.getInstance();

        player = exoPlayerManager.getPlayer();

        //createButton();

        //interceptPlayer();

        mediaSession = new MediaSession.Builder(this, player).build();
    }

    private void createButton() {
        skipNextButton = new CommandButton.Builder(CommandButton.ICON_NEXT)
                        .setPlayerCommand(ExoPlayer.COMMAND_SEEK_TO_NEXT)
                        .setDisplayName("COMMAND_SEEK_TO_NEXT")
                        .setSlots(CommandButton.SLOT_FORWARD)
                        .build();
    }

    private void interceptPlayer() {
        forwardingPlayer = new ForwardingPlayer(player) {

            @Override
            public void seekToPrevious() {
                exoPlayerManager.skipPrevious();
            }

            @Override
            public void seekToNext() {
                exoPlayerManager.skipNext();
            }
        };
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        pauseAllPlayersAndStopSelf();
    }

    @Override
    public void onDestroy() {
        exoPlayerManager.release();
        mediaSession.release();
        mediaSession = null;
        super.onDestroy();
    }
}