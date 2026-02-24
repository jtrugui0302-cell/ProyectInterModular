package com.example.proyectoaplicacinturismolocal;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.proyectoaplicacinturismolocal.Adapters.ComentarioAdapter;
import com.example.proyectoaplicacinturismolocal.Adapters.FavoritosAdapter;
import com.example.proyectoaplicacinturismolocal.Models.Comentarios;
import com.example.proyectoaplicacinturismolocal.Models.Sitio;
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
import LoginResources.LoginActivity;

public class MainActivity extends AppCompatActivity {

    private MapView map = null;
    private DatabaseConnector dbConnector;
    private FrameLayout fragmentContainer;
    private List<Sitio> listaSitiosCompleta = new ArrayList<>();
    private List<Sitio> listaFavoritos = new ArrayList<>();
    private FavoritosAdapter favAdapter;
    private MyLocationNewOverlay myLocationOverlay;
    private Polyline rutaActual;
    private GeoPoint puntoManual = null;
    private Marker marcadorManual = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_main);

        solicitarPermisos();
        map = findViewById(R.id.map);
        fragmentContainer = findViewById(R.id.fragment_container);
        dbConnector = new DatabaseConnector();

        // Buscador
        EditText searchEdit = findViewById(R.id.edit_text_search);
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filtrarSitios(s.toString()); }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        // RecyclerView Favoritos
        RecyclerView rvFav = findViewById(R.id.recycler_view_favoritos);
        rvFav.setLayoutManager(new LinearLayoutManager(this));
        favAdapter = new FavoritosAdapter(listaFavoritos, pos -> {
            listaFavoritos.remove(pos); favAdapter.notifyDataSetChanged();
            findViewById(R.id.text_view_empty).setVisibility(listaFavoritos.isEmpty() ? View.VISIBLE : View.GONE);
        });
        rvFav.setAdapter(favAdapter);

        if (map != null) {
            map.setTileSource(TileSourceFactory.MAPNIK);
            map.setMultiTouchControls(true);
            map.getController().setZoom(17.0);
            map.getController().setCenter(new GeoPoint(37.408, -4.483));
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
            myLocationOverlay.enableMyLocation();
            map.getOverlays().add(myLocationOverlay);
            configurarPulsacionLarga();
        }

        conectarYObtenerSitios();
        configurarNavegacion();
    }

    private void configurarNavegacion() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            findViewById(R.id.favorite_layout).setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.GONE);
            if (id == R.id.nav_home) { mostrarMapa(true); }
            else if (id == R.id.nav_favorites) { mostrarMapa(false); findViewById(R.id.favorite_layout).setVisibility(View.VISIBLE); }
            else if (id == R.id.nav_profile) { mostrarMapa(false); mostrarPerfil(); }
            return true;
        });
    }

    private void mostrarPerfil() {
        fragmentContainer.setVisibility(View.VISIBLE);
        fragmentContainer.removeAllViews();
        // Corrección del inflate para el perfil también para evitar errores de contexto
        View v = LayoutInflater.from(this).inflate(R.layout.profile_layout, null);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int uid = prefs.getInt("userId", -1);

        TextView txtNom = v.findViewById(R.id.perfil_nombre_completo);
        TextView txtMail = v.findViewById(R.id.perfil_gmail);
        Button btnLogin = v.findViewById(R.id.btn_login);
        Button btnLogout = v.findViewById(R.id.btn_logout);
        RecyclerView rvMisCom = v.findViewById(R.id.recycler_comentarios_usuario);

        if (uid == -1) {
            // ESTADO: INVITADO
            txtNom.setText("Modo Invitado");
            txtMail.setText("Regístrate para ver tu actividad");

            if (btnLogin != null) {
                btnLogin.setVisibility(View.VISIBLE);
                btnLogin.setOnClickListener(view -> {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                });
            }
            if (btnLogout != null) btnLogout.setVisibility(View.GONE);

        } else {
            // ESTADO: LOGUEADO
            String nombreCompleto = prefs.getString("userName", "") + " " + prefs.getString("userApellido", "");
            txtNom.setText(nombreCompleto);
            txtMail.setText(prefs.getString("userEmail", ""));

            if (btnLogin != null) btnLogin.setVisibility(View.GONE); // No puede volver a iniciar sesión
            if (btnLogout != null) {
                btnLogout.setVisibility(View.VISIBLE);
                btnLogout.setOnClickListener(view -> {
                    prefs.edit().clear().apply();
                    recreate();
                });
            }

            // Cargar historial
            if (rvMisCom != null) {
                rvMisCom.setLayoutManager(new LinearLayoutManager(this));
                dbConnector.obtenerComentariosUsuario(uid, new DatabaseConnector.CommentListener() {
                    @Override public void onCommentsLoaded(List<Comentarios> list) {
                        runOnUiThread(() -> rvMisCom.setAdapter(new ComentarioAdapter(list)));
                    }
                    @Override public void onCommentAdded(boolean e) {}
                    @Override public void onError(String m) {}
                });
            }
        }
        fragmentContainer.addView(v);
    }

    private void mostrarDetalle(Sitio s) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.detalle_layout, null);
        ((TextView)view.findViewById(R.id.detalle_titulo)).setText(s.getNombre());
        ((TextView)view.findViewById(R.id.detalle_descripcion)).setText(s.getDescripcion());
        ((TextView)view.findViewById(R.id.detalle_categoria)).setText("Categoría: " + s.getTipo());
        Glide.with(this).load(s.getUrlImagen()).into((ImageView)view.findViewById(R.id.detalle_imagen));

        view.findViewById(R.id.btn_comentario).setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            int userId = prefs.getInt("userId", -1);
            dialog.dismiss();

            if (userId == -1) {
                Toast.makeText(MainActivity.this, "Debes estar registrado para comentar", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            } else {
                abrirSocial(s);
            }
        });

        view.findViewById(R.id.btn_navegar).setOnClickListener(v -> {
            GeoPoint d = new GeoPoint(s.getLatitud(), s.getLongitud());
            GeoPoint o = (puntoManual != null) ? puntoManual : (myLocationOverlay.getMyLocation() != null ? myLocationOverlay.getMyLocation() : (GeoPoint)map.getMapCenter());
            trazarRuta(o, d); dialog.dismiss();
        });
        dialog.setContentView(view); dialog.show();
    }

    private void abrirSocial(Sitio s) {
        BottomSheetDialog socialDialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_comentarios, null);
        RecyclerView rv = v.findViewById(R.id.rv_comentarios);
        rv.setLayoutManager(new LinearLayoutManager(this));

        dbConnector.obtenerComentariosSitio(s.getId(), new DatabaseConnector.CommentListener() {
            @Override public void onCommentsLoaded(List<Comentarios> list) {
                runOnUiThread(() -> rv.setAdapter(new ComentarioAdapter(list)));
            }
            @Override public void onCommentAdded(boolean e) {}
            @Override public void onError(String m) {}
        });

        v.findViewById(R.id.btn_enviar).setOnClickListener(view -> {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            int uid = prefs.getInt("userId", -1);
            String txt = ((EditText)v.findViewById(R.id.et_nuevo_comentario)).getText().toString().trim();
            if(!txt.isEmpty()){
                dbConnector.insertarComentario(uid, s.getId(), txt, new DatabaseConnector.CommentListener() {
                    @Override public void onCommentAdded(boolean e) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Enviado", Toast.LENGTH_SHORT).show();
                            socialDialog.dismiss();
                        });
                    }
                    @Override public void onCommentsLoaded(List<Comentarios> l) {}
                    @Override public void onError(String m) { runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: "+m, Toast.LENGTH_SHORT).show()); }
                });
            }
        });
        socialDialog.setContentView(v); socialDialog.show();
    }

    private void crearMarcador(Sitio s) {
        Marker m = new Marker(map); m.setPosition(new GeoPoint(s.getLatitud(), s.getLongitud())); m.setTitle(s.getNombre());
        m.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.marcador, null));
        m.setOnMarkerClickListener((mk, mv) -> { mostrarDetalle(s); return true; });
        map.getOverlays().add(m);
    }

    private void conectarYObtenerSitios() {
        findViewById(R.id.loadingLayout).setVisibility(View.VISIBLE);
        dbConnector.ejecutarConsulta(new DatabaseConnector.DatabaseListener() {
            @Override public void onSitioEncontrado(int id, String n, double la, double lo, String d, String u, String t) {
                Sitio s = new Sitio(n, d, u, t); s.setLatitud(la); s.setLongitud(lo); s.setId(id);
                listaSitiosCompleta.add(s);
                new Handler(Looper.getMainLooper()).post(() -> crearMarcador(s));
            }
            @Override public void onError(String m) {}
            @Override public void onFinalizado() { new Handler(Looper.getMainLooper()).post(() -> findViewById(R.id.loadingLayout).setVisibility(View.GONE)); }
        });
    }

    private void filtrarSitios(String t) {
        String q = t.toLowerCase(); map.getOverlays().clear();
        if (myLocationOverlay != null) map.getOverlays().add(myLocationOverlay);
        if (marcadorManual != null) map.getOverlays().add(marcadorManual);
        if (rutaActual != null) map.getOverlays().add(rutaActual);
        configurarPulsacionLarga();
        for (Sitio s : listaSitiosCompleta) {
            if (s.getNombre().toLowerCase().contains(q) || (s.getTipo() != null && s.getTipo().toLowerCase().contains(q))) {
                crearMarcador(s);
            }
        }
        map.invalidate();
    }

    private void trazarRuta(GeoPoint o, GeoPoint d) {
        new Thread(() -> {
            OSRMRoadManager rm = new OSRMRoadManager(this, getPackageName());
            rm.setMean(OSRMRoadManager.MEAN_BY_FOOT);
            ArrayList<GeoPoint> w = new ArrayList<>(); w.add(o); w.add(d);
            Road rd = rm.getRoad(w);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (rd.mStatus == Road.STATUS_OK) {
                    if (rutaActual != null) map.getOverlays().remove(rutaActual);
                    rutaActual = RoadManager.buildRoadOverlay(rd);
                    rutaActual.getOutlinePaint().setColor(Color.BLUE); rutaActual.getOutlinePaint().setStrokeWidth(12f);
                    map.getOverlays().add(rutaActual); map.invalidate();
                }
            });
        }).start();
    }

    private void configurarPulsacionLarga() {
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) { return false; }
            @Override public boolean longPressHelper(GeoPoint p) {
                puntoManual = p;
                if (marcadorManual != null) map.getOverlays().remove(marcadorManual);
                marcadorManual = new Marker(map); marcadorManual.setPosition(p);
                marcadorManual.setIcon(ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_mylocation, null));
                map.getOverlays().add(marcadorManual); map.invalidate();
                return true;
            }
        };
        map.getOverlays().add(new MapEventsOverlay(mReceive));
    }

    private void mostrarMapa(boolean v) { int vis = v ? View.VISIBLE : View.GONE; map.setVisibility(vis); findViewById(R.id.search_card).setVisibility(vis); }
    private void solicitarPermisos() { if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1); } }
    @Override protected void onResume() { super.onResume(); if (map != null) map.onResume(); }
    @Override protected void onPause() { super.onPause(); if (map != null) map.onPause(); }
}