package scrapers

import com.microsoft.playwright.BrowserContext
import models.Product
import models.ScrapeRequest
import models.ScrapeResult

abstract class InstacartScraper : Scraper {

    protected abstract val baseUrl: String
    protected abstract val storeId: String

    override suspend fun scrape(context: BrowserContext, request: ScrapeRequest): ScrapeResult {
        val page = PlaywrightPageProxy(context.newPage())
        return try {
            scrapeWithPage(page, request)
        } finally {
            page.close()
        }
    }

    suspend fun scrapeWithPage(page: PageProxy, request: ScrapeRequest): ScrapeResult {
        println("[$storeName] Navigating to $storeName...")
        page.navigate("$baseUrl/store/$storeId/s?k=${request.query}")
        
        println("[$storeName] Waiting for results...")
        try {
            handleLocationModal(page, request.zipCode)
            page.waitForSelector("a[href*='/products/']", 20000.0)
        } catch (e: Exception) {
            val title = try { page.title() } catch (t: Exception) { "Unknown" }
            return ScrapeResult.Failure(storeName, "Timeout waiting for results (Title: $title): ${e.message}")
        }
        
        println("[$storeName] Parsing results...")
        val results = page.querySelectorAll("a[href*='/products/']").mapNotNull { element ->
            val name = element.querySelector("[role='heading']")?.textContent() 
                ?: element.querySelector(".e-1gh06cz")?.textContent()
                ?: return@mapNotNull null
            
            val priceText = element.querySelector("span.screen-reader-only")?.textContent() 
                ?: element.querySelector("span.e-gx2pr0")?.textContent()
                ?: element.textContent() 
                ?: return@mapNotNull null
            
            val link = element.getAttribute("href") ?: return@mapNotNull null
            
            Product(
                store = storeName,
                priceCents = parsePrice(priceText),
                link = if (link.startsWith("http")) link else "$baseUrl$link",
                productName = name.trim()
            )
        }
        
        return if (results.isEmpty()) {
            ScrapeResult.Failure(storeName, "No results found for query: ${request.query}")
        } else {
            ScrapeResult.Success(results)
        }
    }

    private fun handleLocationModal(page: PageProxy, zipCode: String) {
        try {
            val button = page.querySelector("button:has-text('Choose a store')") ?:
                         page.querySelector("button:has-text('Choose your store')") ?: 
                         page.querySelector("button:has-text('Select Store')")
            if (button != null) {
                button.click()
                page.fill("input[name='zipCode']", zipCode)
                page.click("button[type='submit']", 2000.0)
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
