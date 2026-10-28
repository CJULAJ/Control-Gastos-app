package umg.edu.gt.controlgastos

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import edu.umg.programacion2.gastos.modelo.Categoria
import edu.umg.programacion2.gastos.modelo.Gasto
import edu.umg.programacion2.gastos.servicio.ControlGastos
import kotlinx.coroutines.launch
import umg.edu.gt.controlgastos.data.local.AppDatabase
import umg.edu.gt.controlgastos.data.local.GastoRepositoryRoom
import umg.edu.gt.controlgastos.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.appcompat.app.AlertDialog
class MainActivity : AppCompatActivity() {

    // Permite utilizar los elementos del archivo XML.
    private lateinit var binding: ActivityMainBinding

    // Conecta la lógica de gastos con la base de datos Room.
    private val control by lazy {
        val database = AppDatabase.obtenerInstancia(applicationContext)
        ControlGastos(GastoRepositoryRoom(database.gastoDao()))
    }

    // Adaptador que muestra los gastos en la lista.
    // Recibe el gasto seleccionado cuando se presiona Eliminar.
    private val gastoAdapter = GastoAdapter(
        onEliminar = { gasto ->
            confirmarEliminacion(gasto)
        }
    )

    // Configura la pantalla al abrir la aplicación.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Carga el diseño XML.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura la lista de gastos.
        binding.rvGastos.layoutManager = LinearLayoutManager(this)
        binding.rvGastos.adapter = gastoAdapter

        // Muestra las categorías disponibles.
        binding.spCategoria.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Categoria.entries
        )

        // Guarda un gasto al presionar el botón.
        binding.btnGuardar.setOnClickListener {
            guardar()
        }

        // Muestra los gastos guardados al abrir la aplicación.
        lifecycleScope.launch {
            actualizarResumen()
            cargarGastos()
        }
    }

    // Consulta Room y muestra los gastos en la lista.
    private suspend fun cargarGastos() {
        val database = AppDatabase.obtenerInstancia(applicationContext)

        val gastos = database.gastoDao().obtenerTodos().map { entidad ->
            Gasto(
                id = entidad.id,
                descripcion = entidad.descripcion,
                monto = entidad.monto,
                categoria = Categoria.valueOf(entidad.categoria),
                fecha = entidad.fecha
            )
        }

        gastoAdapter.actualizarGastos(gastos)
    }
    // Pregunta al usuario antes de eliminar un gasto.
    private fun confirmarEliminacion(gasto: Gasto) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar gasto")
            .setMessage(
                "¿Estás seguro de que deseas eliminar '${gasto.descripcion}'?"
            )
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarGasto(gasto)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Elimina un gasto de Room y actualiza la pantalla.
    private fun eliminarGasto(gasto: Gasto) {
        lifecycleScope.launch {

            // Obtiene la base de datos.
            val database = AppDatabase.obtenerInstancia(applicationContext)
            val dao = database.gastoDao()

            // Busca el gasto seleccionado por su identificador.
            val gastoGuardado = dao.obtenerPorId(gasto.id)

            if (gastoGuardado == null) {
                Toast.makeText(
                    this@MainActivity,
                    "No se encontró el gasto",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            // Elimina el gasto de la base de datos.
            dao.eliminar(gastoGuardado)

            // Actualiza el total y la lista.
            actualizarResumen()
            cargarGastos()

            // Confirma que el gasto fue eliminado.
            Toast.makeText(
                this@MainActivity,
                "Gasto eliminado correctamente",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    // Valida los datos y registra un nuevo gasto.
    private fun guardar() {
        val monto = binding.etMonto.text.toString().toDoubleOrNull()

        // Evita guardar montos incorrectos.
        if (monto == null || monto <= 0) {
            binding.etMonto.error = "Ingresa un monto mayor que cero"
            return
        }

        // Obtiene los datos escritos por el usuario.
        val gasto = Gasto(
            descripcion = binding.etDescripcion.text.toString().trim(),
            monto = monto,
            categoria = binding.spCategoria.selectedItem as Categoria,
            fecha = fechaDeHoy()
        )

        // Guarda el gasto utilizando la biblioteca gastos-core.
        lifecycleScope.launch {
            val error = control.registrar(gasto)

            if (error != null) {
                Toast.makeText(
                    this@MainActivity,
                    error,
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            // Limpia los campos después de guardar.
            binding.etDescripcion.text.clear()
            binding.etMonto.text.clear()

            // Actualiza el resumen y la lista.
            actualizarResumen()
            cargarGastos()
        }
    }

    // Actualiza el total de gastos.
    private suspend fun actualizarResumen() {
        binding.tvResumen.text = control.resumen()
        binding.tvHolaMundo.text = "Hola que hace!!!"
    }

    // Obtiene la fecha actual.
    private fun fechaDeHoy(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Date())
    }
}

