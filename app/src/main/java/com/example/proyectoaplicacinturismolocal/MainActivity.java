package com.example.proyectoaplicacinturismolocal;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class MainActivity extends AppCompatActivity {

    private MapView map = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Cargar configuración de OSM (Obligatorio)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_main);

        // 2. Inicializar el mapa
        map = findViewById(R.id.map);
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);

            // Desactivamos los botones antiguos para usar los tuyos del XML
            map.setBuiltInZoomControls(false);

            // Configurar vista inicial en Lucena
            GeoPoint lucena = new GeoPoint(37.408, -4.483);
            map.getController().setZoom(17.0);
            map.getController().setCenter(lucena);
        }

        // 3. Configurar los botones de Zoom del XML
        FloatingActionButton btnZoomIn = findViewById(R.id.btn_zoom_in);
        FloatingActionButton btnZoomOut = findViewById(R.id.btn_zoom_out);

        if (btnZoomIn != null) {
            btnZoomIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (map != null) map.getController().zoomIn();
                }
            });
        }

        if (btnZoomOut != null) {
            btnZoomOut.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (map != null) map.getController().zoomOut();
                }
            });
        }
    }

    // Métodos obligatorios para que el mapa no se congele
    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}