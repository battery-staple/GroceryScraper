import engine.ScraperEngine
import kotlinx.coroutines.runBlocking
import models.ScrapeRequest
import scrapers.*
import output.HtmlExporter
import java.util.*

fun main(args: Array<String>) = runBlocking {
    val scanner = Scanner(System.`in`)
    val engine = ScraperEngine(listOf(
//        WalmartScraper(),
        WegmansScraper(),
        TopsScraper(),
        AldiScraper(),
        TraderJoesScraper(),
        FoodBazaarScraper()
    ))

    println("\u001b[33mGroceryScraper\u001b[0m")
    println("-----------------")

    while (true) {
        print("\nEnter Zip Code: ")
        val zipCode = scanner.nextLine().trim()
        if (zipCode.isEmpty()) continue

        var useHeadless = true
        while (true) {
            println("\nEnter Product to Search (or 'b' to change zip): ")
            val input = scanner.nextLine().trim()
            if (input.lowercase() == "b") break
            if (input.isEmpty()) continue
            
            val query = if (input.lowercase() == "d") {
                useHeadless = false
                println("\u001b[33mDebug Mode Enabled (Non-headless)\u001b[0m")
                print("Enter Product to Search: ")
                scanner.nextLine().trim()
            } else {
                useHeadless = true
                input
            }
            if (query.isEmpty()) continue

            println("\n\u001b[36mScraping stores for '$query' in $zipCode... (Use 'd' for debug/non-headless mode)\u001b[0m")
            
            try {
                val response = engine.runScrapers(ScrapeRequest(query, zipCode), headless = useHeadless)
                
                if (response.results.isNotEmpty()) {
                    println("\nResults:")
                    println(String.format("%-20s %-10s %-40s", "Store", "Price", "Product"))
                    println("-".repeat(70))
                    
                    response.results.sortedBy { it.priceCents ?: Int.MAX_VALUE }.forEach { product ->
                        val priceStr = product.priceCents?.let { String.format("$%.2f", it / 100.0) } ?: "Unavailable"
                        println(String.format("%-20s %-10s %-40s", 
                            product.store, 
                            priceStr, 
                            (product.productName ?: "N/A").take(40)))
                    }
                } else {
                    println("\nNo results found.")
                }

                if (response.failures.isNotEmpty()) {
                    println("\n\u001b[31mFailures:\u001b[0m")
                    response.failures.forEach { failure ->
                        println("- ${failure.store}: ${failure.reason}")
                    }
                }

                HtmlExporter.exportAndOpen(query, response)

            } catch (e: Exception) {
                println("\u001b[31mAn error occurred: ${e.message}\u001b[0m")
            }

            println("\nPress Enter to search again, 'b' to change zip, or 'q' to quit.")
            val next = scanner.nextLine().trim().lowercase()
            if (next == "q") return@runBlocking
            if (next == "b") break
        }
    }
}
