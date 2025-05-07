package kz.tilek.lottus.viewmodels

enum class MyAuctionFilterType(val value: String) {
    PARTICIPATING("PARTICIPATING"), // Аукционы, в которых участвую
    WON("WON"),                     // Выигранные аукционы
    CREATED("CREATED")              // Созданные мной аукционы
}
