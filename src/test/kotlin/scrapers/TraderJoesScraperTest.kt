package scrapers

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import models.ScrapeRequest
import models.ScrapeResult
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class TraderJoesScraperTest {

    @Test
    fun whenValidResults_returnsSuccess() = runBlocking {
        val scraper = TraderJoesScraper()
        val fakePage = FakePageProxy(pageTitle = "Fake Trader Joes").apply {
            elements = listOf(
                FakeElementProxy(
                    children = mapOf(
                        "a[class*='SearchResultCard_searchResultCard__titleLink__']" to FakeElementProxy(
                            text = "TJ's Organic Milk",
                            attrs = mapOf("href" to "/p/milk/1")
                        ),
                        "span[class*='ProductPrice_productPrice__price__']" to FakeElementProxy(text = "$5.69 / 64 Fl Oz")
                    )
                )
            )
        }

        val result = scraper.scrapeWithPage(fakePage, ScrapeRequest("milk", "14850"))

        val success = assertIs<ScrapeResult.Success>(result)
        assertThat(success.results).hasSize(1)
        assertThat(success.results[0].productName).isEqualTo("TJ's Organic Milk")
        assertThat(success.results[0].priceCents).isEqualTo(569)
    }
}
