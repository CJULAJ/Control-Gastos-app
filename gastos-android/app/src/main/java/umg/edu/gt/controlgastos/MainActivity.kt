package umg.edu.gt.controlgastos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import edu.umg.programacion2.gastos.modelo.Categoria
import edu.umg.programacion2.gastos.modelo.Gasto
import edu.umg.programacion2.gastos.servicio.ControlGastos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import umg.edu.gt.controlgastos.data.local.AppDatabase
import umg.edu.gt.controlgastos.data.local.GastoRepositoryRoom
import umg.edu.gt.controlgastos.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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


        binding.navInicio.setOnClickListener {
            mostrarSeccion("inicio")
        }

        binding.navGastos.setOnClickListener {
            mostrarSeccion("gastos")
        }

        binding.navAgregar.setOnClickListener {
            mostrarSeccion("agregar")
        }

        binding.navDatos.setOnClickListener {
            mostrarSeccion("datos")
        }

        binding.navPerfil.setOnClickListener {
            mostrarSeccion("perfil")
        }

// Muestra Inicio al abrir la aplicación.
        mostrarSeccion("inicio")


    }

    private fun mostrarSeccion(seccion: String) {

        // Oculta las secciones existentes.
        binding.seccionInicio.visibility = View.GONE
        binding.seccionResumenInicio.visibility = View.GONE
        binding.seccionMovimientos.visibility = View.GONE
        binding.seccionAgregar.visibility = View.GONE
        binding.seccionFiltros.visibility = View.GONE
        binding.btnCerrarSesion.visibility = View.GONE
        binding.seccionDatos.visibility = View.GONE
        binding.seccionPerfil.visibility = View.GONE

        when (seccion) {
            "inicio" -> {
                binding.seccionInicio.visibility = View.VISIBLE
                binding.seccionResumenInicio.visibility = View.VISIBLE

                // Mostrar filtros y movimientos debajo del resumen.
                binding.seccionFiltros.visibility = View.VISIBLE
                binding.seccionMovimientos.visibility = View.VISIBLE

                // Actualizar las tarjetas del inicio.
                actualizarResumenInicio()
            }

            "gastos" -> {
                binding.seccionMovimientos.visibility = View.VISIBLE
                binding.seccionFiltros.visibility = View.VISIBLE
            }

            "agregar" -> {
                binding.seccionAgregar.visibility = View.VISIBLE
                binding.contenidoRegistro.visibility = View.VISIBLE
                binding.tvToggleRegistro.text =
                    "＋  Registrar nuevo gasto   ▴"
            }

            "datos" -> {
                binding.seccionDatos.visibility = View.VISIBLE
                cargarEstadisticas()
            }

            "perfil" -> {
                binding.seccionPerfil.visibility = View.VISIBLE
                binding.btnCerrarSesion.visibility = View.VISIBLE
                cargarPerfil()
            }
        }

        // Regresa al inicio del contenido.
        binding.main.post {
            binding.main.scrollTo(0, 0)
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

    private fun actualizarResumenInicio() {
        lifecycleScope.launch {
            try {
                val gastos = control.listar()

                val formato = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                )

                val hoy = formato.format(Date())

                val calendario = Calendar.getInstance()
                calendario.add(Calendar.DAY_OF_YEAR, -6)

                val inicioPeriodo = formato.format(calendario.time)

                val totalHoy = gastos
                    .filter { it.fecha == hoy }
                    .sumOf { it.monto.toDouble() }

                val totalSemana = gastos
                    .filter {
                        it.fecha >= inicioPeriodo &&
                        it.fecha <= hoy
                    }
                    .sumOf { it.monto.toDouble() }

                binding.tvGastosHoy.text = String.format(
                    Locale.US,
                    "Q %.2f",
                    totalHoy
                )

                binding.tvGastosSemana.text = String.format(
                    Locale.US,
                    "Q %.2f",
                    totalSemana
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al actualizar Inicio", e)
            }
        }
    }

    private suspend fun actualizarResumen() {
        try {
            // Obtiene el resumen de gastos del usuario actual.
            val resumen = control.resumen()

            // Actualiza el texto únicamente si la consulta funciona.
            binding.tvResumen.text = resumen
            actualizarResumenInicio()

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

    private fun cargarEstadisticas() {

        lifecycleScope.launch {
            try {
                // Obtener los gastos reales del usuario actual.
                val gastos = control.listar()

                val formatoFecha = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                )

                val formatoDia = SimpleDateFormat(
                    "EEE",
                    Locale("es", "GT")
                )

                val calendario = Calendar.getInstance()

                // Preparar los últimos siete días.
                val fechas = mutableListOf<String>()
                val nombresDias = mutableListOf<String>()

                for (i in 6 downTo 0) {
                    val dia = Calendar.getInstance()
                    dia.add(Calendar.DAY_OF_YEAR, -i)

                    fechas.add(formatoFecha.format(dia.time))
                    nombresDias.add(formatoDia.format(dia.time))
                }

                // Calcular cuánto se gastó cada día.
                val totales = fechas.map { fecha ->
                    gastos
                        .filter { it.fecha == fecha }
                        .sumOf { it.monto.toDouble() }
                }

                val totalSemanal = totales.sum()

                binding.tvTotalSemanal.text =
                    String.format(
                        Locale.US,
                        "Q %.2f",
                        totalSemanal
                    )

                // Limpiar la gráfica anterior.
                binding.contenedorGrafica.removeAllViews()

                val maximo = totales.maxOrNull() ?: 0.0

                // Crear las siete barras.
                for (i in totales.indices) {

                    val columna = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1f
                        )
                    }

                    val altura = if (maximo > 0) {
                        ((totales[i] / maximo) * 150).toInt()
                            .coerceAtLeast(if (totales[i] > 0) 4 else 0)
                    } else {
                        0
                    }

                    val barra = android.view.View(this@MainActivity).apply {
                        setBackgroundColor(
                            androidx.core.content.ContextCompat.getColor(
                                this@MainActivity,
                                R.color.finance_primary
                            )
                        )

                        layoutParams = LinearLayout.LayoutParams(
                            22.dp(),
                            altura.dp()
                        )
                    }

                    val etiqueta = TextView(this@MainActivity).apply {
                        text = nombresDias[i]
                        textSize = 11f
                        gravity = Gravity.CENTER
                        setTextColor(
                            androidx.core.content.ContextCompat.getColor(
                                this@MainActivity,
                                R.color.finance_secondary_text
                            )
                        )
                    }

                    columna.addView(barra)
                    columna.addView(etiqueta)

                    binding.contenedorGrafica.addView(columna)
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    "MainActivity",
                    "Error al cargar estadísticas",
                    e
                )

                Toast.makeText(
                    this@MainActivity,
                    "No se pudieron cargar las estadísticas",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun cargarPerfil() {
        lifecycleScope.launch {
            try {
                // Buscar al usuario que inició sesión.
                val usuario = withContext(Dispatchers.IO) {
                    AppDatabase.obtenerInstancia(applicationContext)
                        .usuarioDao()
                        .buscarPorId(usuarioId)
                }

                binding.tvNombrePerfil.text =
                    usuario?.nombreUsuario ?: "Usuario"

                // Consultar únicamente sus gastos.
                val gastos = control.listar()

                val total = gastos.sumOf {
                    it.monto.toDouble()
                }

                val hoy = fechaDeHoy()

                val totalHoy = gastos
                    .filter { it.fecha == hoy }
                    .sumOf { it.monto.toDouble() }

                binding.tvResumenPerfil.text =
                    String.format(
                        Locale.US,
                        "%d gastos · Total: Q %.2f",
                        gastos.size,
                        total
                    )

                binding.tvGastosHoyPerfil.text =
                    String.format(
                        Locale.US,
                        "Gastado hoy: Q %.2f",
                        totalHoy
                    )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al cargar perfil", e)

                Toast.makeText(
                    this@MainActivity,
                    "No se pudo cargar el perfil",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

}

