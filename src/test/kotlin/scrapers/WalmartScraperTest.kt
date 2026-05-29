package scrapers

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import models.ScrapeRequest
import models.ScrapeResult
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class WalmartScraperTest {

    @Test
    fun whenValidResults_returnsSuccess() = runBlocking {
        val scraper = WalmartScraper()
        val fakePage = FakePageProxy(pageTitle = "Fake Walmart").apply {
            elements = listOf(
                FakeElementProxy(
                    children = mapOf(
                        "[data-automation-id='product-title']" to FakeElementProxy(text = "Milk"),
                        "[data-automation-id='product-price']" to FakeElementProxy(text = "$3.95"),
                        "a[data-automation-id='product-title-link']" to FakeElementProxy(attrs = mapOf("href" to "/milk-link"))
                    )
                )
            )
        }

        val result = scraper.scrapeWithPage(fakePage, ScrapeRequest("milk", "14850"))

        val success = assertIs<ScrapeResult.Success>(result)
        assertThat(success.results).hasSize(1)
        assertThat(success.results[0].productName).isEqualTo("Milk")
        assertThat(success.results[0].priceCents).isEqualTo(395)
        assertThat(success.results[0].link).isEqualTo("https://www.walmart.com/milk-link")
    }

    @Test
    fun whenNoResults_returnsFailure() = runBlocking {
        val scraper = WalmartScraper()
        val fakePage = FakePageProxy(pageTitle = "Fake Walmart").apply {
            elements = emptyList()
        }

        val result = scraper.scrapeWithPage(fakePage, ScrapeRequest("nothing", "14850"))

        val failure = assertIs<ScrapeResult.Failure>(result)
        assertThat(failure.reason).contains("No results found")
    }

    @Test
    fun whenTimeout_returnsFailure() = runBlocking {
        val scraper = WalmartScraper()
        val fakePage = FakePageProxy(pageTitle = "Fake Walmart").apply {
            shouldTimeout = true
        }

        val result = scraper.scrapeWithPage(fakePage, ScrapeRequest("milk", "14850"))

        val failure = assertIs<ScrapeResult.Failure>(result)
        assertThat(failure.reason).contains("Timeout")
    }
}
