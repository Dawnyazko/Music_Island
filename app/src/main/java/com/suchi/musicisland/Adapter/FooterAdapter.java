package com.suchi.musicisland.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.R;

import org.jetbrains.annotations.NotNull;

public class FooterAdapter extends RecyclerView.Adapter<FooterAdapter.FooterVH> {
    private final long songCount;
    private final long timeCount;
    private final String copyRight;

    public FooterAdapter(long songCount, long timeCount, String copyRight) {
        this.songCount = songCount;
        this.timeCount = timeCount;
        this.copyRight = copyRight;

    }

    @NotNull
    @Override
    public FooterVH onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_footer, parent, false);

        return new FooterVH(v);
    }

    @Override
    public void onBindViewHolder(@NotNull FooterVH holder, int position) {
        holder.tvSongCount.setText(String.valueOf(songCount));
        holder.tvTimeCount.setText(String.valueOf(timeCount));
        holder.tvcopyRight.setText(copyRight);
    }

    @Override
    public int getItemCount(){
        return 1;
    }

    static class FooterVH extends RecyclerView.ViewHolder {
        TextView tvSongCount;
        TextView tvTimeCount;
        TextView tvcopyRight;

        FooterVH(@NotNull View itemView) {
            super(itemView);
            tvSongCount = itemView.findViewById(R.id.tvSongCountNum);
            tvTimeCount = itemView.findViewById(R.id.tvTimeCountNum);
            tvcopyRight = itemView.findViewById(R.id.tvCopyRight);
        }
    }
}
