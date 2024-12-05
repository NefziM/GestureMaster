package com.example.gesturemaster;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class yoga extends AppCompatActivity implements SensorEventListener { // Implémentation de SensorEventListener
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private int shakeCount = 0;
    private TextView textView;

    // Seuil pour détecter un "frottement"
    private static final float SHAKE_THRESHOLD = 15.0f;

    // Dernière mesure de temps pour éviter plusieurs secousses d'affilée
    private long lastShakeTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_yoga);

        textView = findViewById(R.id.textView);
        textView.setText(String.valueOf(shakeCount)); // Initialise le compteur à 0

        // Initialisation du gestionnaire de capteurs
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Écouteur de capteur
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Ajout du listener pour le bouton
        Button resetButton = findViewById(R.id.button2);
        resetButton.setOnClickListener(v -> {
            // Réinitialisation du compteur
            shakeCount = 0;
            textView.setText(String.valueOf(shakeCount)); // Mise à jour du TextView
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Inscription au capteur d'accéléromètre
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Désinscription du capteur pour économiser la batterie
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Calcul de la force G
            double gForce = Math.sqrt(x * x + y * y + z * z);
            long currentTime = System.currentTimeMillis();

            if (gForce > SHAKE_THRESHOLD) {
                // Vérifie si suffisamment de temps s'est écoulé depuis la dernière secousse
                if (currentTime - lastShakeTime > 500) { // 500ms entre chaque secousse
                    lastShakeTime = currentTime;
                    shakeCount++;
                    textView.setText(String.valueOf(shakeCount)); // Met à jour le TextView
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Pas utilisé dans ce cas
    }
}
