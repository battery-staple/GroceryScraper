package scrapers

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import models.ScrapeRequest
import models.ScrapeResult
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class TopsScraperTest {

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

        override fun navigate(url: String) { navigatedUrl = url }
        override fun waitForSelector(selector: String, timeoutMs: Double) {}
        override fun querySelectorAll(selector: String): List<ElementProxy> = elements
        override fun querySelector(selector: String): ElementProxy? = null
        override fun fill(selector: String, value: String) {}
        override fun click(selector: String, timeoutMs: Double) {}
        override fun waitForTimeout(timeoutMs: Double) {}
        override fun waitForLoadState(state: String) {}
        override fun title(): String = "Fake Tops"
        override fun close() {}
    }

    @Test
    fun whenValidResults_returnsSuccess() = runBlocking {
        val scraper = TopsScraper()
        val fakePage = FakePageProxy().apply {
            elements = listOf(
                FakeElementProxy(
                    text = "Tops Whole Milk Current price: $3.99",
                    attrs = mapOf("href" to "/p/milk/1"),
                    children = mapOf(
                        "[role='heading']" to FakeElementProxy(text = "Tops Whole Milk")
                    )
                )
            )
        }

        val result = scraper.scrapeWithPage(fakePage, ScrapeRequest("milk", "14850"))

        val success = assertIs<ScrapeResult.Success>(result)
        assertThat(success.results).hasSize(1)
        assertThat(success.results[0].productName).isEqualTo("Tops Whole Milk")
        assertThat(success.results[0].priceCents).isEqualTo(399)
    }
}
