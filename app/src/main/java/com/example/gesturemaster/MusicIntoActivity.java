package com.example.gesturemaster;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicIntoActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1;
    private RecyclerView recyclerView;
    private MusicAdapter adapter;
    private List<File> musicFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_into);

        recyclerView = findViewById(R.id.recyclerView1);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
            } else {
                loadMusic();
            }
        } else {
            loadMusic();
        }
    }

    private void loadMusic() {
        musicFiles = getAllMp3Files(Environment.getExternalStorageDirectory());
        if (musicFiles.isEmpty()) {
            Toast.makeText(this, "Aucune chanson trouvée", Toast.LENGTH_SHORT).show();
            return;
        }

        adapter = new MusicAdapter(musicFiles, file -> {
            // Launch MusicPlayerActivity
            Intent intent = new Intent(MusicIntoActivity.this, MusicPlayerActivity.class);
            intent.putExtra("filePath", file.getAbsolutePath());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private List<File> getAllMp3Files(File root) {
        List<File> mp3Files = new ArrayList<>();
        File[] files = root.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    mp3Files.addAll(getAllMp3Files(file));
                } else if (file.getName().endsWith(".mp3")) {
                    mp3Files.add(file);
                }
            }
        }
        return mp3Files;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusic();
            } else {
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }
}