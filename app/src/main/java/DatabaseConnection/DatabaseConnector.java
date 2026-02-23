package DatabaseConnection;

import android.util.Log;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnector {

    private static final String IP_SERVIDOR = "192.168.0.65";
    private static final String DB_NAME = "rutas_turisticas";
    private static final String URL = "jdbc:mysql://" + IP_SERVIDOR + ":33065/" + DB_NAME + "?useSSL=false";
    private static final String USER = "admin";
    private static final String PASS = "nocturno";

    public interface DatabaseListener {
        void onSitioEncontrado(String nombre, double lat, double lon, String desc, String imagenUrl, String tipo);
        void onError(String mensaje);
        void onFinalizado();
    }

    public interface LoginListener {
        void onResult(int idUsuario, String nombre);
        void onError(String mensaje);
    }

    public interface RegisterListener {
        void onSuccess(String mensaje);
        void onError(String mensaje);
    }

    // --- CONSULTA DE SITIOS (TU CÓDIGO ORIGINAL) ---
    public void ejecutarConsulta(DatabaseListener listener) {
        new Thread(() -> {
            Connection con = null;
            Statement st = null;
            ResultSet rs = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                con = DriverManager.getConnection(URL, USER, PASS);
                st = con.createStatement();
                rs = st.executeQuery("SELECT nombre, latitud, longitud, descripcion, tipo, imagen_url FROM sitios");

                while (rs.next()) {
                    listener.onSitioEncontrado(
                            rs.getString("nombre"), rs.getDouble("latitud"),
                            rs.getDouble("longitud"), rs.getString("descripcion"),
                            rs.getString("imagen_url"), rs.getString("tipo")
                    );
                }
            } catch (Exception e) {
                listener.onError(e.getMessage());
            } finally {
                listener.onFinalizado();
                cerrarRecursos(con, st, rs);
            }
        }).start();
    }

    // --- LOGIN: USANDO 'gmail' Y 'password_hash' ---
    public void loginUsuario(String gmail, String pass, LoginListener listener) {
        new Thread(() -> {
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                con = DriverManager.getConnection(URL, USER, PASS);
                // Buscamos por gmail y password_hash según tu tabla
                ps = con.prepareStatement("SELECT id, nombre FROM usuarios WHERE gmail=? AND password_hash=?");
                ps.setString(1, gmail);
                ps.setString(2, pass);
                rs = ps.executeQuery();
                if (rs.next()) listener.onResult(rs.getInt("id"), rs.getString("nombre"));
                else listener.onResult(-1, null);
            } catch (Exception e) {
                listener.onError(e.getMessage());
            } finally { cerrarRecursos(con, ps, rs); }
        }).start();
    }

    // --- REGISTRO: CON TODOS LOS CAMPOS DE TU IMAGEN ---
    public void registrarUsuario(String nombre, String apellido, int edad, String gmail, String telefono, String pass, RegisterListener listener) {
        new Thread(() -> {
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DriverManager.getConnection(URL, USER, PASS);
                // INSERT según el orden de tu tabla (id es auto_increment)
                String query = "INSERT INTO usuarios (nombre, apellido, edad, gmail, numero_telefono, tipo, password_hash) VALUES (?, ?, ?, ?, ?, ?, ?)";
                ps = con.prepareStatement(query);
                ps.setString(1, nombre);
                ps.setString(2, apellido);
                ps.setInt(3, edad);
                ps.setString(4, gmail);
                ps.setString(5, telefono);
                ps.setInt(6, 1); // Tipo por defecto (ej: 1 para usuario normal)
                ps.setString(7, pass);

                ps.executeUpdate();
                listener.onSuccess("¡Registro completado con éxito!");
            } catch (Exception e) {
                listener.onError("Error: " + e.getMessage());
            } finally { cerrarRecursos(con, ps, null); }
        }).start();
    }

    private void cerrarRecursos(Connection con, Statement st, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (st != null) st.close();
            if (con != null) con.close();
        } catch (Exception e) { Log.e("MYSQL_DEBUG", "Cerrado: " + e.getMessage()); }
    }
}