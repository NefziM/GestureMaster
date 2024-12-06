package com.example.gesturemaster;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    private final List<File> musicFiles;
    private final OnMusicClickListener listener;

    public interface OnMusicClickListener {
        void onMusicClick(File file);
    }

    public MusicAdapter(List<File> musicFiles, OnMusicClickListener listener) {
        this.musicFiles = musicFiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.music_item, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        File file = musicFiles.get(position);
        holder.title.setText(file.getName());
        holder.itemView.setOnClickListener(v -> listener.onMusicClick(file));
    }

    @Override
    public int getItemCount() {
        return musicFiles.size();
    }

    public static class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.musicTitle);
        }
    }
}
