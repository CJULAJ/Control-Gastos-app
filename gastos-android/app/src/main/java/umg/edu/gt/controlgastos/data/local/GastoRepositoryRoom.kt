
package umg.edu.gt.controlgastos.data.local

import edu.umg.programacion2.gastos.datos.GastoRepository
import edu.umg.programacion2.gastos.modelo.Categoria
import edu.umg.programacion2.gastos.modelo.Gasto

// Conecta gastos-core con Room y separa los gastos por usuario.
class GastoRepositoryRoom(
    private val gastoDao: GastoDao,
    private val usuarioId: Int
) : GastoRepository {

    init {
        require(usuarioId > 0) {
            "Se necesita un usuario válido"
        }
    }

    // Guarda un gasto asociado al usuario actual.
    override suspend fun agregar(gasto: Gasto): Gasto {

        val entidad = GastoEntity(
            id = gasto.id,
            descripcion = gasto.descripcion,
            monto = gasto.monto,
            categoria = gasto.categoria.name,
            fecha = gasto.fecha,
            usuarioId = usuarioId
        )

        val idGenerado = gastoDao.insertar(entidad)

        return gasto.copy(id = idGenerado.toInt())
    }

    // Lista únicamente los gastos del usuario actual.
    override suspend fun listar(): List<Gasto> {

        return gastoDao.obtenerTodos(usuarioId).map { entidad ->
            Gasto(
                id = entidad.id,
                descripcion = entidad.descripcion,
                monto = entidad.monto,
                categoria = Categoria.valueOf(entidad.categoria),
                fecha = entidad.fecha
            )
        }
    }

    // Busca un gasto que pertenezca al usuario actual.
    override suspend fun buscarPorId(id: Int): Gasto? {

        val entidad = gastoDao.obtenerPorId(id, usuarioId)
            ?: return null

        return Gasto(
            id = entidad.id,
            descripcion = entidad.descripcion,
            monto = entidad.monto,
            categoria = Categoria.valueOf(entidad.categoria),
            fecha = entidad.fecha
        )
    }

    // Actualiza únicamente los gastos del usuario actual.
    override suspend fun actualizar(gasto: Gasto): Boolean {

        val filasActualizadas = gastoDao.actualizar(
            id = gasto.id,
            usuarioId = usuarioId,
            descripcion = gasto.descripcion,
            monto = gasto.monto,
            categoria = gasto.categoria.name,
            fecha = gasto.fecha
        )

        return filasActualizadas > 0
    }

    // Elimina únicamente los gastos del usuario actual.
    override suspend fun eliminar(id: Int): Boolean {

        val filasEliminadas = gastoDao.eliminar(
            id = id,
            usuarioId = usuarioId
        )

        return filasEliminadas > 0
    }
}

