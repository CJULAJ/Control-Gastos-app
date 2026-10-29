
package umg.edu.gt.controlgastos

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import umg.edu.gt.controlgastos.data.local.AppDatabase
import umg.edu.gt.controlgastos.data.local.AutenticacionRepository

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Comprobamos si ya existe una sesión.
        val preferencias = getSharedPreferences(
            "sesion_usuario",
            MODE_PRIVATE
        )

        val usuarioId = preferencias.getInt("usuario_id", -1)

        if (usuarioId != -1) {
            abrirPanel()
            return
        }

        setContentView(R.layout.activity_login)

        // Controles de la pantalla.
        val etUsuario = findViewById<EditText>(R.id.etLoginUsuario)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val btnLogin = findViewById<Button>(R.id.btnIniciarSesion)
        val btnRegistro = findViewById<Button>(R.id.btnIrRegistro)

        // Conexión con la base de datos.
        val database = AppDatabase.obtenerInstancia(this)
        val autenticacion = AutenticacionRepository(database.usuarioDao())

        // Abrir la pantalla de registro.
        btnRegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

        // Iniciar sesión.
        btnLogin.setOnClickListener {

            val nombre = etUsuario.text.toString().trim()
            val password = etPassword.text.toString()

            // Validar campos.
            if (nombre.isBlank()) {
                etUsuario.error = "Ingresa tu nombre de usuario"
                return@setOnClickListener
            }

            if (password.isBlank()) {
                etPassword.error = "Ingresa tu contraseña"
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            lifecycleScope.launch {
                try {
                    // Comprobar las credenciales en SQLite.
                    val usuario = autenticacion.iniciarSesion(
                        nombre,
                        password
                    )

                    if (usuario != null) {

                        // Guardar la sesión local.
                        preferencias.edit()
                            .putInt("usuario_id", usuario.id)
                            .apply()

                        Toast.makeText(
                            this@LoginActivity,
                            "Bienvenido, ${usuario.nombreUsuario}",
                            Toast.LENGTH_SHORT
                        ).show()

                        abrirPanel()

                    } else {
                        etPassword.text.clear()

                        Toast.makeText(
                            this@LoginActivity,
                            "Usuario o contraseña incorrectos",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        this@LoginActivity,
                        "No se pudo iniciar sesión",
                        Toast.LENGTH_LONG
                    ).show()

                } finally {
                    btnLogin.isEnabled = true
                }
            }
        }
    }

    // Abre el panel principal de gastos.
    private fun abrirPanel() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
