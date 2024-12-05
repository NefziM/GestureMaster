package com.example.gesturemaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.View; // Import nécessaire pour gérer les clics
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomePageActivity extends AppCompatActivity {
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page); // Assurez-vous que le layout correct est défini ici

        // Lier le bouton au composant dans le layout
        btn = findViewById(R.id.button);

        // Définir un listener pour gérer les clics
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Rediriger vers CarActivity
                Intent i = new Intent(HomePageActivity.this, CardActivityActivity.class);
                startActivity(i);
            }
        });
    }
}