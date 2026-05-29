import cli.CliController
import cli.ConsoleCliView
import engine.ScraperEngine
import kotlinx.coroutines.runBlocking
import output.HtmlExporter
import output.TerminalExporter
import scrapers.*

/**
 * Main entry point of the GroceryScraper application.
 * Performs dependency injection and starts the CLI controller loop.
 */
fun main(args: Array<String>) = runBlocking {
    val engine = ScraperEngine(listOf(
//        WalmartScraper(),
        WegmansScraper(),
        TopsScraper(),
        AldiScraper(),
        TraderJoesScraper(),
        FoodBazaarScraper()
    ))

    val view = ConsoleCliView()
    val exporters = listOf(
        TerminalExporter(),
        HtmlExporter
    )

    val controller = CliController(engine, view, exporters)
    controller.start()
}
