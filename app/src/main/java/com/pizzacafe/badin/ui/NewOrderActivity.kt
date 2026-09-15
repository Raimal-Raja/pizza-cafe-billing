package com.pizzacafe.badin.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.pizzacafe.badin.data.*
import com.pizzacafe.badin.databinding.ActivityNewOrderBinding
import com.pizzacafe.badin.databinding.DialogCartBinding
import com.pizzacafe.badin.ui.adapters.CartAdapter
import com.pizzacafe.badin.ui.adapters.MenuAdapter
import com.pizzacafe.badin.util.Currency
import com.pizzacafe.badin.util.Prefs
import kotlinx.coroutines.launch

class NewOrderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORDER_ID = "order_id"
    }

    private lateinit var binding: ActivityNewOrderBinding
    private lateinit var repo: Repository
    private lateinit var prefs: Prefs

    private var orderType: String = OrderType.DINE_IN
    private var existingOrderId: Long? = null
    private var allMenuItems: List<MenuItem> = emptyList()
    private var allFlavors: List<FlavorOption> = emptyList()
    private var allZones: List<DeliveryZone> = emptyList()
    private var selectedZone: DeliveryZone? = null

    private val cart = mutableListOf<CartLine>()

    private lateinit var menuAdapter: MenuAdapter
    private var cartAdapter: CartAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repo = Repository(this)
        prefs = Prefs(this)

        existingOrderId = intent.getLongExtra(EXTRA_ORDER_ID, -1L).takeIf { it != -1L }

        setupMenuList()
        setupTabs()
        setupCategoryFilter()
        setupZoneObserver()
        setupFlavorObserver()

        binding.btnViewCart.setOnClickListener { showCartSheet() }
        binding.btnSave.setOnClickListener { persistOrder(OrderStatus.OPEN, thenOpenBill = false) }
        binding.btnBill.setOnClickListener { persistOrder(OrderStatus.BILLED, thenOpenBill = true) }

        existingOrderId?.let { loadExistingOrder(it) }

        updateCartFooter()
    }

    private fun setupMenuList() {
        menuAdapter = MenuAdapter { item -> onMenuItemTapped(item) }
        binding.rvMenu.adapter = menuAdapter
        binding.rvMenu.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        repo.menuDao.getAllActive().observe(this) { items ->
            allMenuItems = items
            applyCategoryFilter()
        }
    }

    private fun setupCategoryFilter() {
        repo.menuDao.getCategories().observe(this) { cats ->
            val options = mutableListOf("All Categories")
            options.addAll(cats)
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
            binding.spCategory.setAdapter(adapter)
            if (binding.spCategory.text.isNullOrBlank()) {
                binding.spCategory.setText(options[0], false)
            }
        }
        binding.spCategory.setOnItemClickListener { _, _, _, _ -> applyCategoryFilter() }
    }

    private fun applyCategoryFilter() {
        val selected = binding.spCategory.text?.toString()
        val filtered = if (selected.isNullOrBlank() || selected == "All Categories") {
            allMenuItems
        } else {
            allMenuItems.filter { it.category == selected }
        }
        menuAdapter.submit(filtered)
    }

    private fun setupZoneObserver() {
        repo.zoneDao.getAll().observe(this) { zones ->
            allZones = zones
            val names = zones.map { "${it.name} (${if (it.charge <= 0) "Free" else Currency.format(it.charge)})" }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
            binding.spZone.setAdapter(adapter)
            binding.spZone.setOnItemClickListener { _, _, position, _ ->
                selectedZone = zones[position]
                updateCartFooter()
            }
            if (selectedZone == null && zones.isNotEmpty()) {
                selectedZone = zones[0]
                binding.spZone.setText(names[0], false)
            }
        }
    }

    private fun setupFlavorObserver() {
        repo.menuDao.getFlavors().observe(this) { flavors -> allFlavors = flavors }
    }

    private fun setupTabs() {
        binding.tabOrderType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                orderType = when (tab.position) {
                    0 -> OrderType.DINE_IN
                    1 -> OrderType.DELIVERY
                    else -> OrderType.TAKEAWAY
                }
                updateFieldVisibility()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        updateFieldVisibility()
    }

    private fun updateFieldVisibility() {
        binding.layoutTable.visibility = if (orderType == OrderType.DINE_IN) View.VISIBLE else View.GONE
        val showCustomer = orderType != OrderType.DINE_IN
        binding.layoutName.visibility = if (showCustomer) View.VISIBLE else View.GONE
        binding.layoutPhone.visibility = if (showCustomer) View.VISIBLE else View.GONE
        binding.layoutAddress.visibility = if (orderType == OrderType.DELIVERY) View.VISIBLE else View.GONE
        binding.layoutZone.visibility = if (orderType == OrderType.DELIVERY) View.VISIBLE else View.GONE
        supportActionBar?.title = when (orderType) {
            OrderType.DINE_IN -> "New Order — Dine-in"
            OrderType.DELIVERY -> "New Order — Delivery"
            else -> "New Order — Takeaway"
        }
        updateCartFooter()
    }

    private fun onMenuItemTapped(item: MenuItem) {
        if (item.hasFlavorOption && allFlavors.isNotEmpty()) {
            val names = allFlavors.map { it.name }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Choose a flavor for ${item.name}")
                .setItems(names) { _, which -> addToCart(item, names[which]) }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            addToCart(item, null)
        }
    }

    private fun addToCart(item: MenuItem, flavor: String?) {
        val existing = cart.find { it.menuItemId == item.id && it.flavorNote == flavor }
        if (existing != null) {
            existing.qty += 1
        } else {
            cart.add(CartLine(item.id, item.name, item.price, 1, flavor))
        }
        cartAdapter?.submit(cart.toList())
        updateCartFooter()
        Toast.makeText(this, "${item.name} added", Toast.LENGTH_SHORT).show()
    }

    private fun showCartSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = DialogCartBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(sheetBinding.root)

        cartAdapter = CartAdapter { line, newQty ->
            if (newQty <= 0) {
                cart.remove(line)
            } else {
                line.qty = newQty
            }
            cartAdapter?.submit(cart.toList())
            updateCartFooter()
        }
        sheetBinding.rvCart.adapter = cartAdapter
        sheetBinding.rvCart.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        cartAdapter?.submit(cart.toList())

        dialog.show()
    }

    private fun currentSubtotal(): Double = cart.sumOf { it.lineTotal }

    private fun currentDeliveryCharge(): Double {
        if (orderType != OrderType.DELIVERY) return 0.0
        val subtotal = currentSubtotal()
        if (subtotal >= prefs.freeDeliveryThreshold) return 0.0
        return selectedZone?.charge ?: 0.0
    }

    private fun updateCartFooter() {
        val count = cart.sumOf { it.qty }
        val subtotal = currentSubtotal()
        val delivery = currentDeliveryCharge()
        binding.tvCartSummary.text = "Cart: $count item${if (count == 1) "" else "s"}"
        binding.tvCartTotal.text = Currency.format(subtotal + delivery)
    }

    private fun loadExistingOrder(orderId: Long) {
        lifecycleScope.launch {
            val order = repo.orderDao.getOrderById(orderId) ?: return@launch
            val items = repo.orderDao.getItemsForOrderSync(orderId)

            orderType = order.type
            val tabIndex = when (order.type) {
                OrderType.DINE_IN -> 0
                OrderType.DELIVERY -> 1
                else -> 2
            }
            binding.tabOrderType.getTabAt(tabIndex)?.select()
            updateFieldVisibility()

            binding.etTable.setText(order.tableNumber ?: "")
            binding.etName.setText(order.customerName ?: "")
            binding.etPhone.setText(order.phone ?: "")
            binding.etAddress.setText(order.address ?: "")

            cart.clear()
            items.forEach { oi ->
                cart.add(CartLine(oi.menuItemId, oi.itemName, oi.unitPrice, oi.qty, oi.flavorNote))
            }
            cartAdapter?.submit(cart.toList())
            updateCartFooter()
        }
    }

    private fun persistOrder(status: String, thenOpenBill: Boolean) {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Add at least one item first", Toast.LENGTH_SHORT).show()
            return
        }
        if (orderType == OrderType.DINE_IN && binding.etTable.text.isNullOrBlank()) {
            Toast.makeText(this, "Enter a table number", Toast.LENGTH_SHORT).show()
            return
        }
        if (orderType == OrderType.DELIVERY) {
            when {
                binding.etName.text.isNullOrBlank() -> {
                    Toast.makeText(this, "Enter the customer's name", Toast.LENGTH_SHORT).show()
                    return
                }
                binding.etPhone.text.isNullOrBlank() -> {
                    Toast.makeText(this, "Enter a contact number", Toast.LENGTH_SHORT).show()
                    return
                }
                binding.etAddress.text.isNullOrBlank() -> {
                    Toast.makeText(this, "Enter the delivery address", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        val subtotal = currentSubtotal()
        val delivery = currentDeliveryCharge()

        lifecycleScope.launch {
            val orderId: Long
            if (existingOrderId != null) {
                val existing = repo.orderDao.getOrderById(existingOrderId!!)!!
                val updated = existing.copy(
                    type = orderType,
                    tableNumber = binding.etTable.text?.toString(),
                    customerName = binding.etName.text?.toString(),
                    phone = binding.etPhone.text?.toString(),
                    address = binding.etAddress.text?.toString(),
                    deliveryZoneId = selectedZone?.id,
                    deliveryCharge = delivery,
                    subtotal = subtotal,
                    total = subtotal + delivery,
                    status = status,
                    updatedAt = System.currentTimeMillis()
                )
                repo.orderDao.updateOrder(updated)
                repo.orderDao.clearItemsForOrder(existingOrderId!!)
                orderId = existingOrderId!!
            } else {
                val newOrder = Order(
                    type = orderType,
                    tableNumber = binding.etTable.text?.toString(),
                    customerName = binding.etName.text?.toString(),
                    phone = binding.etPhone.text?.toString(),
                    address = binding.etAddress.text?.toString(),
                    deliveryZoneId = selectedZone?.id,
                    deliveryCharge = delivery,
                    subtotal = subtotal,
                    total = subtotal + delivery,
                    status = status
                )
                orderId = repo.orderDao.insertOrder(newOrder)
            }

            cart.forEach { line ->
                repo.orderDao.insertItem(
                    OrderItem(
                        orderId = orderId,
                        menuItemId = line.menuItemId,
                        itemName = line.name,
                        unitPrice = line.unitPrice,
                        qty = line.qty,
                        flavorNote = line.flavorNote,
                        lineTotal = line.lineTotal
                    )
                )
            }

            if (thenOpenBill) {
                startActivity(Intent(this@NewOrderActivity, BillActivity::class.java).apply {
                    putExtra(BillActivity.EXTRA_ORDER_ID, orderId)
                })
                finish()
            } else {
                Toast.makeText(this@NewOrderActivity, "Order saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
