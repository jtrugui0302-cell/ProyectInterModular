package com.example.proyectoaplicacinturismolocal;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.proyectoaplicacinturismolocal.LoginResources.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;

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

    // --- VARIABLES DE BÚSQUEDA, RUTAS Y UBICACIÓN ---
    private List<Sitio> listaSitiosCompleta = new ArrayList<>();
    private EditText editTextSearch;
    private Polyline rutaActual;
    private MyLocationNewOverlay myLocationOverlay;

    // --- VARIABLES PARA UBICACIÓN MANUAL Y ESTADO DE NAVEGACIÓN ---
    private GeoPoint puntoManual = null;
    private Marker marcadorManual = null;
    private Sitio sitioEnNavegacion = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_main);

        // Solicitar permisos de GPS
        solicitarPermisos();

        loadingLayout = findViewById(R.id.loadingLayout);
        map = findViewById(R.id.map);
        searchCard = findViewById(R.id.search_card);
        txtVacio = findViewById(R.id.text_view_empty);
        dbConnector = new DatabaseConnector();
        editTextSearch = findViewById(R.id.edit_text_search);

        // Configurar buscador
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filtrarSitios(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Configurar Favoritos
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

        // Configurar Mapa
        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            GeoPoint puntoInicial = new GeoPoint(37.408, -4.483);
            map.getController().setZoom(17.0);
            map.getController().setCenter(puntoInicial);

            // Capa de ubicación GPS real
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            myLocationOverlay.enableMyLocation();
            map.getOverlays().add(myLocationOverlay);

            // Capa para detectar pulsación larga (Ubicación manual)
            configurarPulsacionLarga();
        }

        configurarBotonesZoom();
        conectarYObtenerSitios();
        configurarNavegacion();
        actualizarEstadoLista();
    }

    private void configurarPulsacionLarga() {
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) { return false; }
            @Override public boolean longPressHelper(GeoPoint p) {
                puntoManual = p;
                if (marcadorManual != null) { map.getOverlays().remove(marcadorManual); }
                marcadorManual = new Marker(map);
                marcadorManual.setPosition(p);
                marcadorManual.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marcadorManual.setTitle("Mi ubicación manual");
                marcadorManual.setIcon(ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_mylocation, null));
                map.getOverlays().add(marcadorManual);
                map.invalidate();
                Toast.makeText(MainActivity.this, "Punto de origen fijado", Toast.LENGTH_SHORT).show();
                return true;
            }
        };
        map.getOverlays().add(new MapEventsOverlay(mReceive));
    }

    private void solicitarPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void filtrarSitios(String texto) {
        if (map == null) return;
        String query = texto.toLowerCase();
        map.getOverlays().clear();

        // Re-añadir capas permanentes para que no desaparezcan al buscar
        if (myLocationOverlay != null) map.getOverlays().add(myLocationOverlay);
        if (marcadorManual != null) map.getOverlays().add(marcadorManual);
        if (rutaActual != null) map.getOverlays().add(rutaActual);
        configurarPulsacionLarga();

        for (Sitio s : listaSitiosCompleta) {
            if (s.getNombre().toLowerCase().contains(query) || (s.getTipo() != null && s.getTipo().toLowerCase().contains(query))) {
                crearMarcador(s.getNombre(), s.getLatitud(), s.getLongitud(), s.getDescripcion(), s.getUrlImagen(), s.getTipo());
            }
        }
        map.invalidate();
    }

    private void conectarYObtenerSitios() {
        loadingLayout.setVisibility(View.VISIBLE);
        listaSitiosCompleta.clear();
        dbConnector.ejecutarConsulta(new DatabaseConnector.DatabaseListener() {
            @Override public void onSitioEncontrado(String nombre, double lat, double lon, String desc, String urlImagen, String tipo) {
                Sitio sitio = new Sitio(nombre, desc, urlImagen, tipo);
                sitio.setLatitud(lat); sitio.setLongitud(lon);
                listaSitiosCompleta.add(sitio);
                new Handler(Looper.getMainLooper()).post(() -> crearMarcador(nombre, lat, lon, desc, urlImagen, tipo));
            }
            @Override public void onError(String mensaje) { new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(MainActivity.this, "Error DB: " + mensaje, Toast.LENGTH_SHORT).show()); }
            @Override public void onFinalizado() { new Handler(Looper.getMainLooper()).post(() -> loadingLayout.setVisibility(View.GONE)); }
        });
    }

    private void crearMarcador(String nombre, double lat, double lon, String descripcion, String urlImagen, String tipo) {
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(lat, lon));
        marker.setTitle(nombre);
        marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.marcador, null));
        marker.setOnMarkerClickListener((m, mapView) -> {
            Sitio s = new Sitio(nombre, descripcion, urlImagen, tipo);
            s.setLatitud(lat); s.setLongitud(lon);
            mostrarDetalle(s);
            return true;
        });
        map.getOverlays().add(marker);
    }

    private void mostrarDetalle(Sitio sitioSeleccionado) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.detalle_layout, null);

        TextView txtTitulo = view.findViewById(R.id.detalle_titulo);
        TextView txtDesc = view.findViewById(R.id.detalle_descripcion);
        TextView txtCategoria = view.findViewById(R.id.detalle_categoria);
        ImageView imgDetalle = view.findViewById(R.id.detalle_imagen);
        ImageButton btnFavorito = view.findViewById(R.id.btn_favorito);

        com.google.android.material.button.MaterialButton btnNavegar = view.findViewById(R.id.btn_navegar);
        com.google.android.material.button.MaterialButton btnComentario = view.findViewById(R.id.btn_comentario);

        txtTitulo.setText(sitioSeleccionado.getNombre());
        txtDesc.setText(sitioSeleccionado.getDescripcion());
        txtCategoria.setText("Categoría: " + sitioSeleccionado.getTipo());
        Glide.with(this).load(sitioSeleccionado.getUrlImagen()).into(imgDetalle);

        // LÓGICA BOTÓN NAVEGAR (Toggle inteligente)
        if (sitioEnNavegacion != null && sitioEnNavegacion.getNombre().equals(sitioSeleccionado.getNombre())) {
            btnNavegar.setText("Detener navegación");
            btnNavegar.setOnClickListener(v -> {
                if (rutaActual != null) { map.getOverlays().remove(rutaActual); rutaActual = null; sitioEnNavegacion = null; map.invalidate(); }
                dialog.dismiss();
            });
        } else {
            btnNavegar.setText("Navegar");
            btnNavegar.setOnClickListener(v -> {
                GeoPoint destino = new GeoPoint(sitioSeleccionado.getLatitud(), sitioSeleccionado.getLongitud());
                GeoPoint origen = (puntoManual != null) ? puntoManual : (myLocationOverlay.getMyLocation() != null) ? myLocationOverlay.getMyLocation() : (GeoPoint) map.getMapCenter();
                sitioEnNavegacion = sitioSeleccionado;
                trazarRuta(origen, destino);
                dialog.dismiss();
            });
        }

        // LÓGICA BOTÓN COMENTARIO (Verificación de sesión)
        btnComentario.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            if (prefs.getInt("userId", -1) == -1) {
                Toast.makeText(this, "Inicia sesión para comentar", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
            } else {
                Toast.makeText(this, "Panel de comentarios de: " + prefs.getString("userName", ""), Toast.LENGTH_SHORT).show();
                // Aquí podrías inflar un diálogo para escribir comentarios
            }
        });

        // LÓGICA FAVORITOS
        actualizarIconoFavorito(btnFavorito, sitioSeleccionado);
        btnFavorito.setOnClickListener(v -> {
            if (!estaEnFavoritos(sitioSeleccionado)) { listaFavoritos.add(sitioSeleccionado); }
            else { removerDeFavoritos(sitioSeleccionado); }
            adapter.notifyDataSetChanged();
            actualizarIconoFavorito(btnFavorito, sitioSeleccionado);
            actualizarEstadoLista();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void trazarRuta(GeoPoint origen, GeoPoint destino) {
        new Thread(() -> {
            OSRMRoadManager roadManager = new OSRMRoadManager(this, getPackageName());
            roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT); // Modo Pie

            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            waypoints.add(origen); waypoints.add(destino);
            Road road = roadManager.getRoad(waypoints);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (road.mStatus == Road.STATUS_OK) {
                    if (rutaActual != null) map.getOverlays().remove(rutaActual);
                    rutaActual = RoadManager.buildRoadOverlay(road);
                    rutaActual.getOutlinePaint().setColor(Color.BLUE);
                    rutaActual.getOutlinePaint().setStrokeWidth(12f);
                    map.getOverlays().add(rutaActual);
                    map.invalidate();
                }
            });
        }).start();
    }

    private void configurarNavegacion() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            ocultarTodo();
            if (id == R.id.nav_home) { mostrarMapa(true); return true; }
            else if (id == R.id.nav_favorites) { mostrarMapa(false); findViewById(R.id.favorite_layout).setVisibility(View.VISIBLE); return true; }
            return false;
        });
    }

    private void ocultarTodo() { findViewById(R.id.favorite_layout).setVisibility(View.GONE); }
    private void mostrarMapa(boolean v) {
        int vis = v ? View.VISIBLE : View.GONE;
        map.setVisibility(vis); searchCard.setVisibility(vis);
        View zoom = findViewById(R.id.btn_zoom_in).getParent() instanceof View ? (View) findViewById(R.id.btn_zoom_in).getParent() : null;
        if (zoom != null) zoom.setVisibility(vis);
    }
    private boolean estaEnFavoritos(Sitio s) { for (Sitio f : listaFavoritos) { if (f.getNombre().equals(s.getNombre())) return true; } return false; }
    private void removerDeFavoritos(Sitio s) { listaFavoritos.removeIf(f -> f.getNombre().equals(s.getNombre())); }
    private void actualizarIconoFavorito(ImageButton b, Sitio s) { b.setImageResource(estaEnFavoritos(s) ? R.drawable.favorito : R.drawable.estrella); }
    private void actualizarEstadoLista() { boolean v = listaFavoritos.isEmpty(); txtVacio.setVisibility(v ? View.VISIBLE : View.GONE); recyclerView.setVisibility(v ? View.GONE : View.VISIBLE); }
    private void configurarBotonesZoom() { findViewById(R.id.btn_zoom_in).setOnClickListener(v -> map.getController().zoomIn()); findViewById(R.id.btn_zoom_out).setOnClickListener(v -> map.getController().zoomOut()); }

    @Override protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
    }

    @Override protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
    }
}