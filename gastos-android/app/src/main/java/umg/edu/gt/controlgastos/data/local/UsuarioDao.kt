
package umg.edu.gt.controlgastos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

// Permite guardar y consultar usuarios en SQLite.
@Dao
interface UsuarioDao {

    // Registra un usuario nuevo y devuelve su ID.
    @Insert
    suspend fun insertar(usuario: UsuarioEntity): Long

    // Busca una cuenta por su nombre de usuario.
    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :nombre LIMIT 1")
    suspend fun buscarPorNombre(nombre: String): UsuarioEntity?

    // Busca un usuario mediante su identificador.
    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): UsuarioEntity?
}
