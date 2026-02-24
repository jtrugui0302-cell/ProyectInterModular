package DatabaseConnection;

import com.example.proyectoaplicacinturismolocal.Models.Comentarios;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnector {
    private static final String IP_SERVIDOR = "45.83.102.230";
    private static final String DB_NAME = "rutas_turisticas";
    private static final String URL = "jdbc:mysql://" + IP_SERVIDOR + ":33065/" + DB_NAME + "?useSSL=false";
    private static final String USER = "admin";
    private static final String PASS = "nocturno";

    public interface DatabaseListener {
        void onSitioEncontrado(int id, String nombre, double lat, double lon, String desc, String imagenUrl, String tipo);
        void onError(String mensaje);
        void onFinalizado();
    }
    public interface LoginListener {
        void onResult(int id, String nombre, String apellido, String gmail);
        void onError(String mensaje);
    }
    public interface RegisterListener {
        void onSuccess(String mensaje);
        void onError(String mensaje);
    }
    public interface CommentListener {
        void onCommentsLoaded(List<Comentarios> comments);
        void onCommentAdded(boolean exito);
        void onError(String mensaje);
    }

    public void ejecutarConsulta(DatabaseListener listener) {
        new Thread(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                try (Connection con = DriverManager.getConnection(URL, USER, PASS);
                     Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery("SELECT id, nombre, latitud, longitud, descripcion, tipo, imagen_url FROM sitios")) {
                    while (rs.next()) {
                        listener.onSitioEncontrado(rs.getInt("id"), rs.getString("nombre"), rs.getDouble("latitud"), rs.getDouble("longitud"), rs.getString("descripcion"), rs.getString("imagen_url"), rs.getString("tipo"));
                    }
                }
            } catch (Exception e) { listener.onError(e.getMessage()); }
            finally { listener.onFinalizado(); }
        }).start();
    }

    public void loginUsuario(String gmail, String pass, LoginListener listener) {
        new Thread(() -> {
            try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "SELECT id, nombre, apellido, gmail FROM usuarios WHERE gmail=? AND password_hash=?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, gmail); ps.setString(2, pass);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) listener.onResult(rs.getInt("id"), rs.getString("nombre"), rs.getString("apellido"), rs.getString("gmail"));
                else listener.onResult(-1, null, null, null);
            } catch (Exception e) { listener.onError(e.getMessage()); }
        }).start();
    }

    // AÑADIDO: Método para que el Login funcione correctamente
    public void registrarUsuario(String nom, String ape, int ed, String gm, String tel, String pw, RegisterListener listener) {
        new Thread(() -> {
            try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "INSERT INTO usuarios (nombre, apellido, edad, gmail, numero_telefono, tipo, password_hash) VALUES (?, ?, ?, ?, ?, 1, ?)";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, nom); ps.setString(2, ape); ps.setInt(3, ed);
                ps.setString(4, gm); ps.setString(5, tel); ps.setString(6, pw);
                ps.executeUpdate();
                listener.onSuccess("¡Usuario registrado con éxito!");
            } catch (Exception e) { listener.onError(e.getMessage()); }
        }).start();
    }

    public void insertarComentario(int idUser, int idSitio, String texto, CommentListener listener) {
        new Thread(() -> {
            try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "INSERT INTO comentarios (contenidoComentario, usuario_id, sitio_id, valoraciones) VALUES (?, ?, ?, 0)";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, texto); ps.setInt(2, idUser); ps.setInt(3, idSitio);
                ps.executeUpdate();
                listener.onCommentAdded(true);
            } catch (Exception e) { listener.onError(e.getMessage()); }
        }).start();
    }

    public void obtenerComentariosSitio(int idSitio, CommentListener listener) {
        new Thread(() -> {
            List<Comentarios> lista = new ArrayList<>();
            try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "SELECT u.nombre, c.contenidoComentario, c.fechaSubida FROM comentarios c " +
                        "JOIN usuarios u ON c.usuario_id = u.id WHERE c.sitio_id = ? ORDER BY c.fechaSubida DESC";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setInt(1, idSitio);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    lista.add(new Comentarios(rs.getString("nombre"), rs.getString("contenidoComentario"), rs.getString("fechaSubida")));
                }
                listener.onCommentsLoaded(lista);
            } catch (Exception e) { listener.onError(e.getMessage()); }
        }).start();
    }

    public void obtenerComentariosUsuario(int idUser, CommentListener listener) {
        new Thread(() -> {
            List<Comentarios> lista = new ArrayList<>();
            try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "SELECT s.nombre, c.contenidoComentario, c.fechaSubida FROM comentarios c " +
                        "JOIN sitios s ON c.sitio_id = s.id WHERE c.usuario_id = ? ORDER BY c.fechaSubida DESC";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setInt(1, idUser);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    lista.add(new Comentarios(rs.getString("nombre"), rs.getString("contenidoComentario"), rs.getString("fechaSubida")));
                }
                listener.onCommentsLoaded(lista);
            } catch (Exception e) { listener.onError(e.getMessage()); }
        }).start();
    }
}