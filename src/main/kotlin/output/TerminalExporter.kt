package output

import models.FinalResponse
import java.io.PrintStream

/**
 * Exporter that formats and prints the scraping results to a PrintStream (typically standard out).
 * @property out The PrintStream to write the results to.
 */
class TerminalExporter(private val out: PrintStream = System.out) : ResultExporter {
    override fun export(query: String, response: FinalResponse) {
        if (response.results.isNotEmpty()) {
            out.println("\nResults:")
            out.println(String.format("%-20s %-10s %-40s", "Store", "Price", "Product"))
            out.println("-".repeat(70))

            response.results.sortedBy { it.priceCents ?: Int.MAX_VALUE }.forEach { product ->
                val priceStr = product.priceCents?.let { String.format("$%.2f", it / 100.0) } ?: "Unavailable"
                out.println(String.format("%-20s %-10s %-40s",
                    product.store,
                    priceStr,
                    (product.productName ?: "N/A").take(40)))
            }
        } else {
            out.println("\nNo results found.")
        }

        if (response.failures.isNotEmpty()) {
            out.println("\n\u001b[31mFailures:\u001b[0m")
            response.failures.forEach { failure ->
                out.println("- ${failure.store}: ${failure.reason}")
            }
        }
    }
}
