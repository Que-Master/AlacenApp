package com.example.myapplication.view;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;

public class GuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        TextView txtTitle = findViewById(R.id.text_title);
        TextView txtContent = findViewById(R.id.text_content);
        ImageView btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        // Recibir qué guía mostrar
        String type = getIntent().getStringExtra("GUIDE_TYPE");
        if (type == null) type = "";

        switch (type) {
            case "WIFI":
                txtTitle.setText("Configuración y Gestión WiFi");
                txtContent.setText("Sigue estos pasos para conectar y administrar tu Alacena IoT:\n\n" +
                        "⚠️ REQUISITO PREVIO: RED 2.4GHz\n" +
                        "Este dispositivo NO es compatible con redes WiFi 5G (5GHz). Asegúrate de conectarlo a tu red WiFi normal (2.4GHz) o no funcionará.\n\n" +
                        "1️⃣ INICIO\n" +
                        "Toca el ícono de WiFi 📶 en la esquina superior derecha de la pantalla principal.\n\n" +
                        "2️⃣ MODO CONFIGURACIÓN\n" +
                        "Si es la primera vez, la App te guiará. Ve a los ajustes de tu celular y conéctate a la red temporal llamada 'AlacenaSetup' (Clave: 12345678).\n\n" +
                        "3️⃣ INGRESAR DATOS\n" +
                        "Vuelve a la App. Verás dos casillas: escribe el Nombre exacto y la Contraseña de tu WiFi de casa (recuerda, la red 2.4G). Presiona 'Guardar'.\n\n" +
                        "4️⃣ REINICIO AUTOMÁTICO\n" +
                        "El dispositivo guardará los datos y se reiniciará. La luz roja debería apagarse, indicando que ya tiene internet.\n\n" +
                        "5️⃣ VER ESTADO Y GESTIONAR\n" +
                        "Una vez configurado, al tocar el mismo ícono de WiFi verás el 'Estado del Dispositivo' (IP, Señal, Red actual).\n\n" +
                        "⚠️ IMPORTANTE: LA MISMA RED\n" +
                        "Para ver el estado o reconfigurar el dispositivo, TU CELULAR Y LA ALACENA DEBEN ESTAR CONECTADOS AL MISMO WIFI.\n" +
                        "Si usas datos móviles (4G/5G), la App no podrá encontrar el dispositivo.");
                break;
            case "USAR":
                txtTitle.setText("Guía de Uso Operativo");
                txtContent.setText("Utilizar tu Alacena Inteligente es una experiencia intuitiva diseñada para simplificar tu día a día. Antes de comenzar, asegúrate de que el dispositivo se encuentre correctamente conectado a tu red WiFi (la luz roja debe estar apagada).\n\n" +
                        "El panel de control físico cuenta con un sistema de doble confirmación mediante iluminación LED para gestionar tu inventario en tiempo real:\n\n" +
                        "🟢 MODO DE INGRESO (Botón Verde)\n" +
                        "Para abastecer tu alacena, presiona UNA VEZ el botón verde. Observarás que el indicador LED se ilumina en verde, confirmando que el sistema está listo para recibir mercadería. A continuación, simplemente pasa los productos por el escáner uno a uno; el sistema los registrará y sumará al stock automáticamente.\n\n" +
                        "🔴 MODO DE RETIRO (Botón Rojo)\n" +
                        "Al momento de consumir o retirar un ingrediente, presiona UNA VEZ el botón rojo. El indicador LED cambiará a rojo, señalizando el modo de salida. Escanea el código de barras del producto y el sistema descontará la unidad de tu inventario virtual al instante.\n\n" +
                        "💡 TIP PRO: No es necesario presionar el botón por cada producto si vas a escanear varios del mismo tipo (ej. 5 latas de atún); el modo se mantiene activo hasta que cambies de acción o dejes de usarlo.");
                break;

            case "AGREGAR":
                txtTitle.setText("Registro de Nuevos Productos");
                txtContent.setText("Para mantener tu inventario actualizado, el sistema ofrece dos métodos de registro flexibles, diseñados para cubrir tanto productos envasados como alimentos a granel:\n\n" +
                        "1️⃣ MÉTODO AUTOMÁTICO (Recomendado)\n" +
                        "Ideal para productos con código de barras. Dirígete a tu Alacena física, activa el 'Modo de Ingreso' (Botón Verde) y escanea el código del producto nuevo. El sistema consultará automáticamente una base de datos global para obtener el nombre, marca e imagen del producto y lo añadirá a tu inventario sin que tengas que escribir nada.\n\n" +
                        "2️⃣ MÉTODO MANUAL (Desde la App)\n" +
                        "Diseñado para productos sin código de barras (como frutas, verduras, panadería o artículos a granel). Ingresa a la sección 'Ver Inventario' en esta aplicación, toca el botón flotante '+' y completa los campos requeridos (Nombre, Categoría y Cantidad Inicial). Este método te permite tener un control total incluso de los ítems más artesanales.");
                break;

            case "ELIMINAR":
                txtTitle.setText("Eliminar Productos");
                txtContent.setText("Para borrar un producto permanentemente de tu base de datos:\n\n" +
                        "1. Ve a la pantalla 'Ver Inventario'.\n\n" +
                        "2. Busca el producto en la lista.\n\n" +
                        "3. Mantén presionado el dedo sobre el producto durante 2 segundos.\n\n" +
                        "4. Aparecerá un cuadro de confirmación preguntando si deseas eliminarlo. Confirma y listo.");
                break;

            case "EDITAR":
                txtTitle.setText("Editar Información");
                txtContent.setText("Puedes personalizar los detalles de tus productos:\n\n" +
                        "1. Ingresa a 'Ver Inventario'.\n\n" +
                        "2. Toca brevemente sobre cualquier producto de la lista.\n\n" +
                        "3. Se abrirá la ficha de edición donde podrás corregir el Nombre, la Marca y establecer el 'Stock Mínimo' (para que la App te avise cuando te quede poco).");
                break;

            default:
                txtTitle.setText("Ayuda");
                txtContent.setText("Selecciona una opción del menú para ver más detalles.");
                break;
        }
    }
}