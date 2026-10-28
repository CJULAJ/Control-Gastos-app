
package umg.edu.gt.controlgastos.data.local

import edu.umg.programacion2.gastos.datos.GastoRepository
import edu.umg.programacion2.gastos.modelo.Categoria
import edu.umg.programacion2.gastos.modelo.Gasto

class GastoRepositoryRoom(
    private val dao: GastoDao
) : GastoRepository {

    override suspend fun agregar(gasto: Gasto): Gasto {
        val entidad = GastoEntity(
            id = gasto.id,
            descripcion = gasto.descripcion,
            monto = gasto.monto,
            categoria = gasto.categoria.name,
            fecha = gasto.fecha
        )

        val nuevoId = dao.insertar(entidad)
        return gasto.copy(id = nuevoId.toInt())
    }

    override suspend fun listar(): List<Gasto> {
        return dao.obtenerTodos().map { entidad ->
            convertirAGasto(entidad)
        }
    }

    override suspend fun buscarPorId(id: Int): Gasto? {
        val entidad = dao.obtenerPorId(id) ?: return null
        return convertirAGasto(entidad)
    }

    override suspend fun actualizar(gasto: Gasto): Boolean {
        val existente = dao.obtenerPorId(gasto.id) ?: return false

        dao.actualizar(
            existente.copy(
                descripcion = gasto.descripcion,
                monto = gasto.monto,
                categoria = gasto.categoria.name,
                fecha = gasto.fecha
            )
        )

        return true
    }

    override suspend fun eliminar(id: Int): Boolean {
        val entidad = dao.obtenerPorId(id) ?: return false
        dao.eliminar(entidad)
        return true
    }

    private fun convertirAGasto(entidad: GastoEntity): Gasto {
        return Gasto(
            id = entidad.id,
            descripcion = entidad.descripcion,
            monto = entidad.monto,
            categoria = Categoria.valueOf(entidad.categoria),
            fecha = entidad.fecha
        )
    }
}
