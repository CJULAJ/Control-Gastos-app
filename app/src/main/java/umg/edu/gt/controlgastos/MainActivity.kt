package umg.edu.gt.controlgastos

import android.R
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import edu.umg.programacion2.gastos.datos.GastoRepositoryEnMemoria
import edu.umg.programacion2.gastos.modelo.Categoria
import edu.umg.programacion2.gastos.modelo.Gasto
import edu.umg.programacion2.gastos.servicio.ControlGastos
import kotlinx.coroutines.launch
import umg.edu.gt.controlgastos.databinding.ActivityMainBinding
import umg.edu.gt.controlgastos.ui.theme.ControlGastosTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    // lateinit = "la inicializo después, en onCreate". Sin esto Kotlin exige un valor ya.
    private lateinit var binding: ActivityMainBinding

    // IMPORTANTE: la MISMA línea que en el Main.kt de consola. La lógica no cambió.
    private val control = ControlGastos(GastoRepositoryEnMemoria())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Inflar el XML con View Binding y mostrarlo.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. El enum de la librería alimenta el Spinner (muestra el toString(): "Alimentación").
        binding.spCategoria.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, Categoria.entries
        )

        // 3. Evento clic: el bloque { } se ejecuta DESPUÉS, cuando el usuario toca el botón.
        binding.btnGuardar.setOnClickListener { guardar() }

        // 4. resumen() es suspend -> se llama dentro de lifecycleScope.launch (receta).
        lifecycleScope.launch { actualizarResumen() }
    }

    private fun guardar() {
        // ❌ Error común: .toDouble() cierra la app si el campo está vacío.
        // ✅ toDoubleOrNull() + early return (null safety de la Clase 11).
        val monto = binding.etMonto.text.toString().toDoubleOrNull()
        if (monto == null) {
            binding.etMonto.error = "Ingresa un monto válido"
            return
        }

        val gasto = Gasto(
            descripcion = binding.etDescripcion.text.toString().trim(),
            monto = monto,
            categoria = binding.spCategoria.selectedItem as Categoria,
            fecha = fechaDeHoy()
        )

        lifecycleScope.launch {
            // 5. La validación vive en la librería, no en la pantalla.
            val error = control.registrar(gasto)
            if (error != null) {
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                return@launch   // return dentro de launch: sale solo del bloque
            }
            binding.etDescripcion.text.clear()
            binding.etMonto.text.clear()
            actualizarResumen()
        }
    }

    private suspend fun actualizarResumen() {
        binding.tvResumen.text = control.resumen()   // "2 gastos · Total: Q 55.50"
        binding.tvHolaMundo.text = "Hola que hace!!!"
    }

    // Cuidado: java.time.LocalDate necesita API 26; con minSdk 24 usamos SimpleDateFormat.
    private fun fechaDeHoy(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

}
