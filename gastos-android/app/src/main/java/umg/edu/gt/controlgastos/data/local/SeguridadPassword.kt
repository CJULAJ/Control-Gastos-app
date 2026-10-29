
package umg.edu.gt.controlgastos.data.local

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// Protege y verifica las contraseñas de los usuarios.
object SeguridadPassword {

    private const val ITERACIONES = 210_000
    private const val LONGITUD_HASH = 256

    // Genera una sal aleatoria para cada contraseña.
    fun generarSal(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // Convierte una contraseña en un hash protegido.
    fun generarHash(password: String, sal: String): String {
        val bytesSal = Base64.decode(sal, Base64.NO_WRAP)
        val especificacion = PBEKeySpec(
            password.toCharArray(),
            bytesSal,
            ITERACIONES,
            LONGITUD_HASH
        )

        return try {
            val fabrica = SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA256"
            )
            val hash = fabrica.generateSecret(especificacion).encoded
            Base64.encodeToString(hash, Base64.NO_WRAP)
        } finally {
            especificacion.clearPassword()
        }
    }

    // Comprueba si la contraseña ingresada es correcta.
    fun verificar(
        password: String,
        sal: String,
        hashGuardado: String
    ): Boolean {
        val hashCalculado = Base64.decode(
            generarHash(password, sal),
            Base64.NO_WRAP
        )

        val hashOriginal = Base64.decode(
            hashGuardado,
            Base64.NO_WRAP
        )

        return MessageDigest.isEqual(
            hashCalculado,
            hashOriginal
        )
    }
}
