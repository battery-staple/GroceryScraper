package scrapers

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import kotlinx.coroutines.runBlocking
import models.ScrapeRequest
import models.ScrapeResult
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

@Tag("integration")
class ScraperIntegrationTest {

    private fun runScraperTest(
        scraper: Scraper, query: String, zipCode: String, headless: Boolean = true
    ) = runBlocking {
        Playwright.create().use { playwright ->
            playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(headless))
                .use { browser ->
                    val context = browser.newContext(
                        Browser.NewContextOptions()
                            .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                            .setViewportSize(1280, 800)
                    )
                    val result = scraper.scrape(context, ScrapeRequest(query, zipCode))

                    if (result is ScrapeResult.Failure) {
                        println("${scraper.storeName} Failed: ${result.reason}")
                    }

                    assertIs<ScrapeResult.Success>(result)
                    assertTrue(result.results.isNotEmpty(), "Results should not be empty for ${scraper.storeName}")
                    println("${scraper.storeName} found ${result.results.size} items.")
                }
        }
    }

    @Test
    @Tag("flaky")
    fun testWalmart() = runScraperTest(WalmartScraper(), "milk", "14850", headless = false)

    @Test
    fun testWegmans() = runScraperTest(WegmansScraper(), "milk", "14850")

    @Test
    fun testTops() = runScraperTest(TopsScraper(), "milk", "14850")

    @Test
    fun testAldi() = runScraperTest(AldiScraper(), "milk", "14850")

    @Test
    @Tag("flaky")
    fun testTraderJoes() = runScraperTest(TraderJoesScraper(), "milk", "14850")

    @Test
    fun testFoodBazaar() = runScraperTest(FoodBazaarScraper(), "milk", "14850")
}
