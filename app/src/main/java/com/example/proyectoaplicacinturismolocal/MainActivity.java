package com.example.proyectoaplicacinturismolocal;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
    private boolean esFavorito = false;
    private View fragmentContainer;
    private View searchCard;
    private View zoomButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_main);

        View layoutFavoritos = findViewById(R.id.favorite_layout);
        View layoutPerfil = findViewById(R.id.profile_layout);

        if (layoutFavoritos != null) {
            layoutFavoritos.setVisibility(View.GONE);
        }
        if (layoutPerfil != null) {
            layoutPerfil.setVisibility(View.GONE);
        }

        loadingLayout = findViewById(R.id.loadingLayout);
        map = findViewById(R.id.map);
        dbConnector = new DatabaseConnector();

        // Inicializar vistas de navegación
        searchCard = findViewById(R.id.search_card);
        zoomButtons = findViewById(R.id.btn_zoom_in).getParent() instanceof View ? (View) findViewById(R.id.btn_zoom_in).getParent() : null;

        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            GeoPoint puntoInicial = new GeoPoint(37.408, -4.483);
            map.getController().setZoom(17.0);
            map.getController().setCenter(puntoInicial);
        }

        configurarBotonesZoom();
        conectarYObtenerSitios();
        configurarNavegacion();
    }

    private void configurarNavegacion() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            ocultarTodo();

            if (id == R.id.nav_home) {
                mostrarMapa(true); // Tu método que pone el mapa en VISIBLE
                return true;
            }
            else if (id == R.id.nav_favorites) {
                mostrarMapa(false);
                findViewById(R.id.favorite_layout).setVisibility(View.VISIBLE);
                return true;
            }
            else if (id == R.id.nav_profile) {
                mostrarMapa(false);
                return true;
            }
            return false;
        });
    }

    private void ocultarTodo() {
        findViewById(R.id.favorite_layout).setVisibility(View.GONE);
    }

    private void mostrarMapa(boolean visible) {
        if (visible) {
            map.setVisibility(View.VISIBLE);
            searchCard.setVisibility(View.VISIBLE);
            if (zoomButtons != null) zoomButtons.setVisibility(View.VISIBLE);
            if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
        } else {
            map.setVisibility(View.GONE);
            searchCard.setVisibility(View.GONE);
            if (zoomButtons != null) zoomButtons.setVisibility(View.GONE);
            if (fragmentContainer != null) fragmentContainer.setVisibility(View.VISIBLE);
        }
    }

    private void conectarYObtenerSitios() {
        loadingLayout.setVisibility(View.VISIBLE);

        dbConnector.ejecutarConsulta(new DatabaseConnector.DatabaseListener() {
            @Override
            public void onSitioEncontrado(String nombre, double lat, double lon, String desc, String urlImagen) {
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
        ImageButton btnFavorito = view.findViewById(R.id.btn_favorito);

        txtTitulo.setText(nombre);
        txtDesc.setText(descripcion);

        btnFavorito.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                esFavorito = !esFavorito;
                if (esFavorito) {
                    btnFavorito.setImageResource(R.drawable.favorito);
                    Toast.makeText(MainActivity.this, "Añadido a favoritos", Toast.LENGTH_SHORT).show();
                } else {
                    btnFavorito.setImageResource(R.drawable.estrella);
                    Toast.makeText(MainActivity.this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Glide.with(this)
                .load(urlImagen)
                .placeholder(android.R.drawable.progress_horizontal)
                .error(android.R.drawable.ic_menu_report_image)
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