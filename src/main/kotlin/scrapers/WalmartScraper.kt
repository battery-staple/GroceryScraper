package scrapers

import com.microsoft.playwright.BrowserContext
import models.Product
import models.ScrapeRequest
import models.ScrapeResult

class WalmartScraper : Scraper {
    override val storeName: String = "Walmart"

    override suspend fun scrape(context: BrowserContext, request: ScrapeRequest): ScrapeResult {
        val page = PlaywrightPageProxy(context.newPage())
        return try {
            scrapeWithPage(page, request)
        } finally {
            page.close()
        }
    }

    suspend fun scrapeWithPage(page: PageProxy, request: ScrapeRequest): ScrapeResult {
        println("[$storeName] Navigating to Walmart...")
        page.navigate("https://www.walmart.com/search?q=${request.query}")
        
        // Set Zip Code if needed
        println("[$storeName] Setting Zip Code to ${request.zipCode}...")
        setZipCode(page, request.zipCode)
        
        println("[$storeName] Waiting for results...")
        // Walmart uses 'item-stack' for search results
        try {
            page.waitForLoadState("networkidle")
            page.waitForSelector("[data-testid='item-stack']", 15000.0)
        } catch (e: Exception) {
            val title = try { page.title() } catch (t: Exception) { "Unknown" }
            if (title.contains("Robot or human", ignoreCase = true)) {
                if (request.isHeadless) {
                    return ScrapeResult.RetryNonHeadless(storeName, "Bot detection triggered on search page")
                }
                println("[$storeName] Robot detection triggered. Please solve the captcha in the browser window.")
                // Wait indefinitely for manual interaction
                page.waitForSelector("[data-testid='item-stack']", 0.0)
            } else {
                return ScrapeResult.Failure(storeName, "Timeout waiting for results (Title: $title): ${e.message}")
            }
        }
        
        println("[$storeName] Parsing results...")
        // Target elements with data-item-id inside the stack
        val results = page.querySelectorAll("div[data-testid='item-stack'] div[data-item-id]").mapNotNull { element ->
            val name = element.querySelector("[data-automation-id='product-title']")?.textContent() ?: return@mapNotNull null
            
            val priceContainerText = element.querySelector("[data-automation-id='product-price']")?.textContent()
            val priceCents = priceContainerText?.let { parsePrice(it) }
            
            // Be more flexible with the link selection
            val link = element.querySelector("a[data-automation-id='product-title-link']")?.getAttribute("href")
                ?: element.querySelector("a[href*='/ip/']")?.getAttribute("href")
                ?: return@mapNotNull null
            
            Product(
                store = storeName,
                priceCents = priceCents,
                link = if (link.startsWith("http") || link.startsWith("//")) link else "https://www.walmart.com$link",
                productName = name.trim()
            )
        }
        
        return if (results.isEmpty()) {
            ScrapeResult.Failure(storeName, "No results found for query: ${request.query}")
        } else {
            ScrapeResult.Success(results)
        }
    }

    private fun setZipCode(page: PageProxy, zipCode: String) {
        try {
            // Walmart's location selector often starts with a button in the header
            page.click("button:has-text('How do you want your items?')", 3000.0)
            page.fill("input[aria-label='Zip code']", zipCode)
            page.click("button:has-text('Save')")
            page.waitForTimeout(1000.0)
        } catch (e: Exception) {
            // Non-critical, ignore if button not found (layout might be different or already set)
        }
    }

    private fun parsePrice(priceText: String): Int? {
        // Find the first occurrence of something like $3.95 or 3.95
        val match = Regex("""\$?\d+\.\d{2}""").find(priceText)
        return if (match != null) {
            val numeric = match.value.replace("$", "").toDoubleOrNull() ?: return null
            (numeric * 100).toInt()
        } else {
            null
        }
    }
}
