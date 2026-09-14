package com.pizzacafe.badin.data

/**
 * Menu data transcribed from the Pizza Cafe (Badin) printed menu board.
 * Prices in PKR (Rs.). Edit freely from the in-app Menu Manager screen —
 * this is only the initial seed used the first time the app runs.
 */
object SeedData {

    fun menuItems(): List<MenuItem> = listOf(
        // ---- Pizza sizes (pick a flavor at order time, price is by size) ----
        MenuItem(category = "Pizza", name = "Small Pizza", price = 450.0, hasFlavorOption = true),
        MenuItem(category = "Pizza", name = "Medium Pizza", price = 900.0, hasFlavorOption = true),
        MenuItem(category = "Pizza", name = "Large Pizza", price = 1150.0, hasFlavorOption = true),
        MenuItem(category = "Pizza", name = "Kabab Crust Pizza", price = 1300.0, hasFlavorOption = true),
        MenuItem(category = "Pizza", name = "Crown Pizza", price = 1300.0, hasFlavorOption = true),
        MenuItem(category = "Pizza", name = "Malai Boti Pizza", price = 1300.0, hasFlavorOption = true),

        // ---- BBQ ----
        MenuItem(category = "BBQ", name = "Chicken Tikka Spicy Chest", price = 400.0),
        MenuItem(category = "BBQ", name = "Chicken Malai Tikka Chest", price = 450.0),
        MenuItem(category = "BBQ", name = "Chicken Spicy Boti", price = 600.0),
        MenuItem(category = "BBQ", name = "Chicken Malai Boti", price = 700.0),
        MenuItem(category = "BBQ", name = "Reshmi Kabab", price = 700.0),
        MenuItem(category = "BBQ", name = "Gola Kabab", price = 750.0),
        MenuItem(category = "BBQ", name = "Achari Tikka", price = 450.0),
        MenuItem(category = "BBQ", name = "Leg Tikka", price = 350.0),

        // ---- Fries ----
        MenuItem(category = "Fries", name = "Plane Fries", price = 200.0),
        MenuItem(category = "Fries", name = "Masala Fries", price = 200.0),
        MenuItem(category = "Fries", name = "Mayo Fries", price = 250.0),
        MenuItem(category = "Fries", name = "Garlic Fries", price = 250.0),
        MenuItem(category = "Fries", name = "Spicy Fries", price = 200.0),
        MenuItem(category = "Fries", name = "Special Pizza Fries", price = 450.0),

        // ---- Sandwiches ----
        MenuItem(category = "Sandwiches", name = "Club Sandwich", price = 450.0),
        MenuItem(category = "Sandwiches", name = "Mexican Sandwich", price = 450.0),
        MenuItem(category = "Sandwiches", name = "Fajita Sandwich", price = 450.0),
        MenuItem(category = "Sandwiches", name = "Mozzarilla Sandwich", price = 450.0),

        // ---- Mouth Watering (Burgers/Broast) ----
        MenuItem(category = "Burgers", name = "Zinger Burger", price = 400.0),
        MenuItem(category = "Burgers", name = "Big Broast", price = 500.0),
        MenuItem(category = "Burgers", name = "Cheesy Burger", price = 550.0),
        MenuItem(category = "Burgers", name = "Mighty Burger", price = 550.0),
        MenuItem(category = "Burgers", name = "Double Decker Burger", price = 600.0),
        MenuItem(category = "Burgers", name = "Peti Burger", price = 350.0),
        MenuItem(category = "Burgers", name = "Qtr Broast", price = 400.0), // price partly hidden on menu photo — verify & edit

        // ---- Pasta ----
        MenuItem(category = "Pasta", name = "Makhni Pasta", price = 500.0),
        MenuItem(category = "Pasta", name = "Chessy Pasta", price = 650.0),
        MenuItem(category = "Pasta", name = "Plain Pasta", price = 400.0),
        MenuItem(category = "Pasta", name = "Chicken Pasta", price = 450.0),
        MenuItem(category = "Pasta", name = "Chicken Chowmein", price = 500.0),

        // ---- Rice ----
        MenuItem(category = "Rice", name = "Chicken Fried Rice", price = 450.0),
        MenuItem(category = "Rice", name = "Chicken Shashlick Rice", price = 550.0),
        MenuItem(category = "Rice", name = "Chicken Manchurian Rice", price = 550.0),
        MenuItem(category = "Rice", name = "Chicken Jalfrezi Rice", price = 550.0),
        MenuItem(category = "Rice", name = "Chicken Sindhi Biryani", price = 500.0),

        // ---- Rolls ----
        MenuItem(category = "Rolls", name = "Special Pizza Cafe Roll", price = 250.0),
        MenuItem(category = "Rolls", name = "Chicken Roll", price = 200.0),
        MenuItem(category = "Rolls", name = "Bar BQ Roll", price = 200.0),
        MenuItem(category = "Rolls", name = "Crispy Spring Roll", price = 200.0),
        MenuItem(category = "Rolls", name = "Special Pizza Roll", price = 230.0),
        MenuItem(category = "Rolls", name = "Mayo Roll", price = 200.0),
        MenuItem(category = "Rolls", name = "Special Scilian Roll", price = 200.0),
        MenuItem(category = "Rolls", name = "Green Chatni Roll", price = 200.0),
        MenuItem(category = "Rolls", name = "Special Twister Roll", price = 200.0),
        MenuItem(category = "Rolls", name = "Special Garlic Roll", price = 200.0),
        MenuItem(category = "Rolls", name = "Arabic Roll", price = 200.0),
        MenuItem(category = "Rolls", name = "Vegetable Roll", price = 200.0),
        MenuItem(category = "Rolls", name = "Shawarma Roll", price = 200.0),

        // ---- Extra Toppings (Small/Medium/Large) ----
        MenuItem(category = "Extra Topping", name = "Extra Meat (Small)", price = 100.0),
        MenuItem(category = "Extra Topping", name = "Extra Meat (Medium)", price = 150.0),
        MenuItem(category = "Extra Topping", name = "Extra Meat (Large)", price = 200.0),
        MenuItem(category = "Extra Topping", name = "Extra Cheese (Small)", price = 100.0),
        MenuItem(category = "Extra Topping", name = "Extra Cheese (Medium)", price = 150.0),
        MenuItem(category = "Extra Topping", name = "Extra Cheese (Large)", price = 200.0),

        // ---- Deals ----
        MenuItem(category = "Deals", name = "Deal 1: Small Pizza + Roll + Mini Coldrink", price = 650.0),
        MenuItem(category = "Deals", name = "Deal 2: Zinger Burger + Fries + Mini Cola Next", price = 500.0),
        MenuItem(category = "Deals", name = "Deal 3: 3 Rolls + 500ml Cola Next", price = 650.0),
        MenuItem(category = "Deals", name = "Deal 4: 2 Big Broast + Fries + 500ml Cola Next", price = 950.0),
        MenuItem(category = "Deals", name = "Deal 5: 2 Zinger Burger + Fries + 500ml Cola Next", price = 900.0),
        MenuItem(category = "Deals", name = "Deal 6: 2 Large Pizza + 1.5L Cola Next", price = 2300.0),
        MenuItem(category = "Deals", name = "Deal 7: Zinger Burger + Small Pizza + Fries + 500ml Cola Next", price = 950.0),
        MenuItem(category = "Deals", name = "Deal 8: Small Pizza + Broast + Fries + 500ml Cola Next", price = 1050.0),
        MenuItem(category = "Deals", name = "Deal 9: Broast + Fries + Mini Cola Next", price = 550.0),
        MenuItem(category = "Deals", name = "Deal 10: Large Pizza + Big Broast + Zinger Burger + Fries + 1.5L Cola Next", price = 2050.0),
        MenuItem(category = "Deals", name = "Deal 11: Cheese Pasta + Zinger Burger + 500ml Cola Next", price = 1050.0),
        MenuItem(category = "Deals", name = "Deal 12: Medium Pizza + Zinger Burger + Fries + 1.5L Cola Next", price = 1400.0),
        MenuItem(category = "Deals", name = "Midnight Deal: Medium Pizza + Small Pizza + 1L Cola Next", price = 1300.0),
        MenuItem(category = "Deals", name = "Friday Deal: Large Pizza + Medium Pizza + 1.5L Cola Next", price = 2000.0),
        MenuItem(category = "Deals", name = "Friday Deal: Large Pizza + Small Pizza + 1.5L Cola Next", price = 1500.0)
    )

    fun pizzaFlavors(): List<FlavorOption> = listOf(
        "B.BQ Tikka", "Fajita", "Chicken Tikka", "Scilian", "Arabic", "Shawarma",
        "Mayo Creamy", "Cheese Lover", "Veg Lover", "Bihari", "Chicken Malai", "Supreme"
    ).map { FlavorOption(name = it) }

    fun deliveryZones(): List<DeliveryZone> = listOf(
        DeliveryZone(name = "Standard (near Nohria Mohla)", charge = 100.0),
        DeliveryZone(name = "Free delivery (order Rs.500+)", charge = 0.0)
    )
}
