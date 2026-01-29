package com.example.proyectoaplicacinturismolocal;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import DatabaseConnection.DatabaseConnector;

public class MainActivity extends AppCompatActivity {

    private MapView map = null;
    private View loadingLayout;
    private DatabaseConnector dbConnector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_main);

        loadingLayout = findViewById(R.id.loadingLayout);
        map = findViewById(R.id.map);
        dbConnector = new DatabaseConnector();

        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            GeoPoint puntoInicial = new GeoPoint(37.408, -4.483);
            map.getController().setZoom(17.0);
            map.getController().setCenter(puntoInicial);
        }

        configurarBotonesZoom();
        conectarYObtenerSitios();
    }

    private void conectarYObtenerSitios() {
        loadingLayout.setVisibility(View.VISIBLE);

        dbConnector.ejecutarConsulta(new DatabaseConnector.DatabaseListener() {
            @Override
            public void onSitioEncontrado(String nombre, double lat, double lon, String desc, String urlImagen) {
                // Recibimos la URL de la base de datos
                new Handler(Looper.getMainLooper()).post(() ->
                        crearMarcador(nombre, lat, lon, desc, urlImagen)
                );
            }

            @Override
            public void onError(String mensaje) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(MainActivity.this, "Error: " + mensaje, Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onFinalizado() {
                new Handler(Looper.getMainLooper()).post(() ->
                        loadingLayout.setVisibility(View.GONE)
                );
            }
        });
    }

    private void crearMarcador(String nombre, double lat, double lon, String descripcion, String urlImagen) {
        if (map == null) return;

        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setTitle(nombre);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.marcador, null));
        marker.setInfoWindow(null);

        // Pasamos la URL al evento de clic
        marker.setOnMarkerClickListener((m, mapView) -> {
            mostrarDetalle(nombre, descripcion, urlImagen);
            return true;
        });

        map.getOverlays().add(marker);
        map.invalidate();
    }

    private void mostrarDetalle(String nombre, String descripcion, String urlImagen) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.detalle_layout, null);

        TextView txtTitulo = view.findViewById(R.id.detalle_titulo);
        TextView txtDesc = view.findViewById(R.id.detalle_descripcion);
        ImageView imgDetalle = view.findViewById(R.id.detalle_imagen);

        txtTitulo.setText(nombre);
        txtDesc.setText(descripcion);

        // --- CARGAR IMAGEN CON GLIDE ---
        Glide.with(this)
                .load(urlImagen) // La URL VARCHAR que viene de MySQL
                .placeholder(android.R.drawable.progress_horizontal) // Mientras descarga
                .error(android.R.drawable.ic_menu_report_image)     // Si falla la URL
                .centerCrop()
                .into(imgDetalle);

        dialog.setContentView(view);
        dialog.show();
    }

    private void configurarBotonesZoom() {
        FloatingActionButton btnIn = findViewById(R.id.btn_zoom_in);
        FloatingActionButton btnOut = findViewById(R.id.btn_zoom_out);
        if (btnIn != null) btnIn.setOnClickListener(v -> map.getController().zoomIn());
        if (btnOut != null) btnOut.setOnClickListener(v -> map.getController().zoomOut());
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (map != null) map.onResume();
    }
    @Override
    protected void onPause()
    {
        super.onPause();
        if (map != null) map.onPause();
    }
}