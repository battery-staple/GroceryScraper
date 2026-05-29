import cli.CliController
import cli.ConsoleCliView
import engine.ScraperEngine
import kotlinx.coroutines.runBlocking
import output.HtmlExporter
import output.TerminalExporter
import scrapers.*
import web.WebServer

/**
 * Main entry point of the GroceryScraper application.
 * Performs dependency injection and starts the CLI controller loop.
 */
fun main(args: Array<String>) = runBlocking {
    val debugMode = args.contains("--debug") || args.contains("-d")
    val tuiMode = args.contains("--tui") || args.contains("-t")

    val engine = ScraperEngine(listOf(
//        WalmartScraper(),
        WegmansScraper(),
        TopsScraper(),
        AldiScraper(),
        TraderJoesScraper(),
        FoodBazaarScraper()
    ))

    if (tuiMode) {
        val view = ConsoleCliView()
        val exporters = listOf(
            TerminalExporter(),
            HtmlExporter
        )

        val controller = CliController(engine, view, exporters, debugMode)
        controller.start()
    } else {
        val server = WebServer(engine)
        println("Starting Web Interface at http://localhost:8080")
        try {
            Runtime.getRuntime().exec(arrayOf("open", "http://localhost:8080"))
        } catch (e: Exception) {
            // Ignore error if 'open' fails (e.g. if no GUI environment is available)
        }
        server.start(8080)
    }
}
