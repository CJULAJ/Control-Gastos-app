
package edu.umg.programacion2.gastos.servicio

import edu.umg.programacion2.gastos.datos.GastoRepositoryEnMemoria
import edu.umg.programacion2.gastos.modelo.Categoria
import edu.umg.programacion2.gastos.modelo.Gasto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ControlGastosTest {

    private val control = ControlGastos(GastoRepositoryEnMemoria())

    @Test
    fun `rechazar gasto con monto cero`() {
        val gasto = Gasto(
            descripcion = "Almuerzo",
            monto = 0.0,
            categoria = Categoria.ALIMENTACION,
            fecha = "2026-10-28"
        )

        assertEquals("El monto debe ser mayor que 0", control.validar(gasto))
    }

    @Test
    fun `rechazar descripcion vacia`() {
        val gasto = Gasto(
            descripcion = "",
            monto = 25.0,
            categoria = Categoria.ALIMENTACION,
            fecha = "2026-10-28"
        )

        assertEquals("La descripción no puede estar vacía", control.validar(gasto))
    }

    @Test
    fun `rechazar monto negativo`() {
        val gasto = Gasto(
            descripcion = "Transporte",
            monto = -15.0,
            categoria = Categoria.TRANSPORTE,
            fecha = "2026-10-28"
        )

        assertEquals("El monto debe ser mayor que 0", control.validar(gasto))
    }

    @Test
    fun `rechazar formato de fecha incorrecto`() {
        val gasto = Gasto(
            descripcion = "Cuaderno",
            monto = 20.0,
            categoria = Categoria.ESTUDIOS,
            fecha = "28/10/2026"
        )

        assertEquals(
            "La fecha debe tener el formato yyyy-MM-dd",
            control.validar(gasto)
        )
    }
}
