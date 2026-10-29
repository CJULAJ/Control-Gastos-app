
package edu.umg.programacion2.gastos.servicio

import edu.umg.programacion2.gastos.datos.GastoRepositoryEnMemoria
import edu.umg.programacion2.gastos.modelo.Categoria
import edu.umg.programacion2.gastos.modelo.Gasto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ControlGastosTest {

    @Test
    fun `rechazar gasto con monto cero`() {

        // Creamos el servicio con un repositorio de prueba.
        val control = ControlGastos(GastoRepositoryEnMemoria())

        // Creamos un gasto con monto inválido.
        val gasto = Gasto(
            descripcion = "Almuerzo",
            monto = 0.0,
            categoria = Categoria.ALIMENTACION,
            fecha = "2026-10-28"
        )

        // Ejecutamos la validación.
        val resultado = control.validar(gasto)

        // Comprobamos que devuelva el mensaje esperado.
        assertEquals("El monto debe ser mayor que 0", resultado)
    }
}
