package com.pizzacafe.badin.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.pizzacafe.badin.util.DateUtil
import com.pizzacafe.badin.util.Prefs
import kotlinx.coroutines.launch

class NewOrderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORDER_ID = "order_id"
        private const val PHONE_LOOKUP_MIN_LEN = 7
    }

    private lateinit var binding: ActivityNewOrderBinding
    private lateinit var repo: Repository
    private lateinit var prefs: Prefs

    private var orderType: String = OrderType.DINE_IN
    private var existingOrderId: Long? = null
    private var existingOrder: Order? = null
    private var allMenuItems: List<MenuItem> = emptyList()
    private var allFlavors: List<FlavorOption> = emptyList()
    private var allZones: List<DeliveryZone> = emptyList()
    private var selectedZone: DeliveryZone? = null

    private val cart = mutableListOf<CartLine>()

    private lateinit var menuAdapter: MenuAdapter
    private var cartAdapter: CartAdapter? = null

    // avoids re-triggering a lookup for the same number, and avoids overwriting fields the
    // user is actively editing for an order that was loaded from an existing customer already
    private var lastLookedUpPhone: String? = null

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
        setupPhoneAutoFill()
        setupDiscountWatcher()

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
        // Phone is always visible/required (Dine-in, Delivery, Takeaway).
        val showCustomerName = orderType != OrderType.DINE_IN
        binding.layoutName.visibility = if (showCustomerName) View.VISIBLE else View.GONE
        binding.layoutAddress.visibility = if (orderType == OrderType.DELIVERY) View.VISIBLE else View.GONE
        binding.layoutZone.visibility = if (orderType == OrderType.DELIVERY) View.VISIBLE else View.GONE
        supportActionBar?.title = when (orderType) {
            OrderType.DINE_IN -> "New Order — Dine-in"
            OrderType.DELIVERY -> "New Order — Delivery"
            else -> "New Order — Takeaway"
        }
        updateCartFooter()
    }

    /** Auto-fills name / address / delivery zone for a returning customer once their phone
     *  number is recognized, so staff don't have to re-type it every time. */
    private fun setupPhoneAutoFill() {
        binding.etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val phone = s?.toString()?.trim().orEmpty()
                if (phone.length < PHONE_LOOKUP_MIN_LEN || phone == lastLookedUpPhone) return
                lastLookedUpPhone = phone
                lookupCustomer(phone)
            }
        })
    }

    private fun lookupCustomer(phone: String) {
        lifecycleScope.launch {
            val customer = repo.customerDao.getByPhone(phone) ?: return@launch
            // Don't clobber text the user already typed for this order.
            if (binding.etName.text.isNullOrBlank() && !customer.name.isNullOrBlank()) {
                binding.etName.setText(customer.name)
            }
            if (orderType == OrderType.DELIVERY && binding.etAddress.text.isNullOrBlank() && !customer.address.isNullOrBlank()) {
                binding.etAddress.setText(customer.address)
            }
            if (orderType == OrderType.DELIVERY && customer.deliveryZoneId != null) {
                val zone = allZones.find { it.id == customer.deliveryZoneId }
                if (zone != null) {
                    selectedZone = zone
                    val label = "${zone.name} (${if (zone.charge <= 0) "Free" else Currency.format(zone.charge)})"
                    binding.spZone.setText(label, false)
                    updateCartFooter()
                }
            }
            Toast.makeText(this@NewOrderActivity, "Loaded saved customer details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDiscountWatcher() {
        binding.etDiscount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = updateCartFooter()
        })
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

    private fun currentDiscount(): Double {
        val raw = binding.etDiscount.text?.toString()?.toDoubleOrNull() ?: 0.0
        return raw.coerceAtLeast(0.0)
    }

    private fun updateCartFooter() {
        val count = cart.sumOf { it.qty }
        val subtotal = currentSubtotal()
        val delivery = currentDeliveryCharge()
        val discount = currentDiscount().coerceAtMost(subtotal + delivery)
        binding.tvCartSummary.text = "Cart: $count item${if (count == 1) "" else "s"}"
        binding.tvCartTotal.text = Currency.format(subtotal + delivery - discount)
        if (discount > 0) {
            binding.tvDiscountApplied.visibility = View.VISIBLE
            binding.tvDiscountApplied.text = "Discount applied: ${Currency.format(discount)}"
        } else {
            binding.tvDiscountApplied.visibility = View.GONE
        }
    }

    private fun loadExistingOrder(orderId: Long) {
        lifecycleScope.launch {
            val order = repo.orderDao.getOrderById(orderId) ?: return@launch
            existingOrder = order
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
            // Pre-fill phone without triggering an auto-fill lookup that could overwrite it.
            lastLookedUpPhone = order.phone
            binding.etPhone.setText(order.phone ?: "")
            binding.etAddress.setText(order.address ?: "")
            if (order.discountAmount > 0) {
                binding.etDiscount.setText(Currency.plain(order.discountAmount))
            }
            binding.etDiscountNote.setText(order.discountNote ?: "")

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
        // Contact number is required for every order type: Dine-in, Delivery and Takeaway.
        if (binding.etPhone.text.isNullOrBlank()) {
            Toast.makeText(this, "Enter a contact number", Toast.LENGTH_SHORT).show()
            return
        }
        if (orderType == OrderType.DELIVERY) {
            when {
                binding.etName.text.isNullOrBlank() -> {
                    Toast.makeText(this, "Enter the customer's name", Toast.LENGTH_SHORT).show()
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
        val discount = currentDiscount().coerceAtMost(subtotal + delivery)
        val discountNote = binding.etDiscountNote.text?.toString()?.trim().takeUnless { it.isNullOrBlank() }
        val phone = binding.etPhone.text?.toString()?.trim()
        val name = binding.etName.text?.toString()?.trim()
        val address = binding.etAddress.text?.toString()?.trim()

        lifecycleScope.launch {
            val orderId: Long
            if (existingOrderId != null) {
                val existing = repo.orderDao.getOrderById(existingOrderId!!)!!
                // Day/invoice number are assigned once, at creation, and kept stable on edits.
                val dateKey = existing.orderDate.ifBlank { DateUtil.dateKey(existing.createdAt) }
                val invoiceNumber = if (existing.invoiceNumber > 0) existing.invoiceNumber
                    else repo.orderDao.countForDate(dateKey) + 1
                val updated = existing.copy(
                    type = orderType,
                    tableNumber = binding.etTable.text?.toString(),
                    customerName = name,
                    phone = phone,
                    address = address,
                    deliveryZoneId = selectedZone?.id,
                    deliveryCharge = delivery,
                    subtotal = subtotal,
                    discountAmount = discount,
                    discountNote = discountNote,
                    total = subtotal + delivery - discount,
                    status = status,
                    orderDate = dateKey,
                    invoiceNumber = invoiceNumber,
                    updatedAt = System.currentTimeMillis()
                )
                repo.orderDao.updateOrder(updated)
                repo.orderDao.clearItemsForOrder(existingOrderId!!)
                orderId = existingOrderId!!
            } else {
                val dateKey = DateUtil.todayKey()
                val invoiceNumber = repo.orderDao.countForDate(dateKey) + 1
                val newOrder = Order(
                    type = orderType,
                    tableNumber = binding.etTable.text?.toString(),
                    customerName = name,
                    phone = phone,
                    address = address,
                    deliveryZoneId = selectedZone?.id,
                    deliveryCharge = delivery,
                    subtotal = subtotal,
                    discountAmount = discount,
                    discountNote = discountNote,
                    total = subtotal + delivery - discount,
                    status = status,
                    orderDate = dateKey,
                    invoiceNumber = invoiceNumber
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

            // Remember this customer (by phone) so their next order auto-fills — this is
            // written to the on-device Room/SQLite database, i.e. persisted local storage.
            if (!phone.isNullOrBlank()) {
                repo.customerDao.upsert(
                    Customer(
                        phone = phone,
                        name = name,
                        address = address,
                        deliveryZoneId = selectedZone?.id,
                        lastOrderType = orderType
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
