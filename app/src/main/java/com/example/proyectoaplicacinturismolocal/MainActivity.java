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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

import DatabaseConnection.DatabaseConnector;

public class MainActivity extends AppCompatActivity {

    private MapView map = null;
    private View loadingLayout;
    private DatabaseConnector dbConnector;
    private View searchCard;
    private View zoomButtons;

    // --- NUEVAS VARIABLES PARA RECYCLERVIEW ---
    private RecyclerView recyclerView;
    private FavoritosAdapter adapter;
    private List<Sitio> listaFavoritos = new ArrayList<>();
    private TextView txtVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_main);

        // 1. Inicializar vistas y componentes
        loadingLayout = findViewById(R.id.loadingLayout);
        map = findViewById(R.id.map);
        searchCard = findViewById(R.id.search_card);
        txtVacio = findViewById(R.id.text_view_empty);
        dbConnector = new DatabaseConnector();

        // 2. Configurar el RecyclerView (el que ya tienes en tu XML)
        recyclerView = findViewById(R.id.recycler_view_favoritos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoritosAdapter(listaFavoritos, posicion -> {
            // Esta es la lógica que se ejecuta al pulsar el botón de borrar del item
            if (!listaFavoritos.isEmpty() && posicion < listaFavoritos.size()) {
                listaFavoritos.remove(posicion);
                adapter.notifyItemRemoved(posicion);
                adapter.notifyItemRangeChanged(posicion, listaFavoritos.size());
                actualizarVistaVacia();
            }
        });        recyclerView.setAdapter(adapter);

        // Ocultar layouts de fragmentos al inicio
        findViewById(R.id.favorite_layout).setVisibility(View.GONE);

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
        actualizarEstadoLista();
    }
    private void actualizarVistaVacia() {
        if (txtVacio != null) {
            txtVacio.setVisibility(listaFavoritos.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
    private void configurarNavegacion() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            ocultarTodo();
            if (id == R.id.nav_home) {
                mostrarMapa(true);
                return true;
            } else if (id == R.id.nav_favorites) {
                mostrarMapa(false);
                findViewById(R.id.favorite_layout).setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });
    }

    private void ocultarTodo() {
        findViewById(R.id.favorite_layout).setVisibility(View.GONE);
    }

    private void mostrarMapa(boolean visible) {
        int vis = visible ? View.VISIBLE : View.GONE;
        map.setVisibility(vis);
        searchCard.setVisibility(vis);
        // El contenedor de zoom
        View zoomContainer = findViewById(R.id.btn_zoom_in).getParent() instanceof View ? (View) findViewById(R.id.btn_zoom_in).getParent() : null;
        if (zoomContainer != null) zoomContainer.setVisibility(vis);
    }

    private void conectarYObtenerSitios() {
        loadingLayout.setVisibility(View.VISIBLE);
        dbConnector.ejecutarConsulta(new DatabaseConnector.DatabaseListener() {
            @Override
            public void onSitioEncontrado(String nombre, double lat, double lon, String desc, String urlImagen) {
                // Aquí recibes los datos de la DB
                new Handler(Looper.getMainLooper()).post(() ->
                        crearMarcador(nombre, lat, lon, desc, urlImagen)
                );
            }

            @Override
            public void onError(String mensaje) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(MainActivity.this, "Error DB: " + mensaje, Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onFinalizado() {
                new Handler(Looper.getMainLooper()).post(() -> loadingLayout.setVisibility(View.GONE));
            }
        });
    }

    private void crearMarcador(String nombre, double lat, double lon, String descripcion, String urlImagen) {
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setTitle(nombre);
        marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.marcador, null));

        // Al hacer clic, pasamos toda la información al detalle
        marker.setOnMarkerClickListener((m, mapView) -> {
            mostrarDetalle(new Sitio(nombre, descripcion, urlImagen));
            return true;
        });

        map.getOverlays().add(marker);
        map.invalidate();
    }

    private void mostrarDetalle(Sitio sitioSeleccionado) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.detalle_layout, null);

        TextView txtTitulo = view.findViewById(R.id.detalle_titulo);
        TextView txtDesc = view.findViewById(R.id.detalle_descripcion);
        ImageView imgDetalle = view.findViewById(R.id.detalle_imagen);
        ImageButton btnFavorito = view.findViewById(R.id.btn_favorito);

        txtTitulo.setText(sitioSeleccionado.getNombre());
        txtDesc.setText(sitioSeleccionado.getDescripcion());
        Glide.with(this).load(sitioSeleccionado.getUrlImagen()).into(imgDetalle);

        // LÓGICA DE FAVORITOS
        actualizarIconoFavorito(btnFavorito, sitioSeleccionado);

        btnFavorito.setOnClickListener(v -> {
            if (!estaEnFavoritos(sitioSeleccionado)) {
                listaFavoritos.add(sitioSeleccionado);
                Toast.makeText(this, "Guardado en favoritos", Toast.LENGTH_SHORT).show();
            } else {
                removerDeFavoritos(sitioSeleccionado);
                Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
            }

            // Refrescar el RecyclerView y el icono
            adapter.notifyDataSetChanged();
            actualizarIconoFavorito(btnFavorito, sitioSeleccionado);
            actualizarEstadoLista();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    // Métodos auxiliares para gestionar la lista
    private boolean estaEnFavoritos(Sitio sitio) {
        for (Sitio s : listaFavoritos) {
            if (s.getNombre().equals(sitio.getNombre())) return true;
        }
        return false;
    }

    private void removerDeFavoritos(Sitio sitio) {
        listaFavoritos.removeIf(s -> s.getNombre().equals(sitio.getNombre()));
    }

    private void actualizarIconoFavorito(ImageButton btn, Sitio sitio) {
        if (estaEnFavoritos(sitio)) {
            btn.setImageResource(R.drawable.favorito); // Tu icono relleno
        } else {
            btn.setImageResource(R.drawable.estrella); // Tu icono vacío
        }
    }

    private void actualizarEstadoLista() {
        if (listaFavoritos.isEmpty()) {
            txtVacio.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtVacio.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void configurarBotonesZoom() {
        FloatingActionButton btnIn = findViewById(R.id.btn_zoom_in);
        FloatingActionButton btnOut = findViewById(R.id.btn_zoom_out);
        if (btnIn != null) btnIn.setOnClickListener(v -> map.getController().zoomIn());
        if (btnOut != null) btnOut.setOnClickListener(v -> map.getController().zoomOut());
    }

    @Override
    protected void onResume() { super.onResume(); if (map != null) map.onResume(); }
    @Override
    protected void onPause() { super.onPause(); if (map != null) map.onPause(); }
}