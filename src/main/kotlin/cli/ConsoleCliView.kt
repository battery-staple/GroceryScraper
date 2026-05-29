package cli

import models.ScrapeState
import java.util.Scanner

/**
 * Implementation of [CliView] that uses standard console I/O for interactions.
 * 
 * @property scanner The scanner used to read user input.
 */
class ConsoleCliView(private val scanner: Scanner = Scanner(System.`in`)) : CliView {

    override fun showWelcome() {
        println("\u001b[33mGroceryScraper\u001b[0m")
        println("-----------------")
    }

    override fun promptZipCode(): String {
        print("\nEnter Zip Code: ")
        return scanner.nextLine().trim()
    }

    override fun promptSearchQuery(): SearchPromptResult {
        println("\nEnter Product to Search (or 'b' to change zip): ")
        val input = scanner.nextLine().trim()
        if (input.lowercase() == "b") return SearchPromptResult.Back
        
        return SearchPromptResult.Search(input)
    }

    override fun showScrapeStart(query: String, zipCode: String) {
        println("\n\u001b[36mScraping stores for '$query' in $zipCode...\u001b[0m")
    }

    override fun showScrapeState(state: ScrapeState) {
        val msg = when (state) {
            is ScrapeState.Navigating -> "Navigating to ${state.url}"
            is ScrapeState.SettingLocation -> "Setting location to ${state.zipCode}"
            is ScrapeState.WaitingForResults -> "Waiting for results... ${state.additionalInfo ?: ""}"
            is ScrapeState.Parsing -> "Parsing results"
            is ScrapeState.Warning -> "WARNING: ${state.message}"
        }
        println("[${state.store}] $msg")
    }

    override fun showSystemError(message: String) {
        println("\u001b[31mAn error occurred: $message\u001b[0m")
    }

    override fun promptNextAction(): NextAction {
        println("\nPress Enter to search again, 'b' to change zip, or 'q' to quit.")
        val next = scanner.nextLine().trim().lowercase()
        return when (next) {
            "q" -> NextAction.QUIT
            "b" -> NextAction.CHANGE_ZIP
            else -> NextAction.SEARCH_AGAIN
        }
    }
}
