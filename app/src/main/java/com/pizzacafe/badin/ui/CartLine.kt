package com.pizzacafe.badin.ui

data class CartLine(
    val menuItemId: Long,
    val name: String,
    val unitPrice: Double,
    var qty: Int,
    var flavorNote: String? = null
) {
    val lineTotal: Double get() = unitPrice * qty
}
