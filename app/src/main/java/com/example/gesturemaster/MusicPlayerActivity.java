package com.example.gesturemaster;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;

public class MusicPlayerActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float SHAKE_THRESHOLD = 30.0f; // Seuil de secousse
    private long lastShakeTime = 0; // Dernière secousse
    private MediaPlayer mediaPlayer;
    private ImageButton playButton, stopButton;
    private TextView songTitle;
    private SeekBar volumeSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        // Liaison des vues
        playButton = findViewById(R.id.playButton);
        stopButton = findViewById(R.id.stopButton);
        songTitle = findViewById(R.id.songTitle);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);

        // Récupérer le chemin du fichier
        String filePath = getIntent().getStringExtra("filePath");
        if (filePath == null || filePath.isEmpty()) {
            Toast.makeText(this, "Fichier invalide", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        File songFile = new File(filePath);
        songTitle.setText(songFile.getName());

        // Initialisation du MediaPlayer
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
        } catch (IOException e) {
            Toast.makeText(this, "Erreur lors de la préparation du fichier", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
            return;
        }

        // Configurer les actions des boutons
        playButton.setOnClickListener(v -> {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                Toast.makeText(this, "Lecture en cours", Toast.LENGTH_SHORT).show();
            }
        });

        stopButton.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                try {
                    mediaPlayer.prepare();
                    Toast.makeText(this, "Musique arrêtée", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Configuration du contrôle du volume
        volumeSeekBar.setMax(100);
        volumeSeekBar.setProgress(50); // Valeur initiale
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100f;
                mediaPlayer.setVolume(volume, volume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Initialiser le gestionnaire de capteurs
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Libérer les ressources MediaPlayer
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Calculer la force G
            double gForce = Math.sqrt(x * x + y * y + z * z);
            long currentTime = System.currentTimeMillis();

            // Détecter une secousse
            if (gForce > SHAKE_THRESHOLD) {
                if (currentTime - lastShakeTime > 500) { // Limiter les secousses
                    lastShakeTime = currentTime;
                    // Arrêter la musique lors de la secousse
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        try {
                            mediaPlayer.prepare();
                            Toast.makeText(this, "Musique arrêtée par secousse", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Non utilisé ici
    }
}