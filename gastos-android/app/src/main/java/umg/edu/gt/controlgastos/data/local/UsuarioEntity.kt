
package umg.edu.gt.controlgastos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Representa una cuenta de usuario guardada en SQLite.
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["nombreUsuario"], unique = true)]
)
data class UsuarioEntity(

    // Identificador único de cada usuario.
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Nombre utilizado para iniciar sesión.
    val nombreUsuario: String,

    // Contraseña protegida mediante un hash con sal.
    val passwordHash: String,

    // Sal aleatoria utilizada para proteger la contraseña.
    val passwordSalt: String
)
