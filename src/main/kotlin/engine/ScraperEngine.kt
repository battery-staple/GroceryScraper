package engine

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import models.FailureReason
import models.FinalResponse
import models.Product
import models.ScrapeRequest
import models.ScrapeResult
import scrapers.Scraper

class ScraperEngine(private val scrapers: List<Scraper>) {

    suspend fun runScrapers(request: ScrapeRequest, headless: Boolean = true): FinalResponse {
        val results = executeScrape(request, headless)

        if (headless && results.any { it is ScrapeResult.RetryNonHeadless }) {
            val stores = results.filterIsInstance<ScrapeResult.RetryNonHeadless>().map { it.store }
            println("\n\u001b[33mBot detection detected for $stores. Relaunching in non-headless mode...\u001b[0m")
            return runScrapers(request, headless = false)
        }

        val successfulProducts = results.filterIsInstance<ScrapeResult.Success>()
            .flatMap { it.results }

        val failures = results.mapNotNull {
            when (it) {
                is ScrapeResult.Failure -> FailureReason(it.store, it.reason)
                is ScrapeResult.RetryNonHeadless -> FailureReason(it.store, "Bot detection triggered: ${it.reason}")
                else -> null
            }
        }

        return FinalResponse(successfulProducts, failures)
    }

    private suspend fun executeScrape(request: ScrapeRequest, headless: Boolean): List<ScrapeResult> = coroutineScope {
        val requestWithHeadless = request.copy(isHeadless = headless)
        Playwright.create().use { playwright ->
            playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(headless)).use { browser ->
                scrapers.map { scraper ->
                    async {
                        val context = browser.newContext(
                            Browser.NewContextOptions()
                                .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                                .setExtraHTTPHeaders(mapOf(
                                    "Accept-Language" to "en-US,en;q=0.9",
                                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8"
                                ))
                        )
                        try {
                            scraper.scrape(context, requestWithHeadless)
                        } catch (e: Exception) {
                            ScrapeResult.Failure(scraper.storeName, e.message ?: "Unknown error")
                        } finally {
                            context.close()
                        }
                    }
                }.map { it.await() }
            }
        }
    }
}
