package scrapers

class AldiScraper : InstacartScraper() {
    override val storeName: String = "Aldi"
    override val baseUrl: String = "https://www.aldi.us"
    override val storeId: String = "aldi"
}
