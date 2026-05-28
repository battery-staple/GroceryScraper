package scrapers

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import models.ScrapeRequest
import models.ScrapeResult
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class WegmansScraperTest {

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
        override fun title(): String = "Fake Wegmans"
        override fun close() {}
    }

    @Test
    fun whenValidResults_returnsSuccess() = runBlocking {
        val scraper = WegmansScraper()
        val fakePage = FakePageProxy().apply {
            elements = listOf(
                FakeElementProxy(
                    attrs = mapOf("data-bv-product-id" to "SKU_59715"),
                    children = mapOf(
                        "h3.component--base-heading" to FakeElementProxy(text = "Wegmans Milk"),
                        ".price b" to FakeElementProxy(text = "$4.25"),
                        "[data-bv-product-id]" to FakeElementProxy(attrs = mapOf("data-bv-product-id" to "SKU_59715"))
                    )
                )
            )
        }

        val result = scraper.scrapeWithPage(fakePage, ScrapeRequest("milk", "14850"))

        val success = assertIs<ScrapeResult.Success>(result)
        assertThat(success.results).hasSize(1)
        assertThat(success.results[0].productName).isEqualTo("Wegmans Milk")
        assertThat(success.results[0].priceCents).isEqualTo(425)
        assertThat(success.results[0].link).isEqualTo("https://www.wegmans.com/shop/product/59715")
    }
}
