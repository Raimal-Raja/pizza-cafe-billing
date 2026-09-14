package com.pizzacafe.badin.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pizzacafe.badin.data.Repository
import com.pizzacafe.badin.databinding.ActivityMainBinding
import com.pizzacafe.badin.util.Prefs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = Repository(this)
        binding.tvRestaurantName.text = Prefs(this).restaurantName.ifBlank { "PIZZA CAFE" }.uppercase()

        repo.orderDao.getActiveOrders().observe(this) { list ->
            binding.tvActiveCount.text = "${list.size} open"
        }

        binding.cardNewOrder.setOnClickListener {
            startActivity(Intent(this, NewOrderActivity::class.java))
        }
        binding.cardOrders.setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }
        binding.cardMenu.setOnClickListener {
            startActivity(Intent(this, MenuManageActivity::class.java))
        }
        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
