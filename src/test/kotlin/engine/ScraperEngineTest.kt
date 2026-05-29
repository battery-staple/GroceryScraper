package engine

import com.google.common.truth.Truth.assertThat
import com.microsoft.playwright.BrowserContext
import kotlinx.coroutines.test.runTest
import models.Product
import models.ScrapeRequest
import models.ScrapeResult
import org.junit.jupiter.api.Test
import scrapers.Scraper
import kotlin.test.assertTrue

class ScraperEngineTest {

    @Test
    fun runScrapers_returnsAggregatedResults() = runTest {
        val fakeScraper1 = FakeScraper("Store A", listOf(Product("Store A", 100, "link1", "Item 1")))
        val fakeScraper2 = FakeScraper("Store B", listOf(Product("Store B", 200, "link2", "Item 2")))
        val engine = ScraperEngine(listOf(fakeScraper1, fakeScraper2))
        val request = ScrapeRequest("test", "12345")

        val response = engine.runScrapers(request)

        assertThat(response.results).containsExactly(
            Product("Store A", 100, "link1", "Item 1"),
            Product("Store B", 200, "link2", "Item 2")
        )
        assertTrue { response.failures.isEmpty() }
    }

    @Test
    fun runScrapers_handlesFailures() = runTest {
        val failingScraper = FailingScraper("Store C", "Connection timeout")
        val engine = ScraperEngine(listOf(failingScraper))
        val request = ScrapeRequest("test", "12345")

        val response = engine.runScrapers(request)

        assertThat(response.results).isEmpty()
        assertThat(response.failures).containsExactly(
            models.FailureReason("Store C", "Connection timeout")
        )
    }

    @Test
    fun runScrapers_retriesOnRetryNonHeadless() = runTest {
        var scrapeCount = 0
        val retryScraper = object : Scraper {
            override val storeName: String = "RetryStore"
            override suspend fun scrape(context: BrowserContext, request: ScrapeRequest, onState: suspend (models.ScrapeState) -> Unit): ScrapeResult {
                scrapeCount++
                return if (request.isHeadless) {
                    ScrapeResult.RetryNonHeadless(storeName, "Bot detected")
                } else {
                    ScrapeResult.Success(listOf(Product(storeName, 500, "link", "Recovered Item")))
                }
            }
        }
        val engine = ScraperEngine(listOf(retryScraper))
        val request = ScrapeRequest("test", "12345")

        val response = engine.runScrapers(request, headless = true)

        assertThat(scrapeCount).isEqualTo(2)
        assertThat(response.results).containsExactly(
            Product("RetryStore", 500, "link", "Recovered Item")
        )
        assertTrue { response.failures.isEmpty() }
    }

    @Test
    fun runScrapers_treatsRetryNonHeadlessAsFailureIfAlreadyNonHeadless() = runTest {
        val retryScraper = object : Scraper {
            override val storeName: String = "RetryStore"
            override suspend fun scrape(context: BrowserContext, request: ScrapeRequest, onState: suspend (models.ScrapeState) -> Unit): ScrapeResult {
                return ScrapeResult.RetryNonHeadless(storeName, "Bot detected")
            }
        }
        val engine = ScraperEngine(listOf(retryScraper))
        val request = ScrapeRequest("test", "12345")

        val response = engine.runScrapers(request, headless = false)

        assertThat(response.results).isEmpty()
        assertThat(response.failures).containsExactly(
            models.FailureReason("RetryStore", "Bot detection triggered: Bot detected")
        )
    }

    //region Fakes
    /**
     * Fake implementation of [Scraper] that always returns a successful list of products.
     * 
     * @property storeName The name of the mock store.
     * @property products The products to return on a successful scrape.
     */
    private class FakeScraper(override val storeName: String, val products: List<Product>) : Scraper {
        /**
         * Returns a success state with the predefined [products].
         */
        override suspend fun scrape(context: BrowserContext, request: ScrapeRequest, onState: suspend (models.ScrapeState) -> Unit): ScrapeResult {
            return ScrapeResult.Success(products)
        }
    }

    /**
     * Fake implementation of [Scraper] that always returns a failure state with the provided error.
     * 
     * @property storeName The name of the mock store.
     * @property error The error message to return on a failed scrape.
     */
    private class FailingScraper(override val storeName: String, val error: String) : Scraper {
        /**
         * Returns a failure state with the predefined [error].
         */
        override suspend fun scrape(context: BrowserContext, request: ScrapeRequest, onState: suspend (models.ScrapeState) -> Unit): ScrapeResult {
            return ScrapeResult.Failure(storeName, error)
        }
    }
    //endregion Fakes
}
