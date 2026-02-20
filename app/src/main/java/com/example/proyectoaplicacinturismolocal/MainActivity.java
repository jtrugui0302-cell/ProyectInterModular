package com.example.proyectoaplicacinturismolocal;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
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
    private RecyclerView recyclerView;
    private FavoritosAdapter adapter;
    private List<Sitio> listaFavoritos = new ArrayList<>();
    private TextView txtVacio;

    private List<Sitio> listaSitiosCompleta = new ArrayList<>();
    private EditText editTextSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_main);

        loadingLayout = findViewById(R.id.loadingLayout);
        map = findViewById(R.id.map);
        searchCard = findViewById(R.id.search_card);
        txtVacio = findViewById(R.id.text_view_empty);
        dbConnector = new DatabaseConnector();
        editTextSearch = findViewById(R.id.edit_text_search);

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarSitios(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        recyclerView = findViewById(R.id.recycler_view_favoritos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoritosAdapter(listaFavoritos, posicion -> {
            if (!listaFavoritos.isEmpty() && posicion < listaFavoritos.size()) {
                listaFavoritos.remove(posicion);
                adapter.notifyItemRemoved(posicion);
                adapter.notifyItemRangeChanged(posicion, listaFavoritos.size());
                actualizarEstadoLista();
            }
        });
        recyclerView.setAdapter(adapter);

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

    // --- LÓGICA DE FILTRADO CORREGIDA ---
    private void filtrarSitios(String texto) {
        if (map == null) return;

        String query = texto.toLowerCase();
        map.getOverlays().clear();

        for (Sitio s : listaSitiosCompleta) {
            boolean coincideNombre = s.getNombre().toLowerCase().contains(query);
            boolean coincideTipo = s.getTipo() != null && s.getTipo().toLowerCase().contains(query);

            if (coincideNombre || coincideTipo) {
                // Se añadieron los paréntesis corregidos en s.getTipo()
                crearMarcador(s.getNombre(), s.getLatitud(), s.getLongitud(), s.getDescripcion(), s.getUrlImagen(), s.getTipo());
            }
        }
        map.invalidate();
    }

    private void conectarYObtenerSitios() {
        loadingLayout.setVisibility(View.VISIBLE);
        listaSitiosCompleta.clear();
        dbConnector.ejecutarConsulta(new DatabaseConnector.DatabaseListener() {
            @Override
            public void onSitioEncontrado(String nombre, double lat, double lon, String desc, String urlImagen, String tipo) {
                Sitio sitio = new Sitio(nombre, desc, urlImagen, tipo);
                sitio.setLatitud(lat);
                sitio.setLongitud(lon);
                listaSitiosCompleta.add(sitio);

                new Handler(Looper.getMainLooper()).post(() ->
                        crearMarcador(nombre, lat, lon, desc, urlImagen, tipo)
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

    private void crearMarcador(String nombre, double lat, double lon, String descripcion, String urlImagen, String tipo) {
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setTitle(nombre);
        marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.marcador, null));

        marker.setOnMarkerClickListener((m, mapView) -> {
            Sitio s = new Sitio(nombre, descripcion, urlImagen, tipo);
            s.setLatitud(lat);
            s.setLongitud(lon);
            mostrarDetalle(s);
            return true;
        });

        map.getOverlays().add(marker);
        map.invalidate(); // Forzamos el dibujado
    }

    private void mostrarDetalle(Sitio sitioSeleccionado) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.detalle_layout, null);

        TextView txtTitulo = view.findViewById(R.id.detalle_titulo);
        TextView txtDesc = view.findViewById(R.id.detalle_descripcion);

        // 1. Buscamos el TextView de la categoría
        TextView txtCategoria = view.findViewById(R.id.detalle_categoria);

        ImageView imgDetalle = view.findViewById(R.id.detalle_imagen);
        ImageButton btnFavorito = view.findViewById(R.id.btn_favorito);

        txtTitulo.setText(sitioSeleccionado.getNombre());
        txtDesc.setText(sitioSeleccionado.getDescripcion());

        // 2. Aquí concatenamos el prefijo con el dato real
        txtCategoria.setText("Categoría: " + sitioSeleccionado.getTipo());

        Glide.with(this).load(sitioSeleccionado.getUrlImagen()).into(imgDetalle);

        actualizarIconoFavorito(btnFavorito, sitioSeleccionado);

        btnFavorito.setOnClickListener(v -> {
            if (!estaEnFavoritos(sitioSeleccionado)) {
                listaFavoritos.add(sitioSeleccionado);
                Toast.makeText(this, "Guardado en favoritos", Toast.LENGTH_SHORT).show();
            } else {
                removerDeFavoritos(sitioSeleccionado);
                Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
            }
            adapter.notifyDataSetChanged();
            actualizarIconoFavorito(btnFavorito, sitioSeleccionado);
            actualizarEstadoLista();
        });

        dialog.setContentView(view);
        dialog.show();
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
        View zoomContainer = findViewById(R.id.btn_zoom_in).getParent() instanceof View ? (View) findViewById(R.id.btn_zoom_in).getParent() : null;
        if (zoomContainer != null) zoomContainer.setVisibility(vis);
    }

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
        btn.setImageResource(estaEnFavoritos(sitio) ? R.drawable.favorito : R.drawable.estrella);
    }

    private void actualizarEstadoLista() {
        boolean vacia = listaFavoritos.isEmpty();
        txtVacio.setVisibility(vacia ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(vacia ? View.GONE : View.VISIBLE);
    }

    private void configurarBotonesZoom() {
        findViewById(R.id.btn_zoom_in).setOnClickListener(v -> map.getController().zoomIn());
        findViewById(R.id.btn_zoom_out).setOnClickListener(v -> map.getController().zoomOut());
    }

    @Override protected void onResume() { super.onResume(); if (map != null) map.onResume(); }
    @Override protected void onPause() { super.onPause(); if (map != null) map.onPause(); }
}