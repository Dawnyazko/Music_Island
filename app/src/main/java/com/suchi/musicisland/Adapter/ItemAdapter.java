package com.suchi.musicisland.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.Listener.SongPlayChangeNotifier;
import com.suchi.musicisland.R;
import com.suchi.musicisland.Utils.ExoPlayerManager;

import org.jetbrains.annotations.NotNull;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> implements SongPlayChangeNotifier.OnSongPlayerChangedListener{

    public interface OnButtonClickListener {
        void onPlayBtnClick(View v);
        void onMenuBtnClick(View v);
        void onPinBtnClick(View v);
    }

    private OnButtonClickListener listener;
    private boolean isRegistered = false;

    public ItemAdapter(OnButtonClickListener listener) {
        this.listener = listener;

        if (!isRegistered) {
            SongPlayChangeNotifier.getInstance().addListener(this);
            isRegistered = true;
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (isRegistered) {
            SongPlayChangeNotifier.getInstance().removeListener(this);
            isRegistered = false;
        }
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album_play, parent, false));
    }

    @Override
    public void onPlayerStateChanged() {
        notifyItemChanged(0);
    }


    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        updatePlayButtonIcon(holder);

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

    private void updatePlayButtonIcon(ViewHolder holder) {
        if (holder == null) return;

        boolean isPlaying = ExoPlayerManager.getInstance().isPlaying();
        holder.playButton.setImageResource(
                isPlaying ? R.drawable.pause_icon : R.drawable.play_arrow_icon
        );
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
