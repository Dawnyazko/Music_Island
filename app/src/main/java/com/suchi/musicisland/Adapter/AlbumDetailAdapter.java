package com.suchi.musicisland.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.suchi.musicisland.Album;
import com.suchi.musicisland.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class     AlbumDetailAdapter extends RecyclerView.Adapter<AlbumDetailAdapter.ViewHolder> {
    private Context context;
    private List<Album> albumList;

    public AlbumDetailAdapter(Context context, List<Album> albumList) {
        this.context = context;
        this.albumList = albumList;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_album_detail, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        Album album = albumList.get(position);

        holder.tvAlbumName.setText(album.getAlbumName());
        holder.tvAlbumArtist.setText(album.getArtist());

        //封面加载
        Glide.with(context)
                .load(album.getCoverUri())
                .transform(new RoundedCorners(20))
                .into(holder.imgCover);
    }

    public int getItemCount() {
        return 1;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvAlbumName;
        TextView tvAlbumArtist;

        public ViewHolder(@NotNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgCover_detail);
            tvAlbumName = itemView.findViewById(R.id.albumName_detail);
            tvAlbumArtist = itemView.findViewById(R.id.albumArtist_detail);
        }
    }
}
