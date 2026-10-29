
package umg.edu.gt.controlgastos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GastoDao {

    // Guarda un gasto.
    @Insert
    suspend fun insertar(gasto: GastoEntity): Long

    // Obtiene los gastos del usuario actual.
    @Query("""
        SELECT * FROM gastos
        WHERE usuarioId = :usuarioId
        ORDER BY fecha DESC, id DESC
    """)
    suspend fun obtenerTodos(
        usuarioId: Int
    ): List<GastoEntity>

    // Busca un gasto que pertenezca al usuario.
    @Query("""
        SELECT * FROM gastos
        WHERE id = :id AND usuarioId = :usuarioId
        LIMIT 1
    """)
    suspend fun obtenerPorId(
        id: Int,
        usuarioId: Int
    ): GastoEntity?

    // Actualiza únicamente los gastos del usuario.
    @Query("""
        UPDATE gastos
        SET descripcion = :descripcion,
            monto = :monto,
            categoria = :categoria,
            fecha = :fecha
        WHERE id = :id AND usuarioId = :usuarioId
    """)
    suspend fun actualizar(
        id: Int,
        usuarioId: Int,
        descripcion: String,
        monto: Double,
        categoria: String,
        fecha: String
    ): Int

    // Elimina únicamente los gastos del usuario.
    @Query("""
        DELETE FROM gastos
        WHERE id = :id AND usuarioId = :usuarioId
    """)
    suspend fun eliminar(
        id: Int,
        usuarioId: Int
    ): Int
}
