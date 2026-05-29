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
     * @param onState A callback to report scraping state.
     */
    suspend fun scrape(
        context: BrowserContext, 
        request: ScrapeRequest,
        onState: suspend (models.ScrapeState) -> Unit = {}
    ): ScrapeResult
}
