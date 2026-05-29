package scrapers

import com.microsoft.playwright.BrowserContext
import models.Product
import models.ScrapeRequest
import models.ScrapeResult
import models.ScrapeState
import scrapers.ScrapingUtils.parsePrice

class WalmartScraper : Scraper {
    override val storeName: String = "Walmart"

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
        val url = "https://www.walmart.com/search?q=${request.query}"
        onState(ScrapeState.Navigating(storeName, url))
        page.navigate(url)
        
        // Set Zip Code if needed
        onState(ScrapeState.SettingLocation(storeName, request.zipCode))
        setZipCode(page, request.zipCode, onState)
        
        onState(ScrapeState.WaitingForResults(storeName))
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
                onState(ScrapeState.WaitingForResults(storeName, "Robot detection triggered. Please solve the captcha in the browser window."))
                // Wait indefinitely for manual interaction
                page.waitForSelector("[data-testid='item-stack']", 0.0)
            } else {
                return ScrapeResult.Failure(storeName, "Timeout waiting for results (Title: $title): ${e.message}")
            }
        }
        
        onState(ScrapeState.Parsing(storeName))
        val results = parseResults(page, request)
        
        return if (results.isEmpty()) {
            ScrapeResult.Failure(storeName, "No results found for query: ${request.query}")
        } else {
            ScrapeResult.Success(results)
        }
    }

    private fun parseResults(page: PageProxy, request: ScrapeRequest): List<Product> {
        // Target elements with data-item-id inside the stack
        return page.querySelectorAll("div[data-testid='item-stack'] div[data-item-id]").mapNotNull { element ->
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
    }

    private suspend fun setZipCode(page: PageProxy, zipCode: String, onState: suspend (ScrapeState) -> Unit) {
        try {
            // Walmart's location selector often starts with a button in the header
            page.click("button:has-text('How do you want your items?')", 3000.0)
            page.fill("input[aria-label='Zip code']", zipCode)
            page.click("button:has-text('Save')")
            page.waitForTimeout(1000.0)
        } catch (e: Exception) {
            onState(ScrapeState.Warning(storeName, "Failed to set zip code: ${e.message}"))
        }
    }
}
