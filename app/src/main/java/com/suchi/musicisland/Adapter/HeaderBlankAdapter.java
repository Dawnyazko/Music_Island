package com.suchi.musicisland.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.R;

import org.jetbrains.annotations.NotNull;

public class HeaderBlankAdapter extends RecyclerView.Adapter<HeaderBlankAdapter.HeaderVH> {

    @NotNull
    @Override
    public HeaderVH onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new HeaderBlankAdapter.HeaderVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_header_blank, parent, false));
    }

    @Override
    public void onBindViewHolder(@NotNull HeaderBlankAdapter.HeaderVH holder, int position) {}

    @Override
    public int getItemCount() {
        return 1;
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        public HeaderVH(View itemView) {
            super(itemView);
        }
    }
}
