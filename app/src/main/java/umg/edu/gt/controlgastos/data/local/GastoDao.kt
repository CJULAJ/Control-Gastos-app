
package umg.edu.gt.controlgastos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface GastoDao {


    @Insert
    suspend fun insertar(gasto: GastoEntity): Long

    @Query("SELECT * FROM gastos ORDER BY fecha DESC, id DESC")
    suspend fun obtenerTodos(): List<GastoEntity>

    @Query("SELECT * FROM gastos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Int): GastoEntity?

    @Update
    suspend fun actualizar(gasto: GastoEntity)

    @Delete
    suspend fun eliminar(gasto: GastoEntity)
}
