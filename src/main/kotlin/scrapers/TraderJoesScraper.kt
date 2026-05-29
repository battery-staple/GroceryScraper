package scrapers

import com.microsoft.playwright.BrowserContext
import models.Product
import models.ScrapeRequest
import models.ScrapeResult
import models.ScrapeState
import scrapers.ScrapingUtils.parsePrice

class TraderJoesScraper : Scraper {
    override val storeName: String = "Trader Joe's"

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
        val url = "https://www.traderjoes.com/home/search?q=${request.query}"
        onState(ScrapeState.Navigating(storeName, url))
        page.navigate(url)
        
        onState(ScrapeState.WaitingForResults(storeName))
        try {
            page.waitForLoadState("networkidle")
            // Target the article card which is stable even if hashes change
            page.waitForSelector("article[class*='SearchResultCard_searchResultCard__']", 15000.0)
        } catch (e: Exception) {
            val title = try { page.title() } catch (t: Exception) { "Unknown" }
            return ScrapeResult.Failure(storeName, "Timeout waiting for results (Title: $title): ${e.message}")
        }
        
        onState(ScrapeState.Parsing(storeName))
        val results = page.querySelectorAll("article[class*='SearchResultCard_searchResultCard__']").mapNotNull { element ->
            // Use contains selectors for resilience against hashed class names
            val nameElement = element.querySelector("a[class*='SearchResultCard_searchResultCard__titleLink__']")
            val name = nameElement?.textContent() ?: return@mapNotNull null
            
            val priceText = element.querySelector("span[class*='ProductPrice_productPrice__price__']")?.textContent() 
                ?: return@mapNotNull null // If no price, it's likely a recipe/story, not a product
                
            val link = nameElement.getAttribute("href") ?: return@mapNotNull null
            
            Product(
                store = storeName,
                priceCents = parsePrice(priceText),
                link = if (link.startsWith("http")) link else "https://www.traderjoes.com$link",
                productName = name.trim()
            )
        }
        
        return if (results.isEmpty()) {
            ScrapeResult.Failure(storeName, "No results found for query: ${request.query}")
        } else {
            ScrapeResult.Success(results)
        }
    }
}
