package com.example.myapplication.view;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Configurar el botón para cerrar la pantalla
        findViewById(R.id.btn_cerrar).setOnClickListener(v -> finish());
    }
}