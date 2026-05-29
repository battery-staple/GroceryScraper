package scrapers

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import models.ScrapeRequest
import models.ScrapeResult
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class WegmansScraperTest {

    @Test
    fun whenValidResults_returnsSuccess() = runBlocking {
        val scraper = WegmansScraper()
        val fakePage = FakePageProxy(pageTitle = "Fake Wegmans").apply {
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
