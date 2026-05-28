package scrapers

class TopsScraper : InstacartScraper() {
    override val storeName: String = "Tops Markets"
    override val baseUrl: String = "https://shop.topsmarkets.com"
    override val storeId: String = "tops-markets"
}
