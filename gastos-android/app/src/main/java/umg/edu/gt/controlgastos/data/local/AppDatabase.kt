
package umg.edu.gt.controlgastos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        GastoEntity::class,
        UsuarioEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Acceso a la tabla de gastos.
    abstract fun gastoDao(): GastoDao

    // Acceso a la tabla de usuarios.
    abstract fun usuarioDao(): UsuarioDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Actualiza la base de datos sin eliminar los gastos.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `usuarios` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nombreUsuario` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `passwordSalt` TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                // Impide registrar nombres de usuario repetidos.
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_usuarios_nombreUsuario`
                    ON `usuarios` (`nombreUsuario`)
                    """.trimIndent()
                )
            }
        }

        fun obtenerInstancia(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "control_gastos.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instancia
                instancia
            }
        }
    }
}
