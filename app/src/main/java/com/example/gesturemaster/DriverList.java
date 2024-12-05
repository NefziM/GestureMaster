package com.example.gesturemaster;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class DriverList extends AppCompatActivity {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;
    private static final int SHAKE_THRESHOLD = 800;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private MediaPlayer mediaPlayer;
    private ListView listView;
    private ActionAdapter adapter;
    private ArrayList<ActionItem> actions;
    private HashMap<Integer, Integer> frottementActions;
    private int frottementCount = 0;
    private Handler handler = new Handler();
    private Runnable frottementRunnable;
    private static final int SCREENSHOT_REQUEST_CODE = 1000;
    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private AudioManager audioManager;
    private MediaRecorder mediaRecorder;
    private String outputFilePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        frottementActions = new HashMap<>();
        actions = new ArrayList<>();

        // Initialisation des actions
        actions.add(new ActionItem("Answer the call", 0, R.drawable.ic_call));
        actions.add(new ActionItem("Play Favorite Music", 1, R.drawable.ic_music));
        actions.add(new ActionItem("Launch App", 2, R.drawable.launch));
        actions.add(new ActionItem("Open Google Maps", 3, R.drawable.ic_maps));
        actions.add(new ActionItem("Send SMS", 4, R.drawable.ic_sms));

        listView = findViewById(R.id.list_actions);
        adapter = new ActionAdapter(this, actions);
        listView.setAdapter(adapter);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        listView.setOnItemClickListener((parent, view, position, id) -> showEditDialog(position));
        checkPermissions();
        ImageView backIcon = findViewById(R.id.back_icon);
        backIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DriverList.this, CardActivityActivity.class);
                startActivity(i);
            }
        });
    }
    private void checkPermissions() {
        // Liste des permissions nécessaires
        String[] permissions = {
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CALL_PHONE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.ACCESS_NETWORK_STATE,
                android.Manifest.permission.CHANGE_WIFI_STATE,
                android.Manifest.permission.WRITE_SETTINGS,
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                android.Manifest.permission.ACCESS_WIFI_STATE,
                android.Manifest.permission.MANAGE_OWN_CALLS,
                android.Manifest.permission.SYSTEM_ALERT_WINDOW,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
        };

        // Liste des permissions à demander
        ArrayList<String> permissionsToRequest = new ArrayList<>();

        // Vérification de chaque permission
        for (String permission : permissions) {
            // Si la permission n'est pas encore accordée
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        // Si des permissions doivent être demandées
        if (!permissionsToRequest.isEmpty()) {
            // Demander les permissions manquantes
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 1);
        }
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
                answerCall();
                break;
            case 1:
                playMusic();
                break;
            case 2:
                launchApp();
                break;
            case 3:
                openGoogleMaps();
                break;
            case 4:
                sendSMS();
                break;
            default:
                Toast.makeText(this, "Action non définie", Toast.LENGTH_SHORT).show();
        }
    }

    private void answerCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Fonctionnalité à implémenter : répondre à un appel", Toast.LENGTH_SHORT).show();
            // Implémentez le code pour répondre à un appel ici
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ANSWER_PHONE_CALLS}, PERMISSION_REQUEST_CODE);
        }
    }



    private void openGoogleMaps() {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps non installé", Toast.LENGTH_SHORT).show();
        }
    }

    //music
    private void playMusic() {
        // Vérifie les permissions pour lire le stockage externe
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            return;
        }

        ArrayList<String> musicPaths = new ArrayList<>();
        ArrayList<String> musicTitles = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA
        };

        // Récupère les musiques depuis le stockage externe
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    musicTitles.add(title);
                    musicPaths.add(path);
                    Log.d("MusicPath", "Path: " + path);  // Déboguer les chemins de fichiers
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (musicTitles.isEmpty()) {
            Toast.makeText(this, "Aucune musique trouvée", Toast.LENGTH_SHORT).show();
        } else {
            // Affiche un dialog pour sélectionner une musique
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Sélectionnez une musique");

            builder.setItems(musicTitles.toArray(new String[0]), (dialog, which) -> {
                String selectedPath = musicPaths.get(which);
                Log.d("SelectedMusic", "Selected path: " + selectedPath);  // Vérifiez le chemin sélectionné
                playSelectedMusic(selectedPath);
            });

            builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());
            builder.show();
        }
    }

    private void playSelectedMusic(String path) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();

            Toast.makeText(this, "Lecture : " + new File(path).getName(), Toast.LENGTH_SHORT).show();

            // Arrêt automatique de la musique lors de la fin
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                Toast.makeText(this, "Musique terminée", Toast.LENGTH_SHORT).show();
            });
        } catch (IOException e) {
            Toast.makeText(this, "Erreur lors de la lecture du fichier audio", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }








    private void openEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        startActivity(Intent.createChooser(emailIntent, "Choisir une application email"));
    }






    //SMS
    private void sendSMS() {
        String phoneNumber = "1234567890";
        String message = "Test de l'application SMS";

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "SMS envoyé", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission SMS non accordée", Toast.LENGTH_SHORT).show();
        }
    }


    //Lancer App
    private void launchApp() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.example.app");
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Toast.makeText(this, "Application non trouvée", Toast.LENGTH_SHORT).show();
        }
    }



    //Record audio
    private void recordAudio() {
        // Vérification des permissions d'enregistrement audio
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission d'enregistrement audio refusée", Toast.LENGTH_SHORT).show();
            return; // Retourner si la permission est refusée
        }

        // Vérification de la permission d'accès au stockage
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission d'accès au stockage refusée", Toast.LENGTH_SHORT).show();
            return; // Retourner si la permission est refusée
        }

        // Définir le chemin de sortie pour le fichier audio enregistré
        outputFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audioRecording.3gp";

        // Initialiser le MediaRecorder
        mediaRecorder = new MediaRecorder();
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // Utiliser le microphone pour l'enregistrement
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); // Format de sortie (ici 3gp)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); // Encoder audio (AMR_NB)
            mediaRecorder.setOutputFile(outputFilePath); // Spécifier le fichier de sortie

            // Préparer le MediaRecorder
            mediaRecorder.prepare();
            // Démarrer l'enregistrement
            mediaRecorder.start();
            Toast.makeText(this, "Enregistrement commencé...", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            // Log de l'erreur avec un message précis et un Toast générique
            Log.e("AudioRecord", "Erreur d'enregistrement : " + e.getMessage());
            Toast.makeText(this, "Erreur d'enregistrement", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Log d'une erreur inattendue et affichage d'un Toast générique
            Log.e("AudioRecord", "Erreur inattendue : " + e.getMessage());
            Toast.makeText(this, "Erreur d'enregistrement", Toast.LENGTH_SHORT).show();
        }
    }



    //Dialogue
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




