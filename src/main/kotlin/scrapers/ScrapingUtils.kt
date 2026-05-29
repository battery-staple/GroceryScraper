package scrapers

object ScrapingUtils {
    fun parsePrice(priceText: String): Int? {
        val match = Regex("""\$?\d+\.\d{2}""").find(priceText)
        return if (match != null) {
            val numeric = match.value.replace("$", "").toDoubleOrNull() ?: return null
            (numeric * 100).toInt()
        } else {
            null
        }
    }
}
