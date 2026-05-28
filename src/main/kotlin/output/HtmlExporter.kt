package output

import models.FinalResponse
import java.io.File

object HtmlExporter {
    fun exportAndOpen(query: String, response: FinalResponse) {
        val htmlBuilder = StringBuilder()
        htmlBuilder.append("<html><head><title>GroceryScraper Results</title>")
        htmlBuilder.append("<style>")
        htmlBuilder.append("body { font-family: 'Inter', system-ui, -apple-system, sans-serif; background-color: #f8f9fa; color: #212529; margin: 40px; }")
        htmlBuilder.append("h1 { color: #343a40; border-bottom: 2px solid #dee2e6; padding-bottom: 10px; }")
        htmlBuilder.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; background-color: #ffffff; box-shadow: 0 4px 6px rgba(0,0,0,0.1); border-radius: 8px; overflow: hidden; }")
        htmlBuilder.append("th, td { padding: 15px; text-align: left; border-bottom: 1px solid #e9ecef; }")
        htmlBuilder.append("th { background-color: #4dabf7; color: white; font-weight: 600; text-transform: uppercase; font-size: 0.9em; letter-spacing: 0.5px; }")
        htmlBuilder.append("tr:last-child td { border-bottom: none; }")
        htmlBuilder.append("tr:hover { background-color: #f1f3f5; }")
        htmlBuilder.append("a { color: #4dabf7; text-decoration: none; font-weight: 500; }")
        htmlBuilder.append("a:hover { text-decoration: underline; color: #3bc9db; }")
        htmlBuilder.append(".failures { margin-top: 40px; background-color: #fff5f5; padding: 20px; border-radius: 8px; border-left: 5px solid #fa5252; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }")
        htmlBuilder.append(".failures h2 { color: #c92a2a; margin-top: 0; }")
        htmlBuilder.append(".failures ul { margin-bottom: 0; }")
        htmlBuilder.append(".failures li { margin-bottom: 5px; }")
        htmlBuilder.append("</style></head><body>")
        
        htmlBuilder.append("<h1>GroceryScraper Results for '").append(query).append("'</h1>")
        
        if (response.results.isNotEmpty()) {
            htmlBuilder.append("<table>")
            htmlBuilder.append("<tr><th>Store</th><th>Price</th><th>Product</th><th>Link</th></tr>")
            response.results.sortedBy { it.priceCents ?: Int.MAX_VALUE }.forEach { product ->
                val priceStr = product.priceCents?.let { String.format("$%.2f", it / 100.0) } ?: "Unavailable"
                val nameStr = product.productName?.replace("<", "&lt;")?.replace(">", "&gt;") ?: "N/A"
                val linkHtml = "<a href=\"${product.link.replace("\"", "&quot;")}\" target=\"_blank\" rel=\"noopener noreferrer\">View</a>"
                htmlBuilder.append("<tr><td>${product.store}</td><td><strong>").append(priceStr).append("</strong></td><td>").append(nameStr).append("</td><td>").append(linkHtml).append("</td></tr>")
            }
            htmlBuilder.append("</table>")
        } else {
            htmlBuilder.append("<p>No results found.</p>")
        }

        if (response.failures.isNotEmpty()) {
            htmlBuilder.append("<div class='failures'><h2>Failures</h2><ul>")
            response.failures.forEach { failure ->
                htmlBuilder.append("<li><strong>${failure.store}:</strong> ").append(failure.reason.replace("<", "&lt;").replace(">", "&gt;")).append("</li>")
            }
            htmlBuilder.append("</ul></div>")
        }

        htmlBuilder.append("</body></html>")
        
        val htmlFile = File("results.html")
        htmlFile.writeText(htmlBuilder.toString())
        
        try {
            Runtime.getRuntime().exec(arrayOf("open", htmlFile.absolutePath))
        } catch (e: Exception) {
            println("\u001b[31mFailed to open HTML file: ${e.message}\u001b[0m")
        }
    }
}
