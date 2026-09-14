package com.pizzacafe.badin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pizzacafe.badin.databinding.ItemCartBinding
import com.pizzacafe.badin.ui.CartLine
import com.pizzacafe.badin.util.Currency

class CartAdapter(
    private val onQtyChanged: (CartLine, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    private var lines: List<CartLine> = emptyList()

    fun submit(newLines: List<CartLine>) {
        lines = newLines
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(lines[position])

    override fun getItemCount(): Int = lines.size

    inner class VH(private val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(line: CartLine) {
            binding.tvCartName.text = line.name
            binding.tvCartFlavor.text = line.flavorNote ?: ""
            binding.tvCartFlavor.visibility = if (line.flavorNote.isNullOrBlank())
                android.view.View.GONE else android.view.View.VISIBLE
            binding.tvCartQty.text = line.qty.toString()
            binding.tvCartLineTotal.text = Currency.format(line.lineTotal)
            binding.btnMinus.setOnClickListener { onQtyChanged(line, line.qty - 1) }
            binding.btnPlus.setOnClickListener { onQtyChanged(line, line.qty + 1) }
        }
    }
}
