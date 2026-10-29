
package umg.edu.gt.controlgastos

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import umg.edu.gt.controlgastos.data.local.AppDatabase
import umg.edu.gt.controlgastos.data.local.AutenticacionRepository

class RegistroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Conectamos los controles de la pantalla.
        val etUsuario = findViewById<EditText>(R.id.etRegistroUsuario)
        val etPassword = findViewById<EditText>(R.id.etRegistroPassword)
        val etConfirmar = findViewById<EditText>(
            R.id.etRegistroConfirmarPassword
        )
        val btnCrearCuenta = findViewById<Button>(R.id.btnCrearCuenta)
        val btnIrLogin = findViewById<Button>(R.id.btnIrLogin)

        // Preparamos el acceso a la base de datos.
        val database = AppDatabase.obtenerInstancia(this)
        val autenticacion = AutenticacionRepository(database.usuarioDao())

        // Registrar un usuario nuevo.
        btnCrearCuenta.setOnClickListener {

            val usuario = etUsuario.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmar = etConfirmar.text.toString()

            // Validamos los campos.
            if (usuario.isBlank()) {
                etUsuario.error = "Ingresa un nombre de usuario"
                return@setOnClickListener
            }

            if (password.isBlank()) {
                etPassword.error = "Ingresa una contraseña"
                return@setOnClickListener
            }

            if (password != confirmar) {
                etConfirmar.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            // Evitamos registros repetidos mientras se procesa.
            btnCrearCuenta.isEnabled = false

            lifecycleScope.launch {
                try {
                    val error = autenticacion.registrar(usuario, password)

                    if (error == null) {
                        Toast.makeText(
                            this@RegistroActivity,
                            "Cuenta creada correctamente",
                            Toast.LENGTH_LONG
                        ).show()

                        // Regresamos a la pantalla anterior.
                        finish()
                    } else {
                        Toast.makeText(
                            this@RegistroActivity,
                            error,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@RegistroActivity,
                        "No se pudo crear la cuenta",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    btnCrearCuenta.isEnabled = true
                }
            }
        }

        // Volver a la pantalla anterior.
        btnIrLogin.setOnClickListener {
            finish()
        }
    }
}
