package com.pizzacafe.badin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pizzacafe.badin.data.Waiter
import com.pizzacafe.badin.databinding.ItemStaffBinding

class WaiterAdapter(
    private val onActiveChanged: (Waiter, Boolean) -> Unit
) : RecyclerView.Adapter<WaiterAdapter.VH>() {

    private var waiters: List<Waiter> = emptyList()

    fun submit(newWaiters: List<Waiter>) {
        waiters = newWaiters
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStaffBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(waiters[position])
    override fun getItemCount(): Int = waiters.size

    inner class VH(private val binding: ItemStaffBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(waiter: Waiter) {
            binding.tvStaffName.text = waiter.name
            binding.tvStaffPhone.text = waiter.phone.orEmpty()
            binding.swStaffActive.setOnCheckedChangeListener(null)
            binding.swStaffActive.isChecked = waiter.active
            binding.swStaffActive.setOnCheckedChangeListener { _, checked -> onActiveChanged(waiter, checked) }
        }
    }
}
