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

public class UserList extends AppCompatActivity {


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
        actions = new ArrayList<>();
        actions.add(new ActionItem("Open the camera", 0, R.drawable.camera));
        actions.add(new ActionItem("Turn on the flashlight", 0, R.drawable.ic_flashlight));
        actions.add(new ActionItem("Take a screenshot", 0, R.drawable.ic_screenshot));
        actions.add(new ActionItem("Record audio", 0, R.drawable.ic_microphone));
        actions.add(new ActionItem("Open the email box", 0, R.drawable.ic_email));
        actions.add(new ActionItem("Turn off the screen", 0, R.drawable.ic_screen_off));
        actions.add(new ActionItem("Send SMS", 0, R.drawable.ic_sms));

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
                Intent i = new Intent(UserList.this, CardActivityActivity.class);
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
                openCamera();
                break;
            case 1:
                toggleFlashlight();
                break;
            case 2:
                takeScreenshot();
                break;
            case 3:
                toggleWiFi();
                break;
            case 4:
                recordAudio();
                break;
            case 5:
                openEmail();
                break;
            case 6:
                sendSMS();
                break;


            default:
                Toast.makeText(this, "Action non définie", Toast.LENGTH_SHORT).show();
        }
    }


    //Camera
    private void openCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Aucune application caméra trouvée", Toast.LENGTH_SHORT).show();
        }
    }

    //Torche

    private boolean isTorchOn = false;

    private void toggleFlashlight() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            isTorchOn = !isTorchOn;
            cameraManager.setTorchMode(cameraId, isTorchOn);
        } catch (Exception e) {
            Toast.makeText(this, "Erreur : Impossible de gérer la lampe torche", Toast.LENGTH_SHORT).show();
        }
    }


    //Screenshot
    private void takeScreenshot() {
        if (mProjectionManager == null) {
            mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }

        if (mProjectionManager != null) {
            Intent captureIntent = mProjectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, SCREENSHOT_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Capture d'écran non supportée", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCREENSHOT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
                mImageReader = ImageReader.newInstance(
                        getResources().getDisplayMetrics().widthPixels,
                        getResources().getDisplayMetrics().heightPixels,
                        PixelFormat.RGBA_8888, 2);
                mVirtualDisplay = mMediaProjection.createVirtualDisplay("Screenshot",
                        getResources().getDisplayMetrics().widthPixels,
                        getResources().getDisplayMetrics().heightPixels,
                        getResources().getDisplayMetrics().densityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mImageReader.getSurface(), null, null);

                mImageReader.setOnImageAvailableListener(reader -> {
                    try (Image image = reader.acquireNextImage()) {
                        saveScreenshot(image);
                    }
                }, handler);
            } else {
                Toast.makeText(this, "Capture d'écran refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveScreenshot(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        File screenshotFile = new File(getExternalFilesDir(null), "screenshot.png");
        try (FileOutputStream fos = new FileOutputStream(screenshotFile)) {
            fos.write(bytes);
            Toast.makeText(this, "Capture d'écran enregistrée : " + screenshotFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Erreur lors de l'enregistrement de la capture d'écran", Toast.LENGTH_SHORT).show();
        }
    }


    //WIFI
    private void toggleWiFi() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            boolean isEnabled = wifiManager.isWifiEnabled();
            wifiManager.setWifiEnabled(!isEnabled);
            Toast.makeText(this, "Wi-Fi " + (isEnabled ? "désactivé" : "activé"), Toast.LENGTH_SHORT).show();
        }
    }



    //Email

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




