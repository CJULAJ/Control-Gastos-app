package umg.edu.gt.controlgastos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.umg.programacion2.gastos.modelo.Gasto
import java.util.Locale

// Muestra los gastos registrados en una lista.
class GastoAdapter : RecyclerView.Adapter<GastoAdapter.GastoViewHolder>()
{

    // Aquí se guardan los gastos que mostraremos.
    private var gastos: List<Gasto> = emptyList()

    // Guarda las referencias a los textos de cada gasto.
    class GastoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val descripcion: TextView =
            itemView.findViewById(R.id.tvDescripcionGasto)

        val monto: TextView =
            itemView.findViewById(R.id.tvMontoGasto)

        val detalle: TextView =
            itemView.findViewById(R.id.tvDetalleGasto)
    }

    // Crea el diseño de cada gasto.
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GastoViewHolder {

        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gasto, parent, false)

        return GastoViewHolder(vista)
    }

    // Coloca la información del gasto en pantalla.
    override fun onBindViewHolder(
        holder: GastoViewHolder,
        position: Int
    ) {
        val gasto = gastos[position]

        holder.descripcion.text = gasto.descripcion

        holder.monto.text = String.format(
            Locale.US,
            "Q %.2f",
            gasto.monto
        )

        holder.detalle.text =
            "${gasto.categoria} - ${gasto.fecha}"
    }

    // Indica cuántos gastos hay en la lista.
    override fun getItemCount(): Int = gastos.size

    // Actualiza la lista cuando cambian los gastos.
    @Suppress("NotifyDataSetChanged")
    fun actualizarGastos(nuevaLista: List<Gasto>) {
        gastos = nuevaLista
        notifyDataSetChanged()
    }
}
