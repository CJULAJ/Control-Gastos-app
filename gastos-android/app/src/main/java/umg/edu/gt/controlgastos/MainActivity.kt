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
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Spinner
import android.content.Intent
class MainActivity : AppCompatActivity() {

    // Permite utilizar los elementos del archivo XML.
    private lateinit var binding: ActivityMainBinding

    // Conecta la lógica de gastos con la base de datos Room.
    private val control by lazy {
        val database = AppDatabase.obtenerInstancia(applicationContext)
        ControlGastos(GastoRepositoryRoom(database.gastoDao()))
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
            val dao = AppDatabase
                .obtenerInstancia(applicationContext)
                .gastoDao()

            val gastos = dao.obtenerTodos().map { entidad ->
                Gasto(
                    id = entidad.id,
                    descripcion = entidad.descripcion,
                    monto = entidad.monto,
                    categoria = Categoria.valueOf(entidad.categoria),
                    fecha = entidad.fecha
                )
            }

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
                        val gastoGuardado = dao.obtenerPorId(gasto.id)

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
                        val gastoActualizado = gastoGuardado.copy(
                            descripcion = nuevaDescripcion,
                            monto = nuevoMonto,
                            categoria = nuevaCategoria.name
                        )

                        dao.actualizar(gastoActualizado)

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

