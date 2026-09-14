package com.pizzacafe.badin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pizzacafe.badin.data.MenuItem
import com.pizzacafe.badin.databinding.ItemMenuManageBinding
import com.pizzacafe.badin.util.Currency

class MenuManageAdapter(
    private val onEdit: (MenuItem) -> Unit,
    private val onDelete: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuManageAdapter.VH>() {

    private var items: List<MenuItem> = emptyList()

    fun submit(newItems: List<MenuItem>) {
        items = newItems.sortedWith(compareBy({ it.category }, { it.name }))
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMenuManageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class VH(private val binding: ItemMenuManageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MenuItem) {
            binding.tvCategory.text = item.category
            binding.tvName.text = item.name
            binding.tvPrice.text = Currency.format(item.price)
            binding.btnEdit.setOnClickListener { onEdit(item) }
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}
