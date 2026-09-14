package com.pizzacafe.badin.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.pizzacafe.badin.data.OrderStatus
import com.pizzacafe.badin.data.Repository
import com.pizzacafe.badin.databinding.ActivityOrdersBinding
import com.pizzacafe.badin.ui.adapters.OrderAdapter

class OrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrdersBinding
    private lateinit var repo: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repo = Repository(this)

        val adapter = OrderAdapter { order ->
            if (order.status == OrderStatus.OPEN) {
                startActivity(Intent(this, NewOrderActivity::class.java).apply {
                    putExtra(NewOrderActivity.EXTRA_ORDER_ID, order.id)
                })
            } else {
                startActivity(Intent(this, BillActivity::class.java).apply {
                    putExtra(BillActivity.EXTRA_ORDER_ID, order.id)
                })
            }
        }
        binding.rvOrders.adapter = adapter
        binding.rvOrders.layoutManager = LinearLayoutManager(this)

        repo.orderDao.getAllOrders().observe(this) { orders -> adapter.submit(orders) }
    }
}
