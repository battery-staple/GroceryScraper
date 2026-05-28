package scrapers

import com.microsoft.playwright.BrowserContext
import models.Product
import models.ScrapeRequest
import models.ScrapeResult

class WegmansScraper : Scraper {
    override val storeName: String = "Wegmans"

    override suspend fun scrape(context: BrowserContext, request: ScrapeRequest): ScrapeResult {
        val page = PlaywrightPageProxy(context.newPage())
        return try {
            scrapeWithPage(page, request)
        } finally {
            page.close()
        }
    }

    suspend fun scrapeWithPage(page: PageProxy, request: ScrapeRequest): ScrapeResult {
        println("[$storeName] Navigating to Wegmans...")
        page.navigate("https://www.wegmans.com/shop/search?query=${request.query}")
        
        println("[$storeName] Setting Zip Code to ${request.zipCode}...")
        setStore(page, request.zipCode)
        
        println("[$storeName] Waiting for results...")
        try {
            // Wait for product cards to appear
            page.waitForSelector(".component--product-tile", 20000.0)
        } catch (e: Exception) {
            val title = try { page.title() } catch (t: Exception) { "Unknown" }
            return ScrapeResult.Failure(storeName, "Timeout waiting for results (Title: $title): ${e.message}")
        }
        
        println("[$storeName] Parsing results...")
        val results = page.querySelectorAll(".component--product-tile").mapNotNull { element ->
            val name = element.querySelector("h3.component--base-heading")?.textContent() 
                ?: element.querySelector(".global--card-title")?.textContent()
            
            val priceText = element.querySelector(".price b")?.textContent() 
                ?: element.querySelector(".price")?.textContent()
            
            // Wegmans tiles have the ID on a child element: <div data-bv-product-id="SKU_12345">
            val productIdAttr = element.querySelector("[data-bv-product-id]")?.getAttribute("data-bv-product-id") ?: ""
            val id = productIdAttr.removePrefix("SKU_")
            
            if (name == null || priceText == null || id.isEmpty()) {
                return@mapNotNull null
            }
            
            val link = "https://www.wegmans.com/shop/product/$id"
            
            Product(
                store = storeName,
                priceCents = parsePrice(priceText),
                link = if (link.startsWith("http")) link else "https://shop.wegmans.com$link",
                productName = name.trim()
            )
        }
        
        return if (results.isEmpty()) {
            ScrapeResult.Failure(storeName, "No results found for query: ${request.query}")
        } else {
            ScrapeResult.Success(results)
        }
    }

    private fun setStore(page: PageProxy, zipCode: String) {
        try {
            // Wegmans often shows a location selector in the header
            val selectButton = page.querySelector("button:has-text('Select a Store')") ?:
                               page.querySelector("button:has-text('In-Store')") ?:
                               page.querySelector("button.header-location-button")
                               
            if (selectButton != null) {
                selectButton.click()
                page.fill("input[placeholder*='Zip Code']", zipCode)
                page.waitForTimeout(500.0)
                page.click(".store-list-item button:has-text('Make This My Store')", 2000.0)
            }
        } catch (e: Exception) {
            // Non-critical
        }
    }

    private fun parsePrice(priceText: String): Int? {
        val match = Regex("""\$?\d+\.\d{2}""").find(priceText)
        return if (match != null) {
            val numeric = match.value.replace("$", "").toDoubleOrNull() ?: return null
            (numeric * 100).toInt()
        } else {
            null
        }
    }
}
