package scrapers

import com.microsoft.playwright.BrowserContext
import models.ScrapeRequest
import models.ScrapeResult

interface Scraper {
    val storeName: String
    
    /**
     * Scrapes the store for a given request.
     * @param context The Playwright BrowserContext to use for scraping.
     * @param request The search query and zip code.
     */
    suspend fun scrape(context: BrowserContext, request: ScrapeRequest): ScrapeResult
}
