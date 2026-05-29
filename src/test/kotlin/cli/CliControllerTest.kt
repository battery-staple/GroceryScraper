package cli

import com.google.common.truth.Truth.assertThat
import engine.ScraperEngine
import kotlinx.coroutines.test.runTest
import models.*
import org.junit.jupiter.api.Test
import output.ResultExporter
import scrapers.Scraper

class CliControllerTest {

    @Test
    fun whenZipCodeIsEmpty_rePrompts() = runTest {
        val view = FakeCliView().apply {
            zipCodePrompts.add("") // First prompt empty
            zipCodePrompts.add("10001") // Second prompt valid
            searchQueries.add(SearchPromptResult.Search("apple"))
            nextActions.add(NextAction.QUIT)
        }
        val engine = ScraperEngine(emptyList())
        val controller = CliController(engine, view, emptyList())

        controller.start()

        assertThat(view.zipCodeIndex).isEqualTo(2) // Ensure it prompted exactly twice
    }

    @Test
    fun whenSearchInputIsEmpty_rePromptsSearch() = runTest {
        val view = FakeCliView().apply {
            zipCodePrompts.add("10001")
            searchQueries.add(SearchPromptResult.Search("")) // Empty query
            searchQueries.add(SearchPromptResult.Search("milk")) // Valid query
            nextActions.add(NextAction.QUIT)
        }
        val engine = ScraperEngine(emptyList())
        val controller = CliController(engine, view, emptyList())

        controller.start()

        assertThat(view.searchIndex).isEqualTo(2)
        assertThat(view.shownScrapeStarts).containsExactly("milk" to "10001")
    }

    @Test
    fun whenSearchInputIsBack_returnsToZipCodePrompt() = runTest {
        val view = FakeCliView().apply {
            zipCodePrompts.add("10001")
            searchQueries.add(SearchPromptResult.Back)
            zipCodePrompts.add("10002")
            searchQueries.add(SearchPromptResult.Search("milk"))
            nextActions.add(NextAction.QUIT)
        }
        val engine = ScraperEngine(emptyList())
        val controller = CliController(engine, view, emptyList())

        controller.start()

        assertThat(view.zipCodeIndex).isEqualTo(2)
    }

    @Test
    fun whenDebugModeIsEnabled_enablesDebugMode() = runTest {
        var isHeadlessObserved = true
        val scraper = object : Scraper {
            override val storeName = "TestStore"
            override suspend fun scrape(
                context: com.microsoft.playwright.BrowserContext,
                request: ScrapeRequest,
                onState: suspend (models.ScrapeState) -> Unit
            ): ScrapeResult {
                isHeadlessObserved = request.isHeadless
                return ScrapeResult.Success(emptyList())
            }
        }
        val engine = ScraperEngine(listOf(scraper))
        val view = FakeCliView().apply {
            zipCodePrompts.add("10001")
            searchQueries.add(SearchPromptResult.Search("milk"))
            nextActions.add(NextAction.QUIT)
        }
        val controller = CliController(engine, view, emptyList(), debugMode = true)

        controller.start()

        assertThat(isHeadlessObserved).isFalse() // Debug mode -> headless is false
    }

    @Test
    fun whenSearchInputIsValid_initiatesScrapeAndExports() = runTest {
        val scraper = object : Scraper {
            override val storeName = "TestStore"
            override suspend fun scrape(
                context: com.microsoft.playwright.BrowserContext,
                request: ScrapeRequest,
                onState: suspend (models.ScrapeState) -> Unit
            ): ScrapeResult {
                return ScrapeResult.Success(listOf(Product("TestStore", 100, "link", "Item")))
            }
        }
        val engine = ScraperEngine(listOf(scraper))
        val exporter = FakeResultExporter()
        val view = FakeCliView().apply {
            zipCodePrompts.add("10001")
            searchQueries.add(SearchPromptResult.Search("milk"))
            nextActions.add(NextAction.QUIT)
        }
        val controller = CliController(engine, view, listOf(exporter))

        controller.start()

        assertThat(exporter.exports).containsExactly(
            "milk" to models.FinalResponse(
                results = listOf(Product("TestStore", 100, "link", "Item")),
                failures = emptyList()
            )
        )
    }

    @Test
    fun whenScraperFails_exportsFailureReasonAndRePrompts() = runTest {
        val scraper = object : Scraper {
            override val storeName = "TestStore"
            override suspend fun scrape(
                context: com.microsoft.playwright.BrowserContext,
                request: ScrapeRequest,
                onState: suspend (models.ScrapeState) -> Unit
            ): ScrapeResult {
                throw RuntimeException("Network error") // ScraperEngine will catch this and return a Failure
            }
        }
        val engine = ScraperEngine(listOf(scraper))
        val exporter = FakeResultExporter()
        val view = FakeCliView().apply {
            zipCodePrompts.add("10001")
            searchQueries.add(SearchPromptResult.Search("milk"))
            nextActions.add(NextAction.QUIT)
        }
        val controller = CliController(engine, view, listOf(exporter))

        controller.start()

        assertThat(exporter.exports).containsExactly(
            "milk" to models.FinalResponse(
                results = emptyList(),
                failures = listOf(models.FailureReason("TestStore", "Network error"))
            )
        )
        assertThat(view.nextActionIndex).isEqualTo(1)
    }

    @Test
    fun whenNextActionIsChangeZip_returnsToZipCodePrompt() = runTest {
        val engine = ScraperEngine(emptyList())
        val view = FakeCliView().apply {
            zipCodePrompts.add("10001")
            searchQueries.add(SearchPromptResult.Search("milk"))
            nextActions.add(NextAction.CHANGE_ZIP)

            zipCodePrompts.add("10002")
            searchQueries.add(SearchPromptResult.Search("eggs"))
            nextActions.add(NextAction.QUIT)
        }
        val controller = CliController(engine, view, emptyList())

        controller.start()

        assertThat(view.zipCodeIndex).isEqualTo(2)
        assertThat(view.searchIndex).isEqualTo(2)
        assertThat(view.shownScrapeStarts.last().second).isEqualTo("10002")
    }

    @Test
    fun whenNextActionIsSearchAgain_staysInSearchPrompt() = runTest {
        val engine = ScraperEngine(emptyList())
        val view = FakeCliView().apply {
            zipCodePrompts.add("10001")
            searchQueries.add(SearchPromptResult.Search("milk"))
            nextActions.add(NextAction.SEARCH_AGAIN)

            searchQueries.add(SearchPromptResult.Search("eggs"))
            nextActions.add(NextAction.QUIT)
        }
        val controller = CliController(engine, view, emptyList())

        controller.start()

        assertThat(view.zipCodeIndex).isEqualTo(1)
        assertThat(view.searchIndex).isEqualTo(2)
        assertThat(view.shownScrapeStarts.last().first).isEqualTo("eggs")
        assertThat(view.shownScrapeStarts.last().second).isEqualTo("10001")
    }

    @Test
    fun whenScrapeEmitsStateUpdates_rendersProgressInRealTime() = runTest {
        val scraper = object : Scraper {
            override val storeName = "TestStore"
            override suspend fun scrape(
                context: com.microsoft.playwright.BrowserContext,
                request: ScrapeRequest,
                onState: suspend (models.ScrapeState) -> Unit
            ): ScrapeResult {
                onState(ScrapeState.Navigating("TestStore", "http://example.com"))
                onState(ScrapeState.Parsing("TestStore"))
                return ScrapeResult.Success(emptyList())
            }
        }
        val engine = ScraperEngine(listOf(scraper))
        val view = FakeCliView().apply {
            zipCodePrompts.add("10001")
            searchQueries.add(SearchPromptResult.Search("milk"))
            nextActions.add(NextAction.QUIT)
        }
        val controller = CliController(engine, view, emptyList())

        controller.start()

        assertThat(view.shownStates).containsExactly(
            ScrapeState.Navigating("TestStore", "http://example.com"),
            ScrapeState.Parsing("TestStore")
        ).inOrder()
    }

    //region Fakes
    /**
     * Fake implementation of [CliView] used to simulate user interactions and track view updates.
     */
    private class FakeCliView : CliView {
        /** List of all mocked zip code strings to return sequentially. */
        val zipCodePrompts = mutableListOf<String>()

        /** List of all mocked search prompt results to return sequentially. */
        val searchQueries = mutableListOf<SearchPromptResult>()

        /** List of all mocked next actions to return sequentially. */
        val nextActions = mutableListOf<NextAction>()

        /** Current index into the [zipCodePrompts] list. */
        var zipCodeIndex = 0

        /** Current index into the [searchQueries] list. */
        var searchIndex = 0

        /** Current index into the [nextActions] list. */
        var nextActionIndex = 0

        /** History of all query and zipCode pairs received in [showScrapeStart]. */
        val shownScrapeStarts = mutableListOf<Pair<String, String>>()

        /** History of all error messages received in [showSystemError]. */
        val shownErrors = mutableListOf<String>()

        /** History of all scrape states received in [showScrapeState]. */
        val shownStates = mutableListOf<ScrapeState>()

        /** Indicates whether [showWelcome] has been called. */
        var welcomeShown = false

        /** Records that the welcome message was shown. */
        override fun showWelcome() {
            welcomeShown = true
        }

        /** Returns the next mocked zip code. */
        override fun promptZipCode(): String = zipCodePrompts[zipCodeIndex++]

        /** Returns the next mocked search prompt result. */
        override fun promptSearchQuery(): SearchPromptResult = searchQueries[searchIndex++]

        /** Records the start of a scrape with the given [query] and [zipCode]. */
        override fun showScrapeStart(query: String, zipCode: String) {
            shownScrapeStarts.add(query to zipCode)
        }

        /** Records the given scrape [state]. */
        override fun showScrapeState(state: ScrapeState) {
            shownStates.add(state)
        }

        /** Records the system error [message]. */
        override fun showSystemError(message: String) {
            shownErrors.add(message)
        }

        /** Returns the next mocked [NextAction]. */
        override fun promptNextAction(): NextAction = nextActions[nextActionIndex++]
    }

    /**
     * Fake implementation of [ResultExporter] used to capture and inspect the final exported responses.
     */
    private class FakeResultExporter : ResultExporter {
        /** History of all exported queries and their corresponding [FinalResponse]. */
        val exports = mutableListOf<Pair<String, FinalResponse>>()

        /** Records the exported [query] and [response]. */
        override fun export(query: String, response: FinalResponse) {
            exports.add(query to response)
        }
    }
    //endregion Fakes
}
