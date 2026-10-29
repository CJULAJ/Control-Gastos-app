
package umg.edu.gt.controlgastos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gastos")
data class GastoEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val descripcion: String,

    val monto: Double,

    val categoria: String,

    val fecha: String,

    // Identifica al usuario propietario del gasto.
    // El valor 0 corresponde a gastos anteriores
    // que todavía no tienen un propietario asignado.
    val usuarioId: Int = 0
)

