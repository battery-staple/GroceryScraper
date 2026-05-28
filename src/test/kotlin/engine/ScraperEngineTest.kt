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
    fun `runScrapers_returnsAggregatedResults`() = runTest {
        // Arrange
        val fakeScraper1 = FakeScraper("Store A", listOf(Product("Store A", 100, "link1", "Item 1")))
        val fakeScraper2 = FakeScraper("Store B", listOf(Product("Store B", 200, "link2", "Item 2")))
        val engine = ScraperEngine(listOf(fakeScraper1, fakeScraper2))
        val request = ScrapeRequest("test", "12345")

        // Act
        val response = engine.runScrapers(request)

        // Assert
        assertThat(response.results).hasSize(2)
        assertThat(response.results.map { it.store }).containsExactly("Store A", "Store B")
        assertTrue { response.failures.isEmpty() }
    }

    @Test
    fun `runScrapers_handlesFailures`() = runTest {
        // Arrange
        val failingScraper = FailingScraper("Store C", "Connection timeout")
        val engine = ScraperEngine(listOf(failingScraper))
        val request = ScrapeRequest("test", "12345")

        // Act
        val response = engine.runScrapers(request)

        // Assert
        assertThat(response.results).isEmpty()
        assertThat(response.failures).hasSize(1)
        assertThat(response.failures.first().store).isEqualTo("Store C")
        assertThat(response.failures.first().reason).isEqualTo("Connection timeout")
    }

    @Test
    fun `runScrapers_retriesOnRetryNonHeadless`() = runTest {
        // Arrange
        var scrapeCount = 0
        val retryScraper = object : Scraper {
            override val storeName: String = "RetryStore"
            override suspend fun scrape(context: BrowserContext, request: ScrapeRequest): ScrapeResult {
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

        // Act
        val response = engine.runScrapers(request, headless = true)

        // Assert
        assertThat(scrapeCount).isEqualTo(2)
        assertThat(response.results).hasSize(1)
        assertThat(response.results.first().productName).isEqualTo("Recovered Item")
        assertTrue { response.failures.isEmpty() }
    }

    @Test
    fun `runScrapers_treatsRetryNonHeadlessAsFailureIfAlreadyNonHeadless`() = runTest {
        // Arrange
        val retryScraper = object : Scraper {
            override val storeName: String = "RetryStore"
            override suspend fun scrape(context: BrowserContext, request: ScrapeRequest): ScrapeResult {
                return ScrapeResult.RetryNonHeadless(storeName, "Bot detected")
            }
        }
        val engine = ScraperEngine(listOf(retryScraper))
        val request = ScrapeRequest("test", "12345")

        // Act
        val response = engine.runScrapers(request, headless = false)

        // Assert
        assertThat(response.results).isEmpty()
        assertThat(response.failures).hasSize(1)
        assertThat(response.failures.first().reason).contains("Bot detection triggered")
    }

    //region Fakes
    private class FakeScraper(override val storeName: String, val products: List<Product>) : Scraper {
        override suspend fun scrape(context: BrowserContext, request: ScrapeRequest): ScrapeResult {
            return ScrapeResult.Success(products)
        }
    }

    private class FailingScraper(override val storeName: String, val error: String) : Scraper {
        override suspend fun scrape(context: BrowserContext, request: ScrapeRequest): ScrapeResult {
            return ScrapeResult.Failure(storeName, error)
        }
    }
    //endregion Fakes
}
