package scrapers

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import models.ScrapeRequest
import models.ScrapeResult
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class WalmartScraperTest {

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
        var lastZipCode: String? = null
        var elements: List<FakeElementProxy> = emptyList()
        var shouldTimeout: Boolean = false

        override fun navigate(url: String) { navigatedUrl = url }
        override fun waitForSelector(selector: String, timeoutMs: Double) {
            if (shouldTimeout) throw Exception("Timeout")
        }
        override fun querySelectorAll(selector: String): List<ElementProxy> = elements
        override fun querySelector(selector: String): ElementProxy? = null
        override fun fill(selector: String, value: String) {
            if (selector.contains("Zip code")) lastZipCode = value
        }
        override fun click(selector: String, timeoutMs: Double) {}
        override fun waitForTimeout(timeoutMs: Double) {}
        override fun waitForLoadState(state: String) {}
        override fun title(): String = "Fake Walmart"
        override fun close() {}
    }
    //endregion

    @Test
    fun whenValidResults_returnsSuccess() = runBlocking {
        // Arrange
        val scraper = WalmartScraper()
        val fakePage = FakePageProxy().apply {
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

        // Act
        val result = scraper.scrapeWithPage(fakePage, ScrapeRequest("milk", "14850"))

        // Assert
        val success = assertIs<ScrapeResult.Success>(result)
        assertThat(success.results).hasSize(1)
        assertThat(success.results[0].productName).isEqualTo("Milk")
        assertThat(success.results[0].priceCents).isEqualTo(395)
        assertThat(success.results[0].link).isEqualTo("https://www.walmart.com/milk-link")
    }

    @Test
    fun whenNoResults_returnsFailure() = runBlocking {
        // Arrange
        val scraper = WalmartScraper()
        val fakePage = FakePageProxy().apply {
            elements = emptyList()
        }

        // Act
        val result = scraper.scrapeWithPage(fakePage, ScrapeRequest("nothing", "14850"))

        // Assert
        val failure = assertIs<ScrapeResult.Failure>(result)
        assertThat(failure.reason).contains("No results found")
    }

    @Test
    fun whenTimeout_returnsFailure() = runBlocking {
        // Arrange
        val scraper = WalmartScraper()
        val fakePage = FakePageProxy().apply {
            shouldTimeout = true
        }

        // Act
        val result = scraper.scrapeWithPage(fakePage, ScrapeRequest("milk", "14850"))

        // Assert
        val failure = assertIs<ScrapeResult.Failure>(result)
        assertThat(failure.reason).contains("Timeout")
    }
}
