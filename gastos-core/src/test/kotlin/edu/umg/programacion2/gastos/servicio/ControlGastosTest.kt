
package edu.umg.programacion2.gastos.servicio

import edu.umg.programacion2.gastos.datos.GastoRepositoryEnMemoria
import edu.umg.programacion2.gastos.modelo.Categoria
import edu.umg.programacion2.gastos.modelo.Gasto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotNull

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

    @Test
    fun `aceptar gasto con datos validos`() {

        // Creamos un gasto con todos sus datos correctos.
        val gasto = Gasto(
            descripcion = "Compra de cuadernos",
            monto = 35.0,
            categoria = Categoria.ESTUDIOS,
            fecha = "2026-10-28"
        )

        // Validamos el gasto.
        val resultado = control.validar(gasto)

        // Si es válido, el resultado debe ser null.
        assertEquals(null, resultado)
    }

    @Test
    fun `registrar gasto valido correctamente`() = runTest {

        // Creamos un repositorio vacío.
        val repositorio = GastoRepositoryEnMemoria()
        val servicio = ControlGastos(repositorio)

        // Creamos un gasto válido.
        val gasto = Gasto(
            descripcion = "Compra de comida",
            monto = 45.0,
            categoria = Categoria.ALIMENTACION,
            fecha = "2026-10-28"
        )

        // Registramos el gasto.
        val resultado = servicio.registrar(gasto)

        // Consultamos los gastos guardados.
        val gastosGuardados = servicio.listar()

        // Verificamos que no hubo errores al registrar.
        assertEquals(null, resultado)

        // Verificamos que existe un gasto guardado.
        assertEquals(1, gastosGuardados.size)

        // Verificamos que sus datos son correctos.
        assertEquals("Compra de comida", gastosGuardados[0].descripcion)
        assertEquals(45.0, gastosGuardados[0].monto)
        assertNotNull(gastosGuardados[0].id)
    }

    @Test
    fun `calcular total de gastos correctamente`() = runTest {

        // Creamos un repositorio vacío.
        val repositorio = GastoRepositoryEnMemoria()
        val servicio = ControlGastos(repositorio)

        // Registramos el primer gasto.
        servicio.registrar(
            Gasto(
                descripcion = "Almuerzo",
                monto = 45.0,
                categoria = Categoria.ALIMENTACION,
                fecha = "2026-10-28"
            )
        )

        // Registramos el segundo gasto.
        servicio.registrar(
            Gasto(
                descripcion = "Transporte",
                monto = 15.0,
                categoria = Categoria.TRANSPORTE,
                fecha = "2026-10-28"
            )
        )

        // Calculamos el total de ambos gastos.
        val total = servicio.total()

        // Comprobamos que 45 + 15 = 60.
        assertEquals(60.0, total, 0.001)
    }

    @Test
    fun `calcular totales por categoria correctamente`() = runTest {

        // Creamos un repositorio vacío.
        val repositorio = GastoRepositoryEnMemoria()
        val servicio = ControlGastos(repositorio)

        // Registramos dos gastos de alimentación.
        servicio.registrar(
            Gasto(
                descripcion = "Almuerzo",
                monto = 45.0,
                categoria = Categoria.ALIMENTACION,
                fecha = "2026-10-28"
            )
        )

        servicio.registrar(
            Gasto(
                descripcion = "Desayuno",
                monto = 30.0,
                categoria = Categoria.ALIMENTACION,
                fecha = "2026-10-28"
            )
        )

        // Registramos un gasto de transporte.
        servicio.registrar(
            Gasto(
                descripcion = "Bus",
                monto = 15.0,
                categoria = Categoria.TRANSPORTE,
                fecha = "2026-10-28"
            )
        )

        // Obtenemos los totales por categoría.
        val totales = servicio.totalPorCategoria()

        // Comprobamos los resultados.
        assertEquals(75.0, totales[Categoria.ALIMENTACION])
        assertEquals(15.0, totales[Categoria.TRANSPORTE])

        // Comprobamos que solamente hay dos categorías.
        assertEquals(2, totales.size)
    }


}
