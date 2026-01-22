package com.example.proyectoaplicacinturismolocal;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class MainActivity extends AppCompatActivity {

    private MapView map = null;

    // CONFIGURACIÓN DE TU BASE DE DATOS
    // Cambia la IP por la de tu PC (usa ipconfig en Windows)
    private static final String IP_SERVIDOR = "192.168.0.142";
    private static final String DB_NAME = "rutas_turisticas";
    private static final String URL = "jdbc:mysql://" + IP_SERVIDOR + ":3306/" + DB_NAME;
    private static final String USER = "admin";
    private static final String PASS = "nocturno";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuración necesaria para OpenStreetMap (osmdroid)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_main);

        // Inicializar Mapa
        map = findViewById(R.id.map);
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            map.setBuiltInZoomControls(false);

            // Centro inicial (Lucena)
            GeoPoint puntoInicial = new GeoPoint(37.408, -4.483);
            map.getController().setZoom(17.0);
            map.getController().setCenter(puntoInicial);
        }

        // Configurar botones de zoom
        configurarBotonesZoom();

        // Iniciar carga de datos desde MySQL
        conectarYObtenerSitios();
    }

    private void conectarYObtenerSitios() {
        // Ejecutamos en un hilo secundario para no bloquear la interfaz
        new Thread(() -> {
            try {
                // Registrar el Driver de MySQL
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Intentar establecer conexión
                Connection con = DriverManager.getConnection(URL, USER, PASS);
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("SELECT nombre, latitud, longitud, descripcion FROM sitios");

                while (rs.next()) {
                    String nombre = rs.getString("nombre");
                    double lat = rs.getDouble("latitud");
                    double lon = rs.getDouble("longitud");
                    String desc = rs.getString("descripcion");

                    // Usamos el Handler para volver al hilo de la UI y dibujar el marcador
                    new Handler(Looper.getMainLooper()).post(() -> {
                        crearMarcador(nombre, lat, lon, desc);
                    });
                }
                con.close();

            } catch (Exception e) {
                e.printStackTrace();
                // Mostrar el error en pantalla para facilitar el debug
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void crearMarcador(String nombre, double lat, double lon, String descripcion) {
        if (map == null) return;

        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setTitle(nombre);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Asegúrate de tener un icono llamado 'marcador' en res/drawable
        marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.marcador, null));

        // Desactivar el InfoWindow por defecto de osmdroid para usar nuestro BottomSheet
        marker.setInfoWindow(null);

        // Al hacer clic, mostramos el detalle personalizado
        marker.setOnMarkerClickListener((m, mapView) -> {
            mostrarDetalle(nombre, descripcion);
            return true;
        });

        map.getOverlays().add(marker);
        map.invalidate(); // Refrescar mapa
    }

    private void mostrarDetalle(String nombre, String descripcion) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        // Inflamos el XML personalizado para el detalle
        View view = getLayoutInflater().inflate(R.layout.detalle_layout, null);

        TextView txtTitulo = view.findViewById(R.id.detalle_titulo);
        TextView txtDesc = view.findViewById(R.id.detalle_descripcion);

        txtTitulo.setText(nombre);
        txtDesc.setText(descripcion);

        dialog.setContentView(view);
        dialog.show();
    }

    private void configurarBotonesZoom() {
        FloatingActionButton btnIn = findViewById(R.id.btn_zoom_in);
        FloatingActionButton btnOut = findViewById(R.id.btn_zoom_out);

        if (btnIn != null) btnIn.setOnClickListener(v -> map.getController().zoomIn());
        if (btnOut != null) btnOut.setOnClickListener(v -> map.getController().zoomOut());
    }

    // Gestión del ciclo de vida del mapa
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