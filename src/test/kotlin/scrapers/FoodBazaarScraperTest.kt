package scrapers

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import models.ScrapeRequest
import models.ScrapeResult
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class FoodBazaarScraperTest {

    //region Fakes
    class FakeElementProxy(
        private val text: String? = null,
        private val attrs: Map<String, String> = emptyMap(),
        private val children: Map<String, FakeElementProxy> = emptyMap()
    ) : ElementProxy {
        override fun textContent(): String? = text
        override fun getAttribute(name: String): String? = attrs[name]
        override fun querySelector(selector: String): ElementProxy? = children[selector]
        override fun click() {}
        override fun fill(value: String) {}
    }

    class FakePageProxy : PageProxy {
        var navigatedUrl: String? = null
        var elements: List<FakeElementProxy> = emptyList()
        var shouldTimeout: Boolean = false

        override fun navigate(url: String) { navigatedUrl = url }
        override fun waitForSelector(selector: String, timeoutMs: Double) {
            if (shouldTimeout) throw Exception("Timeout")
        }
        override fun querySelectorAll(selector: String): List<ElementProxy> = elements
        override fun querySelector(selector: String): ElementProxy? = null
        override fun fill(selector: String, value: String) {}
        override fun click(selector: String, timeoutMs: Double) {}
        override fun waitForTimeout(timeoutMs: Double) {}
        override fun waitForLoadState(state: String) {}
        override fun title(): String = "Fake Food Bazaar"
        override fun close() {}
    }
    //endregion

    @Test
    fun whenValidResults_returnsSuccess() = runBlocking {
        // Arrange
        val scraper = FoodBazaarScraper()
        val fakePage = FakePageProxy().apply {
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

        // Act
        val result = scraper.scrapeWithPage(fakePage, ScrapeRequest("milk", "14850"))

        // Assert
        val success = assertIs<ScrapeResult.Success>(result)
        assertThat(success.results).hasSize(1)
        assertThat(success.results[0].productName).isEqualTo("Food Bazaar Milk")
        assertThat(success.results[0].priceCents).isEqualTo(309)
        assertThat(success.results[0].link).isEqualTo("https://shop.foodbazaar.com/p/milk/123")
    }

    @Test
    fun whenNoResults_returnsFailure() = runBlocking {
        // Arrange
        val scraper = FoodBazaarScraper()
        val fakePage = FakePageProxy().apply {
            elements = emptyList()
        }

        // Act
        val result = scraper.scrapeWithPage(fakePage, ScrapeRequest("nothing", "14850"))

        // Assert
        val failure = assertIs<ScrapeResult.Failure>(result)
        assertThat(failure.reason).contains("No results found")
    }
}
