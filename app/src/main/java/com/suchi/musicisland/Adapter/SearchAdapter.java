package com.suchi.musicisland.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.suchi.musicisland.Album;
import com.suchi.musicisland.R;
import com.suchi.musicisland.Song;

import org.jspecify.annotations.Nullable;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {
    private Context context;
    private List<Object> resultList;
    private OnSearchClickListener listener;
    public static int lastTouchX = 0;
    public static int lastTouchY = 0;

    public interface OnSearchClickListener {
        void onAlbumClicked(Album album);
        void onSongClicked(Song song);
        void onSongLongPressed(Song song, View view);
    }

    public SearchAdapter(Context context, List<Object> list, OnSearchClickListener listener) {
        this.context = context;
        this.resultList = list;
        this.listener = listener;
    }

    public void updateList(List<Object> newList) {
        this.resultList = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (resultList.get(position) instanceof Album) return 0;
        else return 1;
    }

    @Override
    public SearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0){
            view = LayoutInflater.from(context).inflate(R.layout.item_search_album, parent, false);
        }
        else {
            view = LayoutInflater.from(context).inflate(R.layout.item_search_song, parent, false);
        }
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SearchViewHolder holder, int position) {
        Object obj = resultList.get(position);
        boolean isLast = position == getItemCount() - 1;
        boolean isFirst = position == 0;

        //清空，防止复用错乱
        holder.bindSong(null);

        if (obj instanceof Album) {
            Album album = (Album) obj;
            holder.albumTitle.setText(album.getAlbumName());
            holder.albumArtist.setText(album.getArtist());
            holder.itemView.setOnClickListener(v -> listener.onAlbumClicked(album));

            //封面加载
            Glide.with(context)
                    .load(album.getCoverUri())
                    .transform(new RoundedCorners(14))
                    .into(holder.albumCover);
        }

        if (obj instanceof Song) {
            Song song = (Song)obj;
            //提取出歌曲
            holder.bindSong(song);
            holder.songTitle.setText(song.getTitle());
            holder.songArtist.setText(song.getSongArtist());

            if (song.getIsLiked() == true) {
                holder.isLikeSong.setImageResource(R.drawable.solid_favorite_icon);
            } else {
                holder.isLikeSong.setImageResource(R.drawable.blank);
            }

            holder.itemView.setOnClickListener(v -> listener.onSongClicked(song));
            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onSongLongPressed(song, v);
                return true;
            });
            holder.itemView.setOnTouchListener((v, event ) ->{
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    lastTouchX = (int) event.getRawX();
                    lastTouchY = (int) event.getRawY();
                }
                return false;
            });
        }

        if (getItemCount() == 1) {
            if (obj instanceof Album) {
                holder.albumBg.setBackgroundResource(R.drawable.bg_pill_white_single);
                holder.albumBg.setForeground(ContextCompat.getDrawable(context, R.drawable.bg_selected_single));
            } else {
                holder.songBg.setBackgroundResource(R.drawable.bg_pill_white_single);
                holder.songBg.setForeground(ContextCompat.getDrawable(context, R.drawable.bg_selected_single));
            }
        } else {
            if (obj instanceof Album) {
                if (isFirst) {
                    holder.albumBg.setBackgroundResource(R.drawable.bg_pill_white_first);
                    holder.albumBg.setForeground(ContextCompat.getDrawable(context, R.drawable.bg_selected_first));
                } else if (isLast) {
                    holder.albumBg.setBackgroundResource(R.drawable.bg_pill_white_last);
                    holder.albumBg.setForeground(ContextCompat.getDrawable(context, R.drawable.bg_selected_last));
                } else {
                    holder.albumBg.setBackgroundResource(R.drawable.bg_pill_white);
                    holder.albumBg.setForeground(ContextCompat.getDrawable(context, R.drawable.bg_selected));
                }
            } else {
                if (isFirst) {
                    holder.songBg.setBackgroundResource(R.drawable.bg_pill_white_first);
                    holder.songBg.setForeground(ContextCompat.getDrawable(context, R.drawable.bg_selected_first));
                } else if (isLast) {
                    holder.songBg.setBackgroundResource(R.drawable.bg_pill_white_last);
                    holder.songBg.setForeground(ContextCompat.getDrawable(context, R.drawable.bg_selected_last));
                } else {
                    holder.songBg.setBackgroundResource(R.drawable.bg_pill_white);
                    holder.songBg.setForeground(ContextCompat.getDrawable(context, R.drawable.bg_selected));
                }
            }
        }
    }

    @Override
    public int getItemCount() { return resultList.size(); }

    public void removeAt(int position) {
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount() - position);
    }

    public static class SearchViewHolder extends RecyclerView.ViewHolder {
        TextView albumTitle, albumArtist,songTitle, songArtist;
        ImageView albumCover;
        ImageView isLikeSong;
        ConstraintLayout albumBg;
        ConstraintLayout songBg;
        private Song boundSong;

        public void bindSong(Song song) {
            this.boundSong = song;
        }

        @Nullable
        public Song getSong() {
            return boundSong;
        }

        public SearchViewHolder(View itemView) {
            super(itemView);
            albumCover = itemView.findViewById(R.id.ImgCover_search);
            albumTitle = itemView.findViewById(R.id.album_name_search);
            albumArtist = itemView.findViewById(R.id.album_artist_search);
            songTitle = itemView.findViewById(R.id.song_name_search);
            songArtist = itemView.findViewById(R.id.song_artist_search);
            isLikeSong = itemView.findViewById(R.id.like_icon_search);
            albumBg = itemView.findViewById(R.id.bg_album_search);
            songBg = itemView.findViewById(R.id.bg_song_search);
        }
    }
}


