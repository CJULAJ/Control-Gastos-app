
package umg.edu.gt.controlgastos.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Gestiona el registro y el inicio de sesión de usuarios.
class AutenticacionRepository(
    private val usuarioDao: UsuarioDao
) {

    // Registra una cuenta nueva.
    suspend fun registrar(
        nombre: String,
        password: String
    ): String? = withContext(Dispatchers.IO) {

        val usuario = nombre.trim()

        // Validamos los datos ingresados.
        if (usuario.length < 3) {
            return@withContext "El usuario debe tener al menos 3 caracteres"
        }

        if (password.length < 8) {
            return@withContext "La contraseña debe tener al menos 8 caracteres"
        }

        // Comprobamos que el usuario no exista.
        if (usuarioDao.buscarPorNombre(usuario) != null) {
            return@withContext "Este nombre de usuario ya está registrado"
        }

        // Protegemos la contraseña.
        val sal = SeguridadPassword.generarSal()
        val hash = SeguridadPassword.generarHash(password, sal)

        // Guardamos la cuenta en SQLite.
        val nuevoUsuario = UsuarioEntity(
            nombreUsuario = usuario,
            passwordHash = hash,
            passwordSalt = sal
        )

        try {
            usuarioDao.insertar(nuevoUsuario)
            null
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            "Este nombre de usuario ya está registrado"
        }
    }

    // Verifica las credenciales y devuelve el usuario autenticado.
    suspend fun iniciarSesion(
        nombre: String,
        password: String
    ): UsuarioEntity? = withContext(Dispatchers.IO) {

        if (nombre.isBlank() || password.isBlank()) {
            return@withContext null
        }

        // Buscamos la cuenta en SQLite.
        val usuario = usuarioDao.buscarPorNombre(nombre.trim())
            ?: return@withContext null

        // Comprobamos la contraseña.
        val correcta = SeguridadPassword.verificar(
            password,
            usuario.passwordSalt,
            usuario.passwordHash
        )

        if (correcta) usuario else null
    }
}
