package com.moneytracker.vault.domain.model

enum class SpendCategory(
    val displayName: String,
    val subtitle: String,
    val colorHex: Long
) {
    FOOD_DINING(
        displayName = "Food & Dining",
        subtitle = "Food Delivery, Cafes, Restaurants & Bakeries",
        colorHex = 0xFFEE4D37
    ),
    GROCERIES(
        displayName = "Groceries",
        subtitle = "Quick Commerce, Supermarkets & Daily Essentials",
        colorHex = 0xFF06C270
    ),
    SHOPPING(
        displayName = "Shopping",
        subtitle = "E-Commerce, Apparel, Electronics & Lifestyle",
        colorHex = 0xFFF5C518
    ),
    ENTERTAINMENT(
        displayName = "Entertainment",
        subtitle = "OTT, Cloud, Movies, Gaming & Subscriptions",
        colorHex = 0xFFA855F7
    ),
    TRANSPORT_TRAVEL(
        displayName = "Transport & Travel",
        subtitle = "Cabs, Metro, Flights, Trains, Hotels & FASTag",
        colorHex = 0xFF38BDF8
    ),
    BILLS_UTILITIES(
        displayName = "Bills & Utilities",
        subtitle = "Mobile, Broadband, Electricity, Gas & Water",
        colorHex = 0xFF818CF8
    ),
    FUEL_AUTO(
        displayName = "Fuel & Auto",
        subtitle = "Petrol Pumps, EV Charging & Vehicle Care",
        colorHex = 0xFFFB923C
    ),
    HEALTH_WELLNESS(
        displayName = "Health & Wellness",
        subtitle = "Pharmacies, Fitness, Diagnostics & Clinics",
        colorHex = 0xFFF43F5E
    ),
    INVESTMENTS_FINANCE(
        displayName = "Finance & Invest",
        subtitle = "Mutual Funds, Stocks, Insurance & EMIs",
        colorHex = 0xFFE5C158
    ),
    EDUCATION(
        displayName = "Education",
        subtitle = "Courses, Books, Exams & Learning Platforms",
        colorHex = 0xFF2DD4BF
    ),
    CREDIT_CARD_BILL(
        displayName = "Card Bill",
        subtitle = "CRED Club, Kiwi & Credit Card Bill Payments",
        colorHex = 0xFF00E5FF
    ),
    SELF_TRANSFER(
        displayName = "Self Transfer",
        subtitle = "Transfers between your own accounts & wallets",
        colorHex = 0xFFFFD60A
    ),
    TRANSFERS(
        displayName = "People & Transfers",
        subtitle = "UPI Transfers to Contacts & ATM Cash",
        colorHex = 0xFF94A3B8
    ),
    OTHERS(
        displayName = "General Spend",
        subtitle = "Retail & Miscellaneous Spends",
        colorHex = 0xFF71717A
    );

    companion object {
        fun fromNameOrDefault(name: String?): SpendCategory {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: OTHERS
        }
    }
}
