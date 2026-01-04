package com.suchi.musicisland.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.suchi.musicisland.Album;
import com.suchi.musicisland.MusicDBHelper;
import com.suchi.musicisland.R;

import java.util.Objects;

public class AlbumAdapter
        extends ListAdapter<Album, AlbumAdapter.AlbumViewHolder> {

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
        void onAlbumLongPressed(Album album, View view);
    }

    private final Context context;
    private final OnAlbumClickListener listener;

    public AlbumAdapter(Context context, OnAlbumClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
    }

    //===== DiffUtil=====
    private static final DiffUtil.ItemCallback<Album> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Album>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull Album oldItem,
                        @NonNull Album newItem
                ) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Album oldItem,
                        @NonNull Album newItem
                ) {
                    return Objects.equals(oldItem.getAlbumName(), newItem.getAlbumName())
                            && Objects.equals(oldItem.getCoverUri(), newItem.getCoverUri())
                            && MusicDBHelper.getAlbumSongCount(oldItem.getId())
                            == MusicDBHelper.getAlbumSongCount(newItem.getId());
                }
            };

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull AlbumViewHolder holder,
            int position
    ) {
        Album album = getItem(position);
        holder.bind(album);
    }

    // ===== ViewHolder =====
    class AlbumViewHolder extends RecyclerView.ViewHolder {

        ImageView cover;
        TextView albumName;
        TextView albumArtist;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.ImgCover);
            albumName = itemView.findViewById(R.id.AlbumName);
            albumArtist = itemView.findViewById(R.id.AlbumArtist);
        }

        void bind(Album album) {
            albumName.setText(album.getAlbumName());
            albumArtist.setText(album.getArtist());

            // 你原来的封面加载逻辑
            Glide.with(context)
                    .load(album.getCoverUri())
                    .transform(new RoundedCorners(20)) //原角半径
                    .into(cover);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAlbumClick(album);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onAlbumLongPressed(album, v);
                }
                return true;
            });
        }
    }
}




