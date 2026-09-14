package com.pizzacafe.badin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pizzacafe.badin.data.Order
import com.pizzacafe.badin.data.OrderStatus
import com.pizzacafe.badin.data.OrderType
import com.pizzacafe.badin.databinding.ItemOrderBinding
import com.pizzacafe.badin.util.Currency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(
    private val onClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.VH>() {

    private var orders: List<Order> = emptyList()
    private val timeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    fun submit(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(orders[position])

    override fun getItemCount(): Int = orders.size

    inner class VH(private val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: Order) {
            val typeLabel = when (order.type) {
                OrderType.DINE_IN -> "Table ${order.tableNumber ?: "-"}"
                OrderType.DELIVERY -> "Delivery — ${order.customerName ?: "Customer"}"
                else -> "Takeaway — ${order.customerName ?: "Walk-in"}"
            }
            binding.tvOrderTitle.text = "#${order.id}  $typeLabel"
            binding.tvOrderSub.text = timeFormat.format(Date(order.createdAt))
            binding.tvOrderTotal.text = Currency.format(order.total)
            binding.tvOrderStatus.text = order.status
            binding.tvOrderStatus.setBackgroundColor(
                when (order.status) {
                    OrderStatus.OPEN -> 0xFFB0212E.toInt()
                    OrderStatus.BILLED -> 0xFFE8B14A.toInt()
                    OrderStatus.COMPLETED -> 0xFF2E7D4F.toInt()
                    else -> 0xFF6B6B6B.toInt()
                }
            )
            binding.root.setOnClickListener { onClick(order) }
        }
    }
}
