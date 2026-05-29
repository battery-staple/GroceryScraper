package scrapers

import com.microsoft.playwright.BrowserContext
import models.Product
import models.ScrapeRequest
import models.ScrapeResult
import models.ScrapeState
import scrapers.ScrapingUtils.parsePrice

class WegmansScraper : Scraper {
    override val storeName: String = "Wegmans"

    override suspend fun scrape(
        context: BrowserContext, 
        request: ScrapeRequest,
        onState: suspend (ScrapeState) -> Unit
    ): ScrapeResult {
        val page = PlaywrightPageProxy(context.newPage())
        return try {
            scrapeWithPage(page, request, onState)
        } finally {
            page.close()
        }
    }

    suspend fun scrapeWithPage(
        page: PageProxy, 
        request: ScrapeRequest,
        onState: suspend (ScrapeState) -> Unit = {}
    ): ScrapeResult {
        val url = "https://www.wegmans.com/shop/search?query=${request.query}"
        onState(ScrapeState.Navigating(storeName, url))
        page.navigate(url)
        
        onState(ScrapeState.SettingLocation(storeName, request.zipCode))
        setStore(page, request.zipCode, onState)
        
        onState(ScrapeState.WaitingForResults(storeName))
        try {
            // Wait for product cards to appear
            page.waitForSelector(".component--product-tile", 20000.0)
        } catch (e: Exception) {
            val title = try { page.title() } catch (t: Exception) { "Unknown" }
            return ScrapeResult.Failure(storeName, "Timeout waiting for results (Title: $title): ${e.message}")
        }
        
        onState(ScrapeState.Parsing(storeName))
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

    private suspend fun setStore(page: PageProxy, zipCode: String, onState: suspend (ScrapeState) -> Unit) {
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
            onState(ScrapeState.Warning(storeName, "Failed to set store: ${e.message}"))
        }
    }

}
