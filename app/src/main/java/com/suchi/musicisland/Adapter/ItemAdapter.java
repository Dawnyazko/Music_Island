package com.suchi.musicisland.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.Listener.SongPlayChangeNotifier;
import com.suchi.musicisland.R;
import com.suchi.musicisland.Utils.ExoPlayerManager;

import org.jetbrains.annotations.NotNull;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> implements SongPlayChangeNotifier.OnSongPlayerChangedListener{
    @Override
    public void onPlayerStateChanged() {
        notifyItemChanged(0);
    }

    public interface OnButtonClickListener {
        void onPlayBtnClick(View v);
        void onMenuBtnClick(View v);
        void onPinBtnClick(View v);
    }

    private OnButtonClickListener listener;

    public ItemAdapter(OnButtonClickListener listener) {
        this.listener = listener;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album_play, parent, false));
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        //更新按钮图标
        boolean isPlaying = ExoPlayerManager.getInstance().isPlaying();

        holder.playButton.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow_icon);

        holder.playButton.setOnClickListener(v -> {
            holder.playButton.animate().scaleX(0.75f).scaleY(0.75f).setDuration(80)
                    .withEndAction(() -> holder.playButton.animate().scaleX(1f).scaleY(1f).setDuration(80)
                            .withEndAction(() -> {
                                if (listener != null) {
                                    listener.onPlayBtnClick(v);
                                }
                            })
                    );
        });
        holder.menuButton.setOnClickListener(v -> {
            holder.menuButton.animate().scaleX(0.75f).scaleY(0.75f).setDuration(80)
                    .withEndAction(() -> holder.menuButton.animate().scaleX(1f).scaleY(1f).setDuration(80)
                            .withEndAction(() -> {
                                if (listener != null) {
                                    listener.onMenuBtnClick(v);
                                }
                            })
                    );

        });
        holder.pinButton.setOnClickListener(v -> {
            holder.pinButton.animate().scaleX(0.75f).scaleY(0.75f).setDuration(80)
                    .withEndAction(() -> holder.pinButton.animate().scaleX(1f).scaleY(1f).setDuration(80)
                            .withEndAction(() -> {
                                if (listener != null) {
                                    listener.onPinBtnClick(v);
                                }
                            })
                    );

        });
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageButton playButton;
        ImageButton menuButton;
        ImageButton pinButton;

        public ViewHolder(View itemView) {
            super(itemView);
            playButton = itemView.findViewById(R.id.icon_play_album);
            menuButton = itemView.findViewById(R.id.icon_menu_album);
            pinButton = itemView.findViewById(R.id.icon_pin_album);
        }
    }
}
