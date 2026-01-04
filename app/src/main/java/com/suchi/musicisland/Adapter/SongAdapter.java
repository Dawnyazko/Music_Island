package com.suchi.musicisland.Adapter;

import static com.suchi.musicisland.Utils.Util.dpToPx;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.R;
import com.suchi.musicisland.Song;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder>{
    private List<Song> songList;
    private Context context;
    private OnSongClickListener listener;
    public static int lastTouchX = 0;
    public static int lastTouchY = 0;

    public interface OnSongClickListener {
        void onSongClick(Song song);
        void onSongLongPressed(Song song, View view);

    }

    public SongAdapter (Context context, List<Song> songList, OnSongClickListener listener) {
        this.context = context;
        this.songList = songList;
        this.listener = listener;
    }

    @NotNull
    @Override
    public SongViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);

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

        //为最后一首歌加长分割线
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) holder.divider.getLayoutParams();

        if (isLast) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            lp.width = dpToPx(context, 334);
        }

        holder.divider.setLayoutParams(lp);
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvSongName;
        TextView tvArtist;
        TextView tvSongDuration;
        ImageView tvIsLikeSong;
        TextView tvSongTrackNum;
        View divider;

        public SongViewHolder(@NotNull View itemView) {
            super(itemView);
            tvSongName = itemView.findViewById(R.id.song_name);
            tvArtist = itemView.findViewById(R.id.song_artist);
            tvSongDuration = itemView.findViewById(R.id.song_duration);
            tvIsLikeSong = itemView.findViewById(R.id.like_icon_song);
            tvSongTrackNum = itemView.findViewById(R.id.song_trackNum);
            divider = itemView.findViewById(R.id.divider);
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
