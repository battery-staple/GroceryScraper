package scrapers

class FoodBazaarScraper : InstacartScraper() {
    override val storeName: String = "Food Bazaar"
    override val baseUrl: String = "https://shop.foodbazaar.com"
    override val storeId: String = "food-bazaar"
}
