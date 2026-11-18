package com.moneytracker.vault.ai

import com.moneytracker.vault.domain.model.SpendCategory

/**
 * Full ISO 18245 Merchant Category Code (MCC) offline lookup directory.
 * Resolves 4-digit Visa / Mastercard / RuPay / UPI P2M MCC codes completely offline.
 */
object MccDirectory {

    data class MccInfo(
        val code: Int,
        val description: String,
        val category: SpendCategory
    )

    private val exactMccMap: Map<Int, Pair<String, SpendCategory>> = mapOf(
        // Entertainment, OTT, Cloud & Digital Goods
        4899 to ("Streaming, Cable & Cloud Subscription" to SpendCategory.ENTERTAINMENT),
        5815 to ("Digital Media, Movies & Music" to SpendCategory.ENTERTAINMENT),
        5816 to ("Digital Games & Gaming Platforms" to SpendCategory.ENTERTAINMENT),
        5817 to ("Digital Applications & Cloud Storage" to SpendCategory.ENTERTAINMENT),
        5818 to ("Digital Goods Multi-Category" to SpendCategory.ENTERTAINMENT),
        7832 to ("Motion Picture Theaters & Multiplexes" to SpendCategory.ENTERTAINMENT),
        7922 to ("Concerts, Events & Ticket Agencies" to SpendCategory.ENTERTAINMENT),
        7996 to ("Amusement Parks & Recreation" to SpendCategory.ENTERTAINMENT),

        // Food & Dining
        5811 to ("Caterers & Cloud Kitchens" to SpendCategory.FOOD_DINING),
        5812 to ("Restaurants & Dining" to SpendCategory.FOOD_DINING),
        5813 to ("Bars, Cafes & Lounges" to SpendCategory.FOOD_DINING),
        5814 to ("Quick Service Restaurants & Fast Food" to SpendCategory.FOOD_DINING),
        5462 to ("Bakeries & Confectionery" to SpendCategory.FOOD_DINING),
        5441 to ("Sweets, Candy & Nut Stores" to SpendCategory.FOOD_DINING),

        // Groceries & Supermarkets
        5411 to ("Grocery Stores & Supermarkets" to SpendCategory.GROCERIES),
        5422 to ("Meat, Poultry & Seafood Markets" to SpendCategory.GROCERIES),
        5451 to ("Dairy Products Stores" to SpendCategory.GROCERIES),
        5499 to ("Quick Commerce & Convenience Stores" to SpendCategory.GROCERIES),

        // Transport & Travel
        4111 to ("Metro, Commuter Rail & Local Transit" to SpendCategory.TRANSPORT_TRAVEL),
        4112 to ("Passenger Railways (IRCTC)" to SpendCategory.TRANSPORT_TRAVEL),
        4121 to ("Taxicabs, Ride-Hailing & Auto" to SpendCategory.TRANSPORT_TRAVEL),
        4131 to ("Intercity Bus Lines" to SpendCategory.TRANSPORT_TRAVEL),
        4511 to ("Airlines & Air Carriers" to SpendCategory.TRANSPORT_TRAVEL),
        4722 to ("Travel Agencies & Tour Operators" to SpendCategory.TRANSPORT_TRAVEL),
        4784 to ("FASTag, Bridge & Highway Tolls" to SpendCategory.TRANSPORT_TRAVEL),
        7011 to ("Hotels, Resorts & Stays" to SpendCategory.TRANSPORT_TRAVEL),
        7523 to ("Parking Lots & Garages" to SpendCategory.TRANSPORT_TRAVEL),

        // Fuel & Auto
        5541 to ("Service Stations & Petrol Pumps" to SpendCategory.FUEL_AUTO),
        5542 to ("Automated Fuel Dispensers" to SpendCategory.FUEL_AUTO),
        5552 to ("Electric Vehicle Charging" to SpendCategory.FUEL_AUTO),
        7538 to ("Automotive Service Shops" to SpendCategory.FUEL_AUTO),

        // Bills & Utilities
        4812 to ("Telecommunication Equipment" to SpendCategory.BILLS_UTILITIES),
        4814 to ("Mobile Recharge & Broadband Services" to SpendCategory.BILLS_UTILITIES),
        4816 to ("Internet & Network Services" to SpendCategory.BILLS_UTILITIES),
        4900 to ("Electricity, Gas & Water Utilities" to SpendCategory.BILLS_UTILITIES),

        // Shopping & Retail
        5200 to ("Home Supply Warehouse" to SpendCategory.SHOPPING),
        5310 to ("Discount Stores" to SpendCategory.SHOPPING),
        5311 to ("Department Stores & E-Commerce" to SpendCategory.SHOPPING),
        5399 to ("General Merchandise Marketplace" to SpendCategory.SHOPPING),
        5651 to ("Family Clothing & Apparel" to SpendCategory.SHOPPING),
        5691 to ("Men's & Women's Clothing" to SpendCategory.SHOPPING),
        5661 to ("Shoe & Footwear Stores" to SpendCategory.SHOPPING),
        5712 to ("Furniture & Home Furnishings" to SpendCategory.SHOPPING),
        5732 to ("Electronics & Appliances" to SpendCategory.SHOPPING),
        5734 to ("Computer Software Stores" to SpendCategory.SHOPPING),
        5942 to ("Book Stores" to SpendCategory.SHOPPING),
        5944 to ("Jewelry, Watches & Silverware" to SpendCategory.SHOPPING),
        5977 to ("Cosmetics & Beauty Stores" to SpendCategory.SHOPPING),

        // Health & Wellness
        5912 to ("Drug Stores & Pharmacies" to SpendCategory.HEALTH_WELLNESS),
        7230 to ("Salons, Grooming & Spa" to SpendCategory.HEALTH_WELLNESS),
        7997 to ("Fitness Clubs & Gyms" to SpendCategory.HEALTH_WELLNESS),
        8011 to ("Doctors & Clinics" to SpendCategory.HEALTH_WELLNESS),
        8021 to ("Dentists & Orthodontics" to SpendCategory.HEALTH_WELLNESS),
        8062 to ("Hospitals & Healthcare" to SpendCategory.HEALTH_WELLNESS),
        8071 to ("Medical & Diagnostic Laboratories" to SpendCategory.HEALTH_WELLNESS),
        8099 to ("Health Practitioners & Wellness" to SpendCategory.HEALTH_WELLNESS),

        // Finance & Investments
        6011 to ("ATM Cash Withdrawal" to SpendCategory.TRANSFERS),
        6012 to ("Financial Institutions" to SpendCategory.INVESTMENTS_FINANCE),
        6211 to ("Stock Brokers, Mutual Funds & Wealth" to SpendCategory.INVESTMENTS_FINANCE),
        6300 to ("Insurance Premiums" to SpendCategory.INVESTMENTS_FINANCE),

        // Education
        8211 to ("Schools & Secondary Education" to SpendCategory.EDUCATION),
        8220 to ("Colleges & Universities" to SpendCategory.EDUCATION),
        8299 to ("EdTech, Courses & Coaching" to SpendCategory.EDUCATION)
    )

    fun resolve(mccString: String?): MccInfo? {
        val code = mccString?.trim()?.toIntOrNull() ?: return null
        if (code !in 1..9999) return null

        exactMccMap[code]?.let { (desc, cat) ->
            return MccInfo(code = code, description = desc, category = cat)
        }

        // ISO 18245 Range Fallbacks
        val (desc, cat) = when (code) {
            in 3000..3299 -> "Airline Carrier" to SpendCategory.TRANSPORT_TRAVEL
            in 3351..3499 -> "Car Rental Agency" to SpendCategory.TRANSPORT_TRAVEL
            in 3501..3999 -> "Hotel & Hospitality Chain" to SpendCategory.TRANSPORT_TRAVEL
            in 4000..4799 -> "Transportation & Travel Service" to SpendCategory.TRANSPORT_TRAVEL
            in 4800..4999 -> "Telecom & Utility Bill" to SpendCategory.BILLS_UTILITIES
            in 5400..5499 -> "Groceries & Food Market" to SpendCategory.GROCERIES
            in 5500..5599 -> "Fuel & Automotive" to SpendCategory.FUEL_AUTO
            in 5200..5399, in 5600..5799, in 5920..5999 -> "Retail & Shopping" to SpendCategory.SHOPPING
            in 5811..5814 -> "Dining & Food Service" to SpendCategory.FOOD_DINING
            in 5815..5818, in 7800..7996 -> "Entertainment & Digital Media" to SpendCategory.ENTERTAINMENT
            in 6010..6011 -> "Cash / Transfer" to SpendCategory.TRANSFERS
            in 6012..6799 -> "Financial & Investment Service" to SpendCategory.INVESTMENTS_FINANCE
            in 7997..8099 -> "Health, Medical & Fitness" to SpendCategory.HEALTH_WELLNESS
            in 8200..8299 -> "Education & Learning" to SpendCategory.EDUCATION
            else -> return null
        }

        return MccInfo(code = code, description = desc, category = cat)
    }
}
