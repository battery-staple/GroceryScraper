package scrapers

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import models.ScrapeRequest
import models.ScrapeResult
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class TopsScraperTest {

    @Test
    fun whenValidResults_returnsSuccess() = runBlocking {
        val scraper = TopsScraper()
        val fakePage = FakePageProxy(pageTitle = "Fake Tops").apply {
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
