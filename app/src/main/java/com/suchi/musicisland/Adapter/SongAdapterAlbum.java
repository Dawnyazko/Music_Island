package com.suchi.musicisland.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.R;
import com.suchi.musicisland.Song;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SongAdapterAlbum extends RecyclerView.Adapter<SongAdapterAlbum.SongViewHolder>{
    private List<Song> songList;
    private Context context;
    private OnSongClickListener listener;
    public static int lastTouchX = 0;
    public static int lastTouchY = 0;

    public interface OnSongClickListener {
        void onSongClick(Song song);
        void onSongLongPressed(Song song, View view);

    }

    public SongAdapterAlbum(Context context, List<Song> songList, OnSongClickListener listener) {
        this.context = context;
        this.songList = songList;
        this.listener = listener;
    }

    @NotNull
    @Override
    public SongViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_song_album, parent, false);

        return new SongViewHolder(v);
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    @Override
    public void onBindViewHolder(@NotNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        boolean isLast = position == getItemCount() - 1;
        boolean isFirst = position == 0;

        holder.tvSongName.setText(song.getTitle());
        holder.tvArtist.setText(song.getSongArtist());
        holder.tvSongDuration.setText(formatDuration(song.getDurationMs()));
        holder.tvSongTrackNum.setText(String.valueOf(song.getTrackNum()));

        //为已喜爱的歌曲加上favorite,写else是为了防止recyclerView的复用机制，导致图标出现在别的歌上
        if (song.getIsLiked() == true) {
            holder.tvIsLikeSong.setImageResource(R.drawable.solid_favorite_icon);
        } else {
            holder.tvIsLikeSong.setImageResource(R.drawable.blank);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSongClick(song);
        });

        holder.itemView.setOnLongClickListener(v ->  {
            if (listener != null) listener.onSongLongPressed(song, v);
            return true;
        });

        holder.itemView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lastTouchX = (int) event.getRawX();
                lastTouchY = (int) event.getRawY();
            }
            return false;
        });

        if (getItemCount() == 1) {
            holder.tvBgSong.setBackgroundResource(R.drawable.bg_pill_white_single);
            holder.tvBgSong.setForeground(ContextCompat.getDrawable(context, R.drawable.bg_selected_single));
        } else {
            if (isFirst) {
                holder.tvBgSong.setBackgroundResource(R.drawable.bg_pill_white_first);
                holder.tvBgSong.setForeground(ContextCompat.getDrawable(context, R.drawable.bg_selected_first));
            } else if (isLast) {
                holder.tvBgSong.setBackgroundResource(R.drawable.bg_pill_white_last);
                holder.tvBgSong.setForeground(ContextCompat.getDrawable(context, R.drawable.bg_selected_last));
            } else {
                holder.tvBgSong.setBackgroundResource(R.drawable.bg_pill_white);
                holder.tvBgSong.setForeground(ContextCompat.getDrawable(context, R.drawable.bg_selected));
            }
        }
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvSongName;
        TextView tvArtist;
        TextView tvSongDuration;
        ImageView tvIsLikeSong;
        TextView tvSongTrackNum;
        View tvBgSong;

        public SongViewHolder(@NotNull View itemView) {
            super(itemView);
            tvSongName = itemView.findViewById(R.id.song_name);
            tvArtist = itemView.findViewById(R.id.song_artist);
            tvSongDuration = itemView.findViewById(R.id.song_duration);
            tvIsLikeSong = itemView.findViewById(R.id.like_icon_song);
            tvSongTrackNum = itemView.findViewById(R.id.song_trackNum);
            tvBgSong = itemView.findViewById(R.id.bg_song);
        }
    }

    public void removeAt(int position) {
        songList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount() - position);
    }


    //把long格式的歌曲长度转换为分秒格式
    public static String formatDuration(long durationMs) {
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }
}
