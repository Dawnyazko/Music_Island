package com.suchi.musicisland.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.R;

import org.jetbrains.annotations.NotNull;

public class HeaderPlayListAdapter extends RecyclerView.Adapter<HeaderPlayListAdapter.HeaderVH> {
    private final String title;

    public HeaderPlayListAdapter(String title) {
        this.title = title;
    }

    @NotNull
    @Override
    public HeaderVH onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header_playlist, parent, false);

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
            tvTitle = itemView.findViewById(R.id.tvHeaderTitle_playList);
        }
    }
}
