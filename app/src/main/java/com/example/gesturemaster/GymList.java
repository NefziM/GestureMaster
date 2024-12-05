package com.example.gesturemaster;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;

public class GymList extends AppCompatActivity {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private int frottementCounter = 0;
    private long lastUpdate = 0;
    private static final int SHAKE_THRESHOLD = 800;

    private ListView listView;
    private ActionAdapter adapter;
    private ArrayList<ActionItem> actions;
    private HashMap<Integer, Integer> frottementActions;
    private int frottementCount = 0;
    private Handler handler = new Handler();
    private Runnable frottementRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frottementActions = new HashMap<>();
        actions = new ArrayList<>();
        actions.add(new ActionItem("Turn Off The Screen", 0, R.drawable.ic_screen_off));
        actions.add(new ActionItem("Play Favorite Music", 0, R.drawable.ic_music));
        actions.add(new ActionItem("Launch An Application", 0, R.drawable.launch));
        actions.add(new ActionItem("Squat", 0, R.drawable.squat));
        actions.add(new ActionItem("Yoga", 0, R.drawable.yoga));
        actions.add(new ActionItem("Run", 0, R.drawable.run));

        listView = findViewById(R.id.list_actions);
        adapter = new ActionAdapter(this, actions);
        listView.setAdapter(adapter);

        // Gestion des clics sur la liste
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 3) {
                // Si un des éléments Squat, Yoga ou Course est cliqué, on redirige vers l'activité correspondante
                switch (position) {
                    case 3:
                        Intent squatIntent = new Intent(GymList.this, squat.class);
                        startActivity(squatIntent);
                        break;
                    case 4:
                        Intent yogaIntent = new Intent(GymList.this, yoga.class);
                        startActivity(yogaIntent);
                        break;
                    case 5:
                        Intent courseIntent = new Intent(GymList.this, Course.class);
                        startActivity(courseIntent);
                        break;
                }
            } else {
                // Sinon, afficher le dialogue pour configurer les frottements
                showEditDialog(position);
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        checkPermissions();
    }

    private void checkPermissions() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.SEND_SMS, Manifest.permission.READ_EXTERNAL_STORAGE};
        ArrayList<String> permissionsToRequest = new ArrayList<>();

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 1);
        }
    }

    private void showKeyguard(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null) {
            KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("MyApp");
            keyguardLock.disableKeyguard(); // Désactive temporairement l'écran de verrouillage
        }
        Toast.makeText(this, "ok", Toast.LENGTH_SHORT).show();
    }

    private void onFrottementDetected() {
        frottementCount++;

        if (frottementRunnable != null) {
            handler.removeCallbacks(frottementRunnable);
        }

        frottementRunnable = () -> {
            executeActionBasedOnFrottementCount();
            frottementCount = 0;
        };

        handler.postDelayed(frottementRunnable, 1000);
    }

    private void executeActionBasedOnFrottementCount() {
        if (frottementActions.containsKey(frottementCount)) {
            int actionPosition = frottementActions.get(frottementCount);
            performAction(actionPosition);
        } else {
            Toast.makeText(this, "Aucune action assignée pour " + frottementCount + " frottements", Toast.LENGTH_SHORT).show();
        }
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long currentTime = System.currentTimeMillis();
                long diffTime = currentTime - lastUpdate;

                if (diffTime > 100) {
                    lastUpdate = currentTime;

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    float acceleration = (x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH;

                    if (acceleration > SHAKE_THRESHOLD) {
                        onFrottementDetected();
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private void performAction(int position) {
        switch (position) {
            case 0:
                Context context = this;
                showKeyguard(context);
                break;
            case 1:
                playFavoriteMusic();
                break;
            case 2:
                launchApp();
                break;
            default:
                Toast.makeText(this, "Action non définie", Toast.LENGTH_SHORT).show();
        }
    }

    private void playFavoriteMusic() {
        // Logique pour jouer la musique favorite
        Toast.makeText(this, "Jouer la musique favorite", Toast.LENGTH_SHORT).show();
    }

    private void launchApp() {
        // Créer un Intent pour afficher une liste des applications compatibles
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setType("application/pdf");  // Remplacez par le type MIME que vous souhaitez explorer
        Intent chooser = Intent.createChooser(intent, "Choisissez une application pour afficher le fichier");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser); // Lancer l'intent et laisser l'utilisateur choisir l'application
        } else {
            Toast.makeText(this, "Aucune application disponible pour ouvrir ce type de contenu", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configurer les frottements pour : " + actions.get(position).getTitle());

        String[] options = {"1 frottement", "2 frottements", "3 frottements", "4 frottements"};
        builder.setItems(options, (dialog, which) -> {
            int selectedFrottements = which + 1;
            if (frottementActions.containsKey(selectedFrottements)) {
                int existingPosition = frottementActions.get(selectedFrottements);
                actions.get(existingPosition).setFrottements(0);
            }
            frottementActions.put(selectedFrottements, position);
            actions.get(position).setFrottements(selectedFrottements);
            adapter.notifyDataSetChanged();
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }
}
