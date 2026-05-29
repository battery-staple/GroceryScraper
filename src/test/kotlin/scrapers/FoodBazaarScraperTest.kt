package scrapers

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import models.ScrapeRequest
import models.ScrapeResult
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class FoodBazaarScraperTest {

    @Test
    fun whenValidResults_returnsSuccess() = runBlocking {
        val scraper = FoodBazaarScraper()
        val fakePage = FakePageProxy(pageTitle = "Fake Food Bazaar").apply {
            elements = listOf(
                FakeElementProxy(
                    attrs = mapOf("href" to "/p/milk/123"),
                    children = mapOf(
                        "[role='heading']" to FakeElementProxy(text = "Food Bazaar Milk"),
                        "span.screen-reader-only" to FakeElementProxy(text = "Current price: $3.09")
                    )
                )
            )
        }

        val result = scraper.scrapeWithPage(fakePage, ScrapeRequest("milk", "14850"))

        val success = assertIs<ScrapeResult.Success>(result)
        assertThat(success.results).hasSize(1)
        assertThat(success.results[0].productName).isEqualTo("Food Bazaar Milk")
        assertThat(success.results[0].priceCents).isEqualTo(309)
        assertThat(success.results[0].link).isEqualTo("https://shop.foodbazaar.com/p/milk/123")
    }

    @Test
    fun whenNoResults_returnsFailure() = runBlocking {
        val scraper = FoodBazaarScraper()
        val fakePage = FakePageProxy(pageTitle = "Fake Food Bazaar").apply {
            elements = emptyList()
        }

        val result = scraper.scrapeWithPage(fakePage, ScrapeRequest("nothing", "14850"))

        val failure = assertIs<ScrapeResult.Failure>(result)
        assertThat(failure.reason).contains("No results found")
    }
}
