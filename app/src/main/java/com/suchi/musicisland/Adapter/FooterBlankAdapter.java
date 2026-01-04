package com.suchi.musicisland.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.suchi.musicisland.R;

import org.jetbrains.annotations.NotNull;

public class FooterBlankAdapter extends RecyclerView.Adapter<FooterBlankAdapter.FooterVH> {

    @NotNull
    @Override
    public FooterVH onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new FooterVH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_footer_blank, parent, false));
    }

    @Override
    public void onBindViewHolder(@NotNull FooterVH holder, int position) {}

    @Override
    public int getItemCount() {
        return 1;
    }

    static class FooterVH extends RecyclerView.ViewHolder {
        public FooterVH(View itemView) {
            super(itemView);
        }
    }
}

