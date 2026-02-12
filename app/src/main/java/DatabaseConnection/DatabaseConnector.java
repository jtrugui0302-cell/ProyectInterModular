package DatabaseConnection;

import android.util.Log;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseConnector {

    private static final String IP_SERVIDOR = "192.168.0.158";
    private static final String DB_NAME = "rutas_turisticas";
    private static final String URL = "jdbc:mysql://" + IP_SERVIDOR + ":3306/" + DB_NAME + "?useSSL=false";
    private static final String USER = "admin";
    private static final String PASS = "nocturno";

    // 1. MODIFICADO: Agregamos String imagenUrl a la interfaz
    public interface DatabaseListener {
        void onSitioEncontrado(String nombre, double lat, double lon, String desc, String imagenUrl);
        void onError(String mensaje);
        void onFinalizado();
    }

    public void ejecutarConsulta(DatabaseListener listener) {
        new Thread(() -> {
            Connection con = null;
            Statement st = null;
            ResultSet rs = null;

            try {
                Class.forName("com.mysql.jdbc.Driver");
                con = DriverManager.getConnection(URL, USER, PASS);
                st = con.createStatement();

                // 2. MODIFICADO: Añadimos la columna 'imagen' a la consulta SQL
                rs = st.executeQuery("SELECT nombre, latitud, longitud, descripcion, imagen_url FROM sitios");

                while (rs.next()) {
                    // 3. MODIFICADO: Extraemos el String de la URL y lo enviamos
                    listener.onSitioEncontrado(
                            rs.getString("nombre"),
                            rs.getDouble("latitud"),
                            rs.getDouble("longitud"),
                            rs.getString("descripcion"),
                            rs.getString("imagen_url")
                    );
                }

            } catch (Exception e) {
                Log.e("MYSQL_DEBUG", "Error: " + e.getMessage());
                listener.onError(e.getMessage());
            } finally {
                listener.onFinalizado();
                try {
                    if (rs != null) rs.close();
                    if (st != null) st.close();
                    if (con != null) con.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }
}