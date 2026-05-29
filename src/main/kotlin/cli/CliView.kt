package cli

import models.ScrapeState

/**
 * Represents the result of a search input prompt.
 */
sealed interface SearchPromptResult {
    /**
     * Represents a search query intent.
     * @property query The product search query.
     */
    data class Search(val query: String) : SearchPromptResult

    /**
     * Represents user intent to go back to the zip code prompt.
     */
    object Back : SearchPromptResult
}

/**
 * Represents the next action selected by the user after a scraping run.
 */
enum class NextAction {
    /** Prompt for another product search in the same zip code. */
    SEARCH_AGAIN,
    /** Go back to the zip code prompt. */
    CHANGE_ZIP,
    /** Exit the application. */
    QUIT
}

/**
 * Interface representing the Command Line User Interface.
 */
interface CliView {
    /**
     * Displays the welcome banner.
     */
    fun showWelcome()

    /**
     * Prompts the user for a Zip Code.
     * @return The trimmed zip code entered by the user.
     */
    fun promptZipCode(): String

    /**
     * Prompts the user for a search query, allowing navigation commands.
     * @return A [SearchPromptResult] indicating search query or navigation intent.
     */
    fun promptSearchQuery(): SearchPromptResult

    /**
     * Displays that a scraping run is beginning.
     * @param query The search query.
     * @param zipCode The target zip code.
     */
    fun showScrapeStart(query: String, zipCode: String)

    /**
     * Displays a progress update for a store's scraper.
     * @param state The current state of the scraping process.
     */
    fun showScrapeState(state: ScrapeState)

    /**
     * Displays a system-level error message.
     * @param message The details of the error.
     */
    fun showSystemError(message: String)

    /**
     * Prompts the user for the next action to perform.
     * @return The selected [NextAction].
     */
    fun promptNextAction(): NextAction
}
