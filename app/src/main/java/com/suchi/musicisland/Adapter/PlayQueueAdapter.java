package com.suchi.musicisland.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.R;
import com.suchi.musicisland.Song;
import com.suchi.musicisland.Utils.ExoPlayerManager.QueueItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlayQueueAdapter extends RecyclerView.Adapter<PlayQueueAdapter.ViewHolder> {
    private final List<QueueItem> queueItems;
    private final OnSongQueueClickedListener listener;

    public interface OnSongQueueClickedListener {
        void onSongClick(QueueItem item);
    }

    public PlayQueueAdapter(List<QueueItem> queueItems, OnSongQueueClickedListener listener) {
        this.queueItems = queueItems;
        this.listener = listener;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_play_queue, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        holder.tvSongName.setText(queueItems.get(position).song.getTitle());
        holder.tvArtist.setText(queueItems.get(position).song.getSongArtist());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSongClick(queueItems.get(holder.getBindingAdapterPosition()));
        });
    }

    @Override
    public int getItemCount() {
        return queueItems == null ? 0 : queueItems.size();
    }

    public Song getItem(int position) {
        if (queueItems == null || position < 0 || position >= queueItems.size()) {
            return null;
        }
        return queueItems.get(position).song;
    }

    public void removeAt(int position) {
        if (queueItems == null || position < 0 || position >= queueItems.size()) return;

        queueItems.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount() - position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvSongName;
        TextView tvArtist;

        ViewHolder(@NotNull View itemView) {
            super(itemView);
            tvSongName = itemView.findViewById(R.id.song_name_playList);
            tvArtist = itemView.findViewById(R.id.song_artist_playList);
        }
    }
}
