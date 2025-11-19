package com.example.myapplication.view;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;

public class FaqActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        // Configurar el botón de flecha para volver atrás
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }
}