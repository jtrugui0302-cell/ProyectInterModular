package com.example.proyectoaplicacinturismolocal.LoginResources;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
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
        edNombre = findViewById(R.id.edit_user); // Usamos este para el Nombre
        edApellido = findViewById(R.id.edit_apellido); // Necesitas añadir estos IDs en tu XML
        edEdad = findViewById(R.id.edit_edad);
        edTelefono = findViewById(R.id.edit_telefono);
        edGmail = findViewById(R.id.edit_email);
        edPass = findViewById(R.id.edit_pass);

        btnConfirmar = findViewById(R.id.btn_action);
        txtCambiarModo = findViewById(R.id.txt_toggle);

        txtCambiarModo.setOnClickListener(v -> {
            modoLogin = !modoLogin;
            int visibilidadExtra = modoLogin ? View.GONE : View.VISIBLE;

            edApellido.setVisibility(visibilidadExtra);
            edEdad.setVisibility(visibilidadExtra);
            edTelefono.setVisibility(visibilidadExtra);

            // En login usamos Gmail y Pass. En registro usamos todo.
            edGmail.setVisibility(View.VISIBLE);

            btnConfirmar.setText(modoLogin ? "Iniciar Sesión" : "Crear Cuenta");
            txtCambiarModo.setText(modoLogin ? "¿No tienes cuenta? Regístrate" : "¿Ya tienes cuenta? Entra");
        });

        btnConfirmar.setOnClickListener(v -> {
            String gmail = edGmail.getText().toString().trim();
            String pass = edPass.getText().toString().trim();

            if (modoLogin) {
                db.loginUsuario(gmail, pass, new DatabaseConnector.LoginListener() {
                    @Override
                    public void onResult(int id, String nombre) {
                        if (id != -1) {
                            SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
                            editor.putInt("userId", id).putString("userName", nombre).apply();
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "Bienvenido " + nombre, Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Gmail o contraseña incorrectos", Toast.LENGTH_SHORT).show());
                        }
                    }
                    @Override public void onError(String m) { runOnUiThread(() -> Toast.makeText(LoginActivity.this, m, Toast.LENGTH_SHORT).show()); }
                });
            } else {
                // REGISTRO
                String nombre = edNombre.getText().toString().trim();
                String apellido = edApellido.getText().toString().trim();
                int edad = Integer.parseInt(edEdad.getText().toString().isEmpty() ? "0" : edEdad.getText().toString());
                String tel = edTelefono.getText().toString().trim();

                db.registrarUsuario(nombre, apellido, edad, gmail, tel, pass, new DatabaseConnector.RegisterListener() {
                    @Override
                    public void onSuccess(String m) {
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, m, Toast.LENGTH_SHORT).show();
                            txtCambiarModo.performClick();
                        });
                    }
                    @Override public void onError(String m) { runOnUiThread(() -> Toast.makeText(LoginActivity.this, m, Toast.LENGTH_SHORT).show()); }
                });
            }
        });
    }
}