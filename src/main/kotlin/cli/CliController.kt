package cli

import engine.ScraperEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import models.ScrapeRequest
import models.ScrapeState
import output.ResultExporter

/**
 * Controller responsible for orchestrating the CLI application loop,
 * receiving user input from the view, interacting with the engine,
 * and dispatching results to the output exporters.
 *
 * @property engine The scraper engine instance.
 * @property view The CLI view to handle user interactions.
 * @property exporters List of exporters to run on successful scrape completion.
 */
class CliController(
    private val engine: ScraperEngine,
    private val view: CliView,
    private val exporters: List<ResultExporter>
) {
    /**
     * Starts the main interaction loop of the CLI application.
     */
    suspend fun start() = coroutineScope {
        view.showWelcome()

        while (true) {
            val zipCode = view.promptZipCode()
            if (zipCode.isEmpty()) continue

            while (true) {
                val promptResult = view.promptSearchQuery()
                
                if (promptResult is SearchPromptResult.Back) {
                    break // Go back to zip code loop
                }

                val (query, debugMode) = when (promptResult) {
                    is SearchPromptResult.Search -> promptResult.query to promptResult.debugMode
                    else -> continue
                }
                
                if (query.isEmpty()) continue

                view.showScrapeStart(query, zipCode)

                try {
                    val stateChannel = Channel<ScrapeState>(Channel.BUFFERED)
                    
                    val collectorJob = launch {
                        for (state in stateChannel) {
                            view.showScrapeState(state)
                        }
                    }

                    val response = engine.runScrapers(
                        ScrapeRequest(query, zipCode),
                        headless = !debugMode,
                        stateChannel = stateChannel
                    )
                    
                    stateChannel.close()
                    collectorJob.join()

                    exporters.forEach { it.export(query, response) }

                } catch (e: Exception) {
                    view.showSystemError(e.message ?: "Unknown error")
                }

                val nextAction = view.promptNextAction()
                if (nextAction == NextAction.QUIT) {
                    return@coroutineScope
                }
                if (nextAction == NextAction.CHANGE_ZIP) {
                    break // Break out of search loop, prompt for zip again
                }
                // NextAction.SEARCH_AGAIN loops the inner while(true)
            }
        }
    }
}
