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
import com.google.android.material.button.MaterialButton;

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
    private View searchCard, favoriteLayout;
    private List<Sitio> listaSitiosCompleta = new ArrayList<>();
    private List<Sitio> listaFavoritos = new ArrayList<>();
    private FavoritosAdapter favAdapter;
    private RecyclerView rvFav;
    private MyLocationNewOverlay myLocationOverlay;
    private Polyline rutaActual;
    private Sitio sitioEnNavegacion = null;
    private GeoPoint puntoManual = null;
    private Marker marcadorManual = null;
    private int currentTabId = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_main);

        solicitarPermisos();
        map = findViewById(R.id.map);
        fragmentContainer = findViewById(R.id.fragment_container);
        searchCard = findViewById(R.id.search_card);
        favoriteLayout = findViewById(R.id.favorite_layout);
        dbConnector = new DatabaseConnector();

        gestionarVisibilidad(R.id.nav_home);

        EditText searchEdit = findViewById(R.id.edit_text_search);
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filtrarSitios(s.toString()); }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        rvFav = findViewById(R.id.recycler_view_favoritos);
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

        findViewById(R.id.btn_zoom_in).setOnClickListener(v -> map.getController().zoomIn());
        findViewById(R.id.btn_zoom_out).setOnClickListener(v -> map.getController().zoomOut());

        conectarYObtenerSitios();
        configurarNavegacion();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        if (currentTabId == R.id.nav_profile) mostrarPerfil();
    }

    private void gestionarVisibilidad(int navId) {
        this.currentTabId = navId;
        map.setVisibility(View.GONE);
        searchCard.setVisibility(View.GONE);
        favoriteLayout.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.GONE);
        findViewById(R.id.btn_zoom_in).setVisibility(View.GONE);
        findViewById(R.id.btn_zoom_out).setVisibility(View.GONE);

        if (navId == R.id.nav_home) {
            map.setVisibility(View.VISIBLE);
            searchCard.setVisibility(View.VISIBLE);
            findViewById(R.id.btn_zoom_in).setVisibility(View.VISIBLE);
            findViewById(R.id.btn_zoom_out).setVisibility(View.VISIBLE);
        } else if (navId == R.id.nav_favorites) {
            favoriteLayout.setVisibility(View.VISIBLE);
        } else if (navId == R.id.nav_profile) {
            fragmentContainer.setVisibility(View.VISIBLE);
        }
    }

    private void configurarNavegacion() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            gestionarVisibilidad(item.getItemId());
            if (item.getItemId() == R.id.nav_profile) mostrarPerfil();
            return true;
        });
    }

    private void mostrarPerfil() {
        fragmentContainer.removeAllViews();
        View v = LayoutInflater.from(this).inflate(R.layout.profile_layout, null);
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int uid = prefs.getInt("userId", -1);

        TextView txtNom = v.findViewById(R.id.perfil_nombre_completo);
        TextView txtMail = v.findViewById(R.id.perfil_gmail);
        Button btnLogin = v.findViewById(R.id.btn_login);
        Button btnLogout = v.findViewById(R.id.btn_logout);
        RecyclerView rvMisCom = v.findViewById(R.id.recycler_comentarios_usuario);

        if (uid == -1) {
            txtNom.setText("Modo Invitado");
            if (btnLogin != null) {
                btnLogin.setVisibility(View.VISIBLE);
                btnLogin.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, LoginActivity.class)));
            }
            if (btnLogout != null) btnLogout.setVisibility(View.GONE);
        } else {
            txtNom.setText(prefs.getString("userName", "") + " " + prefs.getString("userApellido", ""));
            txtMail.setText(prefs.getString("userEmail", ""));
            if (btnLogin != null) btnLogin.setVisibility(View.GONE);
            if (btnLogout != null) {
                btnLogout.setVisibility(View.VISIBLE);
                btnLogout.setOnClickListener(view -> { prefs.edit().clear().apply(); mostrarPerfil(); });
            }

            if (rvMisCom != null) {
                rvMisCom.setLayoutManager(new LinearLayoutManager(this));
                // SOLUCIÓN AL ERROR DE LISTENER (CLASE ANÓNIMA COMPLETA)
                dbConnector.obtenerComentariosUsuario(uid, new DatabaseConnector.CommentListener() {
                    @Override public void onCommentsLoaded(List<Comentarios> list) {
                        runOnUiThread(() -> rvMisCom.setAdapter(new ComentarioAdapter(list, c -> {
                            // Diálogo para confirmar borrado
                            new android.app.AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Borrar comentario")
                                    .setMessage("¿Estás seguro?")
                                    .setPositiveButton("Sí", (dialog, which) -> {
                                        dbConnector.eliminarComentario(c.getId(), new DatabaseConnector.CommentListener() {
                                            @Override public void onCommentAdded(boolean exito) { runOnUiThread(() -> mostrarPerfil()); }
                                            @Override public void onCommentsLoaded(List<Comentarios> l) {}
                                            @Override public void onError(String m) {}
                                        });
                                    })
                                    .setNegativeButton("No", null).show();
                        })));
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
        ((TextView)view.findViewById(R.id.detalle_categoria)).setText("Categoría: " + s.getTipo());
        Glide.with(this).load(s.getUrlImagen()).into((ImageView)view.findViewById(R.id.detalle_imagen));

        ImageButton btnFav = view.findViewById(R.id.btn_favorito);
        if (listaFavoritos.contains(s)) btnFav.setImageResource(R.drawable.favorito);
        btnFav.setOnClickListener(v -> {
            if (!listaFavoritos.contains(s)) { listaFavoritos.add(s); btnFav.setImageResource(R.drawable.favorito); }
            else { listaFavoritos.remove(s); btnFav.setImageResource(R.drawable.estrella); }
            favAdapter.notifyDataSetChanged();
        });

        MaterialButton btnNavegar = view.findViewById(R.id.btn_navegar);
        if (rutaActual != null && sitioEnNavegacion != null && sitioEnNavegacion.getId() == s.getId()) btnNavegar.setText("Dejar de navegar");
        else btnNavegar.setText("Navegar");

        btnNavegar.setOnClickListener(v -> {
            if (rutaActual != null && sitioEnNavegacion != null && sitioEnNavegacion.getId() == s.getId()) detenerNavegacion();
            else {
                GeoPoint d = new GeoPoint(s.getLatitud(), s.getLongitud());
                GeoPoint o = (puntoManual != null) ? puntoManual : (myLocationOverlay.getMyLocation() != null ? myLocationOverlay.getMyLocation() : (GeoPoint)map.getMapCenter());
                trazarRuta(o, d, s);
            }
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_comentario).setOnClickListener(v -> {
            if (getSharedPreferences("UserPrefs", MODE_PRIVATE).getInt("userId", -1) == -1) {
                Toast.makeText(this, "Regístrate para comentar", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
            } else { abrirSocial(s); }
            dialog.dismiss();
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
                runOnUiThread(() -> rv.setAdapter(new ComentarioAdapter(list, null)));
            }
            @Override public void onCommentAdded(boolean e) {}
            @Override public void onError(String m) {}
        });

        v.findViewById(R.id.btn_enviar).setOnClickListener(view -> {
            int uid = getSharedPreferences("UserPrefs", MODE_PRIVATE).getInt("userId", -1);
            String txt = ((EditText)v.findViewById(R.id.et_nuevo_comentario)).getText().toString().trim();
            if(!txt.isEmpty()){
                dbConnector.insertarComentario(uid, s.getId(), txt, new DatabaseConnector.CommentListener() {
                    @Override public void onCommentAdded(boolean e) {
                        runOnUiThread(() -> { Toast.makeText(MainActivity.this, "Publicado", Toast.LENGTH_SHORT).show(); socialDialog.dismiss(); });
                    }
                    @Override public void onCommentsLoaded(List<Comentarios> l) {}
                    @Override public void onError(String m) {}
                });
            }
        });
        socialDialog.setContentView(v); socialDialog.show();
    }

    private void conectarYObtenerSitios() {
        dbConnector.ejecutarConsulta(new DatabaseConnector.DatabaseListener() {
            @Override public void onSitioEncontrado(int id, String n, double la, double lo, String d, String u, String t) {
                Sitio s = new Sitio(n, d, u, t); s.setLatitud(la); s.setLongitud(lo); s.setId(id);
                listaSitiosCompleta.add(s);
                new Handler(Looper.getMainLooper()).post(() -> crearMarcador(s));
            }
            @Override public void onError(String m) {}
            @Override public void onFinalizado() { runOnUiThread(() -> findViewById(R.id.loadingLayout).setVisibility(View.GONE)); }
        });
    }

    private void trazarRuta(GeoPoint o, GeoPoint d, Sitio s) {
        new Thread(() -> {
            OSRMRoadManager rm = new OSRMRoadManager(this, getPackageName());
            ArrayList<GeoPoint> w = new ArrayList<>(); w.add(o); w.add(d);
            Road rd = rm.getRoad(w);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (rd.mStatus == Road.STATUS_OK) {
                    if (rutaActual != null) map.getOverlays().remove(rutaActual);
                    rutaActual = RoadManager.buildRoadOverlay(rd);
                    rutaActual.getOutlinePaint().setColor(Color.BLUE); rutaActual.getOutlinePaint().setStrokeWidth(12f);
                    map.getOverlays().add(rutaActual); sitioEnNavegacion = s; map.invalidate();
                }
            });
        }).start();
    }

    private void detenerNavegacion() {
        if (rutaActual != null) { map.getOverlays().remove(rutaActual); rutaActual = null; sitioEnNavegacion = null; map.invalidate(); }
    }

    private void crearMarcador(Sitio s) {
        Marker m = new Marker(map); m.setPosition(new GeoPoint(s.getLatitud(), s.getLongitud())); m.setTitle(s.getNombre());
        m.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.marcador, null));
        m.setOnMarkerClickListener((mk, mv) -> { mostrarDetalle(s); return true; });
        map.getOverlays().add(m);
    }

    private void filtrarSitios(String t) {
        String q = t.toLowerCase(); map.getOverlays().clear();
        if (myLocationOverlay != null) map.getOverlays().add(myLocationOverlay);
        if (marcadorManual != null) map.getOverlays().add(marcadorManual);
        if (rutaActual != null) map.getOverlays().add(rutaActual);
        configurarPulsacionLarga();
        for (Sitio s : listaSitiosCompleta) {
            if (s.getNombre().toLowerCase().contains(q) || (s.getTipo() != null && s.getTipo().toLowerCase().contains(q))) crearMarcador(s);
        }
        map.invalidate();
    }

    private void configurarPulsacionLarga() {
        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) { return false; }
            @Override public boolean longPressHelper(GeoPoint p) {
                puntoManual = p;
                if (marcadorManual != null) map.getOverlays().remove(marcadorManual);
                marcadorManual = new Marker(map); marcadorManual.setPosition(p);
                marcadorManual.setIcon(ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_mylocation, null));
                map.getOverlays().add(marcadorManual); map.invalidate();
                return true;
            }
        }));
    }

    private void solicitarPermisos() { if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1); }
    @Override protected void onPause() { super.onPause(); if (map != null) map.onPause(); }
}