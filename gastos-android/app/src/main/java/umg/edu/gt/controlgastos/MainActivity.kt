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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import umg.edu.gt.controlgastos.data.local.AppDatabase
import umg.edu.gt.controlgastos.data.local.GastoRepositoryRoom
import umg.edu.gt.controlgastos.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Spinner
import android.content.Intent
import java.util.Calendar
import android.view.View
class MainActivity : AppCompatActivity() {

    // Permite utilizar los elementos del archivo XML.
    private lateinit var binding: ActivityMainBinding


    // Identifica al usuario que inició sesión.
    private val usuarioId: Int by lazy {
        getSharedPreferences(
            "sesion_usuario",
            MODE_PRIVATE
        ).getInt("usuario_id", -1)
    }

    // Conecta gastos-core con los gastos del usuario actual.
    private val control by lazy {
        val database = AppDatabase.obtenerInstancia(applicationContext)

        ControlGastos(
            GastoRepositoryRoom(database.gastoDao(), usuarioId)
        )
    }

    // Recibe el gasto seleccionado para editarlo o eliminarlo.
    private val gastoAdapter = GastoAdapter(
        onEditar = { gasto ->
            mostrarEditarGasto(gasto)
        },
        onEliminar = { gasto ->
            confirmarEliminacion(gasto)
        }
    )

    override fun onResume() {
        super.onResume()
        actualizarSaludoUsuario()
    }
    // Configura la pantalla al abrir la aplicación.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verifica que exista una sesión iniciada.
        val preferencias = getSharedPreferences(
            "sesion_usuario",
            MODE_PRIVATE
        )

        val usuarioId = preferencias.getInt("usuario_id", -1)

        // Impide entrar al panel sin iniciar sesión.
        if (usuarioId == -1) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Carga el diseño XML.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actualizarSaludoUsuario()

        binding.tvToggleRegistro.setOnClickListener {
            val abierto = binding.contenidoRegistro.visibility == View.VISIBLE

            binding.contenidoRegistro.visibility =
                if (abierto) View.GONE else View.VISIBLE

            binding.tvToggleRegistro.text =
                if (abierto) "＋  Registrar nuevo gasto   ▾"
                else "＋  Registrar nuevo gasto   ▴"
        }

        binding.tvToggleFiltros.setOnClickListener {
            val abierto = binding.contenidoFiltros.visibility == View.VISIBLE

            binding.contenidoFiltros.visibility =
                if (abierto) View.GONE else View.VISIBLE

            binding.tvToggleFiltros.text =
                if (abierto) "☷  Filtrar movimientos   ▾"
                else "☷  Filtrar movimientos   ▴"
        }


// Cierra la sesión del usuario actual.
        binding.btnCerrarSesion.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Deseas salir de tu cuenta?")
                .setPositiveButton("Cerrar sesión") { _, _ ->

                    // Elimina la sesión guardada.
                    getSharedPreferences(
                        "sesion_usuario",
                        MODE_PRIVATE
                    ).edit().remove("usuario_id").apply()

                    // Regresa al inicio de sesión.
                    val intent = Intent(this, LoginActivity::class.java)

                    // Evita regresar al panel con el botón Atrás.
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }


        // Configura la lista de gastos.
        binding.rvGastos.layoutManager = LinearLayoutManager(this)
        binding.rvGastos.adapter = gastoAdapter

        // Muestra las categorías disponibles.
        binding.spCategoria.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Categoria.entries
        )
        // Prepara las categorías disponibles para filtrar.
        val categoriasFiltro = listOf("Todas las categorías") +
                Categoria.entries.map { it.name }

// Muestra las categorías en el selector de filtros.
        binding.spFiltroCategoria.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categoriasFiltro
        )

        // Guarda un gasto al presionar el botón.
        binding.btnGuardar.setOnClickListener {
            guardar()
        }

// Aplica los filtros al presionar el botón Filtrar.
        binding.btnFiltrar.setOnClickListener {
            filtrarGastos()
        }

// Limpia los filtros y muestra todos los gastos.
        binding.btnMostrarTodos.setOnClickListener {

            binding.spFiltroCategoria.setSelection(0)
            binding.etFiltroFecha.text.clear()

            lifecycleScope.launch {
                cargarGastos()
            }
        }

        // Muestra los gastos guardados al abrir la aplicación.
        lifecycleScope.launch {
            actualizarResumen()
            cargarGastos()
        }
    }


    private fun actualizarSaludoUsuario() {
        lifecycleScope.launch {
            val usuario = withContext(Dispatchers.IO) {
                AppDatabase.obtenerInstancia(applicationContext)
                    .usuarioDao()
                    .buscarPorId(usuarioId)
            }

            val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

            val saludo = when (hora) {
                in 5..11 -> "Buenos días"
                in 12..18 -> "Buenas tardes"
                else -> "Buenas noches"
            }

            val nombre = usuario?.nombreUsuario ?: "Usuario"

            binding.tvSaludoUsuario.text = "¡$saludo, $nombre!"
        }
    }

    private suspend fun cargarGastos() {
        try {
            // Consulta los gastos del usuario actual.
            val gastos = control.listar()

            // Actualiza la lista únicamente si la consulta funciona.
            gastoAdapter.actualizarGastos(gastos)

        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e

        } catch (e: Exception) {
            android.util.Log.e(
                "ControlGastos",
                "Error al consultar los gastos",
                e
            )

            Toast.makeText(
                this@MainActivity,
                "No se pudieron cargar los gastos. Intenta nuevamente.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // Filtra los gastos por categoría y fecha.
    private fun filtrarGastos() {

        // Obtiene los filtros seleccionados.
        val categoriaSeleccionada =
            binding.spFiltroCategoria.selectedItem.toString()

        val fechaSeleccionada =
            binding.etFiltroFecha.text.toString().trim()

        // Comprueba que la fecha tenga un formato válido.
        if (fechaSeleccionada.isNotEmpty()) {
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            formato.isLenient = false

            val fechaValida = try {
                val fecha = formato.parse(fechaSeleccionada)
                fecha != null &&
                        formato.format(fecha) == fechaSeleccionada
            } catch (e: java.text.ParseException) {
                false
            }

            if (!fechaValida) {
                binding.etFiltroFecha.error = "Usa el formato AAAA-MM-DD"
                return
            }
        }

        lifecycleScope.launch {

            // Consulta los gastos guardados en Room.
            val gastos = control.listar()

            // Aplica ambos filtros cuando corresponda.
            val gastosFiltrados = gastos.filter { gasto ->

                val coincideCategoria =
                    categoriaSeleccionada == "Todas las categorías" ||
                            gasto.categoria.name == categoriaSeleccionada

                val coincideFecha =
                    fechaSeleccionada.isEmpty() ||
                            gasto.fecha == fechaSeleccionada

                coincideCategoria && coincideFecha
            }

            // Muestra únicamente los gastos encontrados.
            gastoAdapter.actualizarGastos(gastosFiltrados)

            Toast.makeText(
                this@MainActivity,
                "${gastosFiltrados.size} gastos encontrados",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // Abre el formulario y permite modificar un gasto.
    private fun mostrarEditarGasto(gasto: Gasto) {

        // Carga el formulario de edición.
        val vista = LayoutInflater.from(this)
            .inflate(R.layout.dialog_editar_gasto, null)

        val descripcion = vista.findViewById<EditText>(
            R.id.etEditarDescripcion
        )

        val monto = vista.findViewById<EditText>(
            R.id.etEditarMonto
        )

        val categoria = vista.findViewById<Spinner>(
            R.id.spEditarCategoria
        )

        // Coloca los datos actuales del gasto.
        descripcion.setText(gasto.descripcion)
        monto.setText(gasto.monto.toString())

        categoria.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Categoria.entries
        )

        categoria.setSelection(
            Categoria.entries.indexOf(gasto.categoria)
        )

        // Prepara la ventana.
        val dialogo = AlertDialog.Builder(this)
            .setTitle("Editar gasto")
            .setView(vista)
            .setPositiveButton("Guardar cambios", null)
            .setNegativeButton("Cancelar", null)
            .create()

        // Configura el botón para validar antes de guardar.
        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val nuevaDescripcion =
                        descripcion.text.toString().trim()

                    val nuevoMonto =
                        monto.text.toString().toDoubleOrNull()

                    // Comprueba que la descripción no esté vacía.
                    if (nuevaDescripcion.isEmpty()) {
                        descripcion.error = "Ingresa una descripción"
                        return@setOnClickListener
                    }

                    // Comprueba que el monto sea válido.
                    if (nuevoMonto == null ||
                        !nuevoMonto.isFinite() ||
                        nuevoMonto <= 0.0
                    ) {
                        monto.error = "Ingresa un monto mayor que cero"
                        return@setOnClickListener
                    }

                    val nuevaCategoria =
                        categoria.selectedItem as Categoria

                    // Guarda los cambios en Room.
                    lifecycleScope.launch {

                        val dao = AppDatabase
                            .obtenerInstancia(applicationContext)
                            .gastoDao()

                        // Busca el registro original.
                        val gastoGuardado = dao.obtenerPorId(gasto.id, usuarioId)

                        if (gastoGuardado == null) {
                            Toast.makeText(
                                this@MainActivity,
                                "No se encontró el gasto",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialogo.dismiss()
                            return@launch
                        }

                        // Actualiza únicamente los campos editados.
                        dao.actualizar(
                            id = gasto.id,
                            usuarioId = usuarioId,
                            descripcion = nuevaDescripcion,
                            monto = nuevoMonto,
                            categoria = nuevaCategoria.name,
                            fecha = gasto.fecha
                        )

                        // Actualiza la lista y el resumen.
                        actualizarResumen()
                        cargarGastos()

                        Toast.makeText(
                            this@MainActivity,
                            "Gasto actualizado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        dialogo.dismiss()
                    }
                }
        }

        // Abre la ventana de edición.
        dialogo.show()
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
            try {
                // Intenta eliminar el gasto del usuario actual.
                val exito = control.eliminar(gasto.id)

                if (!exito) {
                    Toast.makeText(
                        this@MainActivity,
                        "No se encontró el gasto",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Actualiza el resumen y la lista.
                actualizarResumen()
                cargarGastos()

                Toast.makeText(
                    this@MainActivity,
                    "Gasto eliminado correctamente",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e

            } catch (e: Exception) {
                android.util.Log.e(
                    "ControlGastos",
                    "Error al eliminar o actualizar los gastos",
                    e
                )

                Toast.makeText(
                    this@MainActivity,
                    "Ocurrió un error al procesar la operación.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Valida los datos y registra un nuevo gasto.
    private fun guardar() {

        val descripcion = binding.etDescripcion.text.toString().trim()
        val monto = binding.etMonto.text.toString().toDoubleOrNull()

// Comprueba que la descripción no esté vacía.
        if (descripcion.isEmpty()) {
            binding.etDescripcion.error = "Ingresa una descripción"
            binding.etDescripcion.requestFocus()
            return
        }

// Comprueba que el monto sea válido.
        if (monto == null || !monto.isFinite() || monto <= 0.0) {
            binding.etMonto.error = "Ingresa un monto válido mayor que cero"
            binding.etMonto.requestFocus()
            return
        }


        // Obtiene los datos escritos por el usuario.
        val gasto = Gasto(
            descripcion = descripcion,
            monto = monto,
            categoria = binding.spCategoria.selectedItem as Categoria,
            fecha = fechaDeHoy()
        )

        lifecycleScope.launch {
            try {
                // Intenta registrar el gasto.
                val error = control.registrar(gasto)

                // Muestra los errores de validación.
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

                Toast.makeText(
                    this@MainActivity,
                    "Gasto registrado correctamente",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: kotlinx.coroutines.CancellationException) {
                // Respeta la cancelación de la corrutina.
                throw e

            } catch (e: Exception) {
                // Muestra un mensaje si ocurre un error inesperado.
                android.util.Log.e(
                    "ControlGastos",
                    "Error al registrar o actualizar los gastos",
                    e
                )

                Toast.makeText(
                    this@MainActivity,
                    "Ocurrió un error. Intenta nuevamente.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    }

    private suspend fun actualizarResumen() {
        try {
            // Obtiene el resumen de gastos del usuario actual.
            val resumen = control.resumen()

            // Actualiza el texto únicamente si la consulta funciona.
            binding.tvResumen.text = resumen

        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e

        } catch (e: Exception) {
            android.util.Log.e(
                "ControlGastos",
                "Error al actualizar el resumen de gastos",
                e
            )

            Toast.makeText(
                this@MainActivity,
                "No se pudo actualizar el resumen de gastos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Obtiene la fecha actual.
    private fun fechaDeHoy(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Date())
    }
}

