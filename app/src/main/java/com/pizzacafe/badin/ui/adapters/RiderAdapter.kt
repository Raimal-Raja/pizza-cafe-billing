package com.pizzacafe.badin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pizzacafe.badin.data.Rider
import com.pizzacafe.badin.databinding.ItemStaffBinding

class RiderAdapter(
    private val onActiveChanged: (Rider, Boolean) -> Unit
) : RecyclerView.Adapter<RiderAdapter.VH>() {

    private var riders: List<Rider> = emptyList()

    fun submit(newRiders: List<Rider>) {
        riders = newRiders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStaffBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(riders[position])
    override fun getItemCount(): Int = riders.size

    inner class VH(private val binding: ItemStaffBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(rider: Rider) {
            binding.tvStaffName.text = rider.name
            binding.tvStaffPhone.text = rider.phone.orEmpty()
            binding.swStaffActive.setOnCheckedChangeListener(null)
            binding.swStaffActive.isChecked = rider.active
            binding.swStaffActive.setOnCheckedChangeListener { _, checked -> onActiveChanged(rider, checked) }
        }
    }
}
