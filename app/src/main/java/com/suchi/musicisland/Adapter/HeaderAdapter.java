package com.suchi.musicisland.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.R;

import org.jetbrains.annotations.NotNull;

public class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.HeaderVH> {
    private final String title;
    private final boolean isFromLikeList;

    public HeaderAdapter(String title, boolean isFromLikeList) {
        this.title = title;
        this.isFromLikeList = isFromLikeList;
    }

    @NotNull
    @Override
    public HeaderVH onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View v;

        if (isFromLikeList) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header_likelist, parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header, parent, false);
        }

        return new HeaderVH(v);
    }

    @Override
    public void onBindViewHolder(@NotNull HeaderVH holder, int position) {
        holder.tvTitle.setText(title);
    }

    @Override
    public int getItemCount(){
        return 1;
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvTitle;

        HeaderVH(@NotNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvHeaderTitle);
        }
    }
}
