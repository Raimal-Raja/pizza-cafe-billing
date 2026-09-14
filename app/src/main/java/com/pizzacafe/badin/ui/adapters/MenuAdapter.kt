package com.pizzacafe.badin.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pizzacafe.badin.data.MenuItem
import com.pizzacafe.badin.databinding.ItemCategoryHeaderBinding
import com.pizzacafe.badin.databinding.ItemMenuBinding
import com.pizzacafe.badin.util.Currency

private const val TYPE_HEADER = 0
private const val TYPE_ITEM = 1

sealed class MenuRow {
    data class Header(val category: String) : MenuRow()
    data class Item(val menuItem: MenuItem) : MenuRow()
}

class MenuAdapter(
    private val onAddClicked: (MenuItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rows: List<MenuRow> = emptyList()

    fun submit(items: List<MenuItem>) {
        val grouped = items.groupBy { it.category }
        val newRows = mutableListOf<MenuRow>()
        grouped.keys.sorted().forEach { cat ->
            newRows.add(MenuRow.Header(cat))
            grouped[cat]?.sortedBy { it.name }?.forEach { newRows.add(MenuRow.Item(it)) }
        }
        rows = newRows
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (rows[position] is MenuRow.Header) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(ItemCategoryHeaderBinding.inflate(inflater, parent, false))
        } else {
            ItemVH(ItemMenuBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is MenuRow.Header -> (holder as HeaderVH).bind(row.category)
            is MenuRow.Item -> (holder as ItemVH).bind(row.menuItem)
        }
    }

    override fun getItemCount(): Int = rows.size

    inner class HeaderVH(private val binding: ItemCategoryHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(category: String) {
            binding.root.text = category
        }
    }

    inner class ItemVH(private val binding: ItemMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MenuItem) {
            binding.tvItemName.text = item.name
            binding.tvItemPrice.text = Currency.format(item.price)
            binding.btnAdd.setOnClickListener { onAddClicked(item) }
        }
    }
}
