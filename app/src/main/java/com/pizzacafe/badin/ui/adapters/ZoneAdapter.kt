package com.pizzacafe.badin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pizzacafe.badin.data.DeliveryZone
import com.pizzacafe.badin.databinding.ItemZoneBinding
import com.pizzacafe.badin.util.Currency

class ZoneAdapter(
    private val onDelete: (DeliveryZone) -> Unit
) : RecyclerView.Adapter<ZoneAdapter.VH>() {

    private var zones: List<DeliveryZone> = emptyList()

    fun submit(newZones: List<DeliveryZone>) {
        zones = newZones
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemZoneBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(zones[position])

    override fun getItemCount(): Int = zones.size

    inner class VH(private val binding: ItemZoneBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(zone: DeliveryZone) {
            binding.tvZoneName.text = zone.name
            binding.tvZoneCharge.text = if (zone.charge <= 0) "Free" else Currency.format(zone.charge)
            binding.btnDeleteZone.setOnClickListener { onDelete(zone) }
        }
    }
}
