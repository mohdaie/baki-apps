package com.mohdaie.baki.parser

/**
 * Keyword-based first guess at a category from a merchant name.
 * Once you correct a category in the popup, the app remembers your choice
 * for that merchant (see Repository), and that wins over these keywords.
 */
object CategoryGuesser {

    private val keywords: List<Pair<String, List<String>>> = listOf(
        // Checked in this order. Food is before transport so "GRAB-EC" / "GRABFOOD"
        // land in Food, while a plain "GRAB" (rides) lands in Transport.
        "food" to listOf(
            "MCDONALD", "MCDONALDS", "MCD", "KFC", "STARBUCKS", "COFFEE", "KOPI", "TEA", "CAFE", "KAFE",
            "RESTORAN", "RESTAURANT", "FOOD", "BAKERY", "BAKERI", "PIZZA", "BURGER", "NASI", "MAMAK",
            "SUSHI", "GRABFOOD", "GRAB-EC", "FOODPANDA", "TEALIVE", "ZUS", "CHATIME", "SUBWAY",
            "DOMINO", "DOMINOS", "MARRYBROWN", "TEXAS CHICKEN", "SECRET RECIPE", "OLDTOWN",
            "A&W", "DINER", "KITCHEN", "BISTRO", "BOOST JUICE", "GIGI COFFEE", "BASKIN",
        ),
        "bills" to listOf(
            "TNB", "TENAGA", "UNIFI", "TELEKOM", "MAXIS", "CELCOM", "DIGI", "CELCOMDIGI", "U MOBILE",
            "UMOBILE", "YES 5G", "AIR SELANGOR", "SYABAS", "RANHILL", "SAJ", "INDAH WATER", "IWK",
            "ASTRO", "TIME DOTCOM", "TIME FIBRE", "SINGTEL", "STARHUB", "SP SERVICES",
        ),
        "health" to listOf(
            "WATSONS", "GUARDIAN", "CARING", "PHARMACY", "FARMASI", "CLINIC", "KLINIK", "HOSPITAL",
            "DENTAL", "MEDICAL", "ALPRO", "BIG PHARMACY", "AA PHARMACY", "SPECIALIST", "POLIKLINIK",
        ),
        "shopping" to listOf(
            "SHOPEE", "LAZADA", "MR DIY", "MRDIY", "IKEA", "UNIQLO", "H&M", "ZALORA", "MYDIN", "AEON",
            "TESCO", "LOTUS", "LOTUSS", "GIANT", "JAYA GROCER", "VILLAGE GROCER", "99 SPEEDMART",
            "SPEEDMART", "FAIRPRICE", "ECONSAVE", "NSK", "DAISO", "DECATHLON", "TAOBAO", "TEMU",
            "AMAZON", "COLD STORAGE", "7-ELEVEN", "7 ELEVEN", "7ELEVEN", "FAMILYMART", "KK MART",
            "SUPERMARKET", "HYPERMARKET", "PARKSON", "SEPHORA", "MART",
        ),
        "entertainment" to listOf(
            "NETFLIX", "SPOTIFY", "YOUTUBE", "DISNEY", "GSC", "TGV", "MBO", "CINEMA", "CINEMAS",
            "STEAM", "PLAYSTATION", "NINTENDO", "BOWLING", "KARAOKE", "TICKET", "TIX", "VIU",
        ),
        "transport" to listOf(
            "TOLL", "PLUS MALAYSIA", "PETRONAS", "SHELL", "PETRON", "CALTEX", "BHP", "PARKING", "SMRT",
            "MRT", "LRT", "KTM", "RAPID", "RAPIDKL", "TRANSIT", "BUS", "BUSES", "TAXI", "GRAB",
            "AIRASIA", "MALAYSIA AIRLINES", "FIREFLY", "BATIK AIR", "TRAINS", "EASYPARKING", "FLEXIPARKING",
        ),
    )

    private val rules: List<Pair<String, Regex>> = keywords.map { (id, words) ->
        val alternatives = words.joinToString("|") { Regex.escape(it) }
        id to Regex("(?i)(?<![A-Za-z0-9])(?:$alternatives)(?![A-Za-z0-9])")
    }

    fun guess(text: String): String? = rules.firstOrNull { it.second.containsMatchIn(text) }?.first
}
