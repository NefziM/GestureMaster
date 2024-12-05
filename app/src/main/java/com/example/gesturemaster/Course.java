package com.example.gesturemaster;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Course extends AppCompatActivity implements SensorEventListener {

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
        setContentView(R.layout.activity_course);

        // Initialisation du SensorManager et de l'accéléromètre
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Référencement du TextView
        textView = findViewById(R.id.textView); // Assurez-vous que l'ID correspond à celui dans votre fichier XML

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

            // Calcul de la force G
            double gForce = Math.sqrt(x * x + y * y + z * z);
            long currentTime = System.currentTimeMillis();

            if (gForce > SHAKE_THRESHOLD) {
                // Vérifie si suffisamment de temps s'est écoulé depuis la dernière secousse
                if (currentTime - lastShakeTime > 500) { // 500ms entre chaque secousse
                    lastShakeTime = currentTime;
                    shakeCount++;
                    if (textView != null) {
                        textView.setText(String.valueOf(shakeCount)); // Met à jour le TextView
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Pas utilisé dans ce cas
    }
}