package LoginResources;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyectoaplicacinturismolocal.R;
import DatabaseConnection.DatabaseConnector;

public class LoginActivity extends AppCompatActivity {
    private boolean modoLogin = true;
    private EditText edNombre, edPass, edGmail, edApellido, edEdad, edTelefono;
    private Button btnConfirmar;
    private TextView txtCambiarModo;
    private DatabaseConnector db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new DatabaseConnector();
        edNombre = findViewById(R.id.edit_user);
        edApellido = findViewById(R.id.edit_apellido);
        edEdad = findViewById(R.id.edit_edad);
        edTelefono = findViewById(R.id.edit_telefono);
        edGmail = findViewById(R.id.edit_email);
        edPass = findViewById(R.id.edit_pass);
        btnConfirmar = findViewById(R.id.btn_action);
        txtCambiarModo = findViewById(R.id.txt_toggle);

        txtCambiarModo.setOnClickListener(v -> {
            modoLogin = !modoLogin;
            int vis = modoLogin ? View.GONE : View.VISIBLE;
            edNombre.setVisibility(vis); edApellido.setVisibility(vis);
            edEdad.setVisibility(vis); edTelefono.setVisibility(vis);
            btnConfirmar.setText(modoLogin ? "Iniciar Sesión" : "Registrarse");
            txtCambiarModo.setText(modoLogin ? "¿No tienes cuenta? Regístrate" : "¿Ya tienes cuenta? Entra");
        });

        btnConfirmar.setOnClickListener(v -> {
            String gmail = edGmail.getText().toString().trim();
            String pass = edPass.getText().toString().trim();
            if (modoLogin) {
                db.loginUsuario(gmail, pass, new DatabaseConnector.LoginListener() {
                    @Override public void onResult(int id, String nom, String ape, String mail) {
                        if (id != -1) {
                            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                    .putInt("userId", id).putString("userName", nom)
                                    .putString("userApellido", ape).putString("userEmail", mail).apply();
                            runOnUiThread(() -> finish());
                        } else {
                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Datos incorrectos", Toast.LENGTH_SHORT).show());
                        }
                    }
                    @Override public void onError(String m) { runOnUiThread(() -> Toast.makeText(LoginActivity.this, m, Toast.LENGTH_SHORT).show()); }
                });
            } else {
                db.registrarUsuario(edNombre.getText().toString(), edApellido.getText().toString(),
                        Integer.parseInt(edEdad.getText().toString().isEmpty() ? "0" : edEdad.getText().toString()),
                        gmail, edTelefono.getText().toString(), pass, new DatabaseConnector.RegisterListener() {
                            @Override public void onSuccess(String m) { runOnUiThread(() -> txtCambiarModo.performClick()); }
                            @Override public void onError(String m) { runOnUiThread(() -> Toast.makeText(LoginActivity.this, m, Toast.LENGTH_SHORT).show()); }
                        });
            }
        });
    }
}